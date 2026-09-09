package com.cloversecho.miq.recipe;

import com.cloversecho.miq.config.MIQConfig;
import com.cloversecho.miq.network.RecipeSyncPayload;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Holds the current daily recipe (food -> desire category) shared by the server
 * and synced to clients, and implements all server-side generation/timing logic.
 *
 * 每日食谱的核心逻辑：识别所有可食用的食物，在约定的时刻随机划分欲望分类，
 * 并把食谱同步给客户端（用于 Tooltip），同时在食谱变化时向所有玩家发送聊天提示。
 */
public final class DailyRecipeManager {

    /** The currently active recipe, shared by the logical server and the client. 当前生效的每日食谱。 */
    private static volatile Map<Item, DesireCategory> currentRecipe = Map.of();

    /** Day-time of the last observed server tick. 上次记录的服务器时间刻。 */
    private static long lastTickDayTime = -1L;
    /** Whether the startup recipe (loaded or freshly generated) has been set. 是否已完成启动期食谱初始化。 */
    private static boolean started = false;
    /** Per-node: the game-day index on which it was last triggered. 记录每个更新节点最近一次触发的游戏日编号。 */
    private static final Map<Integer, Long> nodeTriggeredDay = new HashMap<>();

    /**
     * Server-side tally of how many times each food has been eaten by any player since the last refresh.
     * Used to shift category weights at the next refresh and cleared automatically on each refresh.
     * 自上次刷新以来，全服玩家进食各食物的累计次数统计；用于在下一次刷新时调整分类权重，并在刷新后自动清空。
     */
    private static final Map<Item, Integer> eatCounts = new HashMap<>();

    /**
     * Records a food being eaten, server-side only. Called from the eat hook so the whole server's
     * players are tallied. Client-side calls (tooltips/HUD prediction) are ignored.
     * 在服务端记录一次进食。客户端调用（工具提示/HUD 预估）会被忽略。
     */
    static void recordEat(Player player, ItemStack stack) {
        if (player == null || stack == null || stack.isEmpty() || player.level().isClientSide()) {
            return;
        }
        eatCounts.merge(stack.getItem(), 1, Integer::sum);
    }

    private DailyRecipeManager() {
    }

    // ------------------------------------------------------------------
    // Public accessors / 对外查询接口
    // ------------------------------------------------------------------

    /**
     * Returns the desire category of an item, or {@code null} if the item is not
     * part of the current recipe. 返回某物品当前的欲望分类；若不在食谱中返回 null。
     */
    public static DesireCategory getCategory(Item item) {
        return item == null ? null : currentRecipe.get(item);
    }

    /** Replaces the recipe on the client when a sync payload arrives. 客户端收到同步包后替换本地食谱。 */
    public static void setClientRecipe(Map<Item, DesireCategory> recipe) {
        currentRecipe = recipe == null ? Map.of() : recipe;
    }

    // ------------------------------------------------------------------
    // Server-side per-tick logic / 服务器每 tick 检查逻辑
    // ------------------------------------------------------------------

    /**
     * Called every server tick. Detect whether current in-game time has crossed any configured
     * update node; if so regenerate the recipe exactly once (even if several nodes were crossed).
     *
     * 每个游戏天的更新节点（例如 3000 刻）被越过时触发一次食谱更新。
     * 即使一次 tick 同时越过多个节点，也只更新一次。
     */
    public static void serverTick(ServerLevel level) {
        if (!started) {
            return; // startup event has not finished loading/generating the recipe yet
        }
        long dayTime = level.getDayTime();
        if (lastTickDayTime < 0L) {
            lastTickDayTime = dayTime;
            return;
        }
        long prev = lastTickDayTime;
        long cur = dayTime;
        lastTickDayTime = cur;

        long curDay = cur / 24000L;
        boolean anyCrossed = false;
        for (Integer node : MIQConfig.RECIPE_UPDATE_NODES.get()) {
            if (nodeTriggeredDay.getOrDefault(node, -1L).longValue() == curDay) {
                continue; // already triggered for this game day
            }
            if (crossed(prev, cur, node)) {
                nodeTriggeredDay.put(node, curDay);
                anyCrossed = true;
            }
        }
        if (anyCrossed) {
            regenerate(level);
        }
    }

    /**
     * Whether the given tick-of-day node lies strictly inside the arc
     * (prev%24000, cur%24000] traversed since the last server tick, accounting for day-wrap.
     * 判断给定的时刻节点是否位于上一 tick 到当前 tick 之间前进的时间区间内（考虑跨天）。 */
    private static boolean crossed(long prev, long cur, int node) {
        long pt = prev % 24000L;
        long ct = cur % 24000L;
        if (cur - prev >= 24000L) {
            return true; // moved a full day or more: crossed every node
        }
        if (ct >= pt) {
            return node > pt && node <= ct;
        }
        // wrapped past midnight
        return node > pt || node <= ct;
    }

    // ------------------------------------------------------------------
    // Recipe generation / 食谱生成
    // ------------------------------------------------------------------

    private static void regenerate(ServerLevel level) {
        List<Item> foods = scanEdibleFoods();
        Map<Item, DesireCategory> next = new HashMap<>();
        RandomSource rng = level.getRandom();

        // Fixed preferences first: these items are forced into their configured category and
        // removed from the random pool so they cannot be re-rolled into another category.
        // 先应用固定分类：这些物品被强制划入指定分类，并从随机池中剔除，避免被再次随机。
        List<Item> remaining = new ArrayList<>(foods);
        applyFixedCategories(next, remaining);

        // Weighted random for all remaining foods, shifted by how much each has been eaten.
        // 其余食物按随机权重分配，并根据各自的累计进食次数进行偏移。
        for (Item food : remaining) {
            next.put(food, rollCategory(rng, food));
        }

        setClientRecipe(next);

        broadcastRecipe(level, next);
        announceWant(level, next);
        persist(level);

        // Eat statistics reset automatically each refresh. 进食统计在每次刷新时自动清空。
        eatCounts.clear();
    }

    /**
     * Forces every configured fixed-preference item into the recipe, removing it from the pool.
     * Retention order: don't-want, willing, very-want; an item already fixed by an earlier list is skipped.
     * 将配置中的固定分类物品划入食谱，并将其从随机池中移除。
     * 处理顺序：不想吃、愿意吃、很想吃；已被前面列表处理的物品跳过。
     */
    private static void applyFixedCategories(Map<Item, DesireCategory> next, List<Item> pool) {
        // When the don't-want penalty is disabled, items pinned to "don't want" are downgraded
        // to "willing" instead, matching the random roll which never yields DON_T_WANT then.
        // 当关闭"不想吃"惩罚时，固定为"不想吃"的物品会降级为"愿意吃"，与随机分配保持一致。
        DesireCategory dontWant = MIQConfig.ENABLE_PENALTY.get()
                ? DesireCategory.DON_T_WANT
                : DesireCategory.WILLING;
        applyFixedList(next, pool, MIQConfig.FIXED_DONT_WANT.get(), dontWant);
        applyFixedList(next, pool, MIQConfig.FIXED_WILLING.get(), DesireCategory.WILLING);
        applyFixedList(next, pool, MIQConfig.FIXED_VERY_WANT.get(), DesireCategory.VERY_WANT);
    }

    private static void applyFixedList(Map<Item, DesireCategory> next, List<Item> pool,
                                       List<? extends String> ids, DesireCategory category) {
        if (ids == null) {
            return;
        }
        for (String id : ids) {
            ResourceLocation loc = ResourceLocation.tryParse(id);
            if (loc == null) {
                continue; // malformed id: skip silently. 非法 ID：跳过。
            }
            Item item = BuiltInRegistries.ITEM.get(loc);
            if (item == null || item == Items.AIR) {
                continue;
            }
            if (pool.remove(item)) {
                next.put(item, category);
            }
        }
    }

    // ------------------------------------------------------------------
    // Startup / shutdown persistence / 启动加载与关闭保存
    // ------------------------------------------------------------------

    /**
     * Loads the persisted recipe on server start; if none exists, generates a fresh one.
     * Synchronised state is marked as {@code started} so per-tick node checks can begin.
     *
     * 服务器启动时读取已存档的当日食谱；没有存档则立即刷新一份新食谱。
     */
    public static void onServerStart(MinecraftServer server) {
        ServerLevel level = server.overworld();
        if (level == null) {
            return;
        }
        RecipeSaveData data = loadOrCreate(level);
        if (data.getRecipe().isEmpty()) {
            // No saved recipe: generate one now.
            setClientRecipe(Map.of());
            regenerate(level);
        } else {
            // Restore the persisted recipe so "today's" list stays consistent across restarts.
            setClientRecipe(data.getRecipe());
        }
        lastTickDayTime = level.getDayTime();
        started = true;
    }

    /**
     * Writes the current recipe to the world data on server shutdown.
     * 服务器关闭时把当前食谱写入存档。
     */
    public static void onServerStopping(MinecraftServer server) {
        ServerLevel level = server.overworld();
        if (level == null) {
            return;
        }
        RecipeSaveData data = loadOrCreate(level);
        data.setRecipe(currentRecipe);
        data.setDirty();
    }

    /**
     * Immediately performs a recipe refresh (e.g. via {@code /miq refresh}).
     * 立即执行一次食谱刷新（例如通过 /miq refresh 指令触发）。
     */
    public static void refreshNow(ServerLevel level) {
        if (level == null) {
            return;
        }
        regenerate(level);
        lastTickDayTime = level.getDayTime();
    }

    /** Gets (creating if needed) the persisted recipe storage for a level. 获取(必要时创建)存档对象。 */
    private static RecipeSaveData loadOrCreate(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(RecipeSaveData.factory(), RecipeSaveData.ID);
    }

    /** Writes the current recipe into the world data storage. 将当前食谱写入世界存档。 */
    private static void persist(ServerLevel level) {
        RecipeSaveData data = loadOrCreate(level);
        data.setRecipe(currentRecipe);
        data.setDirty();
    }

    /**
     * Detect every item that is edible and restores hunger (nutrition > 0).
     * Supports food added by other mods, whether via classic FoodProperties or
     * via the FOOD data component. / 识别所有可食用且能回复饱食度的物品（兼容其它模组的食物）。
     */
    private static List<Item> scanEdibleFoods() {
        List<Item> foods = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            ItemStack proxy = new ItemStack(item);
            FoodProperties fp = item.getFoodProperties(proxy, null);
            if (fp != null && fp.nutrition() > 0) {
                foods.add(item);
            }
        }
        return foods;
    }

    /**
     * Assigns a desire category to a food according to the configured weights, shifted by how many
     * times the food has been eaten since the last refresh: the more it was eaten, the stronger it is
     * pushed toward "don't want" and away from "very want".
     * 根据配置权重为一种食物划分欲望分类，并用其自上次刷新以来的进食次数做偏移：被吃得越多，
     * 越容易被划到“不想吃”、越难被划到“很想吃”。
     */
    private static DesireCategory rollCategory(RandomSource rng, Item food) {
        List<? extends Integer> weights = MIQConfig.DESIRE_WEIGHTS.get();
        int very = weights.get(0);
        int willing = weights.get(1);
        int dont = MIQConfig.ENABLE_PENALTY.get() ? weights.get(2) : 0;

        double veryEff = very;
        double willingEff = willing;
        double dontEff = dont;
        int count = eatCounts.getOrDefault(food, 0);
        if (count > 0) {
            double factor = 1.0 + MIQConfig.EAT_COUNT_AFFINITY.get() * count;
            veryEff = very / factor;   // eaten more -> less likely very want. 吃得越多越不可能很想吃
            dontEff = dont * factor;    // eaten more -> more likely don't want. 吃得越多越可能不想吃
        }

        double total = veryEff + willingEff + dontEff;
        double roll = rng.nextDouble() * total;
        if (roll < veryEff) {
            return DesireCategory.VERY_WANT;
        }
        return roll < veryEff + willingEff ? DesireCategory.WILLING : DesireCategory.DON_T_WANT;
    }

    private static void broadcastRecipe(ServerLevel level, Map<Item, DesireCategory> recipe) {
        RecipeSyncPayload payload = new RecipeSyncPayload(recipe);
        for (ServerPlayer player : level.players()) {
            if (player.connection != null) {
                PacketDistributor.sendToPlayer(player, payload);
            }
        }
    }

    /** Sends the current recipe to a single (just-joined) player so tooltips work immediately.
     *  把当前食谱同步给刚加入的玩家。 */
    public static void syncTo(ServerPlayer player) {
        if (player.connection != null && !currentRecipe.isEmpty()) {
            PacketDistributor.sendToPlayer(player, new RecipeSyncPayload(currentRecipe));
        }
    }

    /**
     * Pick one random 'very want to eat' food per player and announce it in chat.
     * 为每位玩家从“很想吃”列表中各自随机挑选一种食物并发送聊天提示。
     */
    private static void announceWant(ServerLevel level, Map<Item, DesireCategory> recipe) {
        List<Item> veryWants = new ArrayList<>();
        for (Map.Entry<Item, DesireCategory> e : recipe.entrySet()) {
            if (e.getValue() == DesireCategory.VERY_WANT) {
                veryWants.add(e.getKey());
            }
        }
        if (veryWants.isEmpty()) {
            return;
        }
        for (ServerPlayer player : level.players()) {
            Item pick = veryWants.get(player.getRandom().nextInt(veryWants.size()));
            player.sendSystemMessage(Component.translatable("chat.miq.want", displayName(pick)));
        }
    }

    /**
     * Returns a display name for {@code item} that is always a <em>translatable</em> {@link Component},
     * so each client renders it in its own configured language (important on multi-language servers).
     * If the hover name comes from a custom/item-name component (a literal, already-resolved text),
     * it falls back to the item's translatable description entry.
     */
    private static Component displayName(Item item) {
        Component hover = item.getDefaultInstance().getHoverName();
        if (hover.getContents() instanceof TranslatableContents) {
            return hover;
        }
        return Component.translatable(item.getDescriptionId());
    }

    // ------------------------------------------------------------------
    // Eat-behaviour helpers / 进食行为辅助（供 Mixin 调用）
    // ------------------------------------------------------------------

    /**
     * Builds a modified {@link FoodProperties} applying hunger/saturation/effect scaling,
     * plus the extra Luck (very-want) or the chance-based Hunger/Nausea (don't-want).
     * If the item is not part of the recipe, the original properties are returned unchanged.
     *
     * 根据欲望分类生成调整后的 FoodProperties：用同一缩放因子同时缩放饱食度恢复与饱和度，
     * 缩放食物自带效果时长，并在“很想吃”时附加幸运效果、在“不想吃”时以一定概率附加饥饿/反胃。
     */
    public static FoodProperties buildModifiedFoodProperties(FoodProperties base, Player player, ItemStack stack) {
        // Tally the eaten food (server-side only) for the next refresh's weight shift.
        // 记录本次进食（仅服务端）供下次刷新做权重偏移。
        recordEat(player, stack);

        DesireCategory cat = getCategory(stack.getItem());
        if (cat == null || cat == DesireCategory.WILLING) {
            return base;
        }

        RandomSource rng = player.getRandom();

        if (cat == DesireCategory.VERY_WANT) {
            // One shared factor scales both hunger-restore and saturation.
            float scale = 1.0f + (float) (double) MIQConfig.VERY_WANT_HUNGER_BOOST.get();
            int nutrition = Math.max(1, Math.round(base.nutrition() * scale));
            float saturation = Math.max(0.0f, base.saturation() * scale);
            float durFactor = 1.0f + (float) (double) MIQConfig.VERY_WANT_EFFECT_DURATION_BOOST.get();
            List<FoodProperties.PossibleEffect> effects = scaleEffectDurations(base.effects(), durFactor);
            if (MIQConfig.VERY_WANT_EXTRA_LUCK.get()) {
                effects.add(new FoodProperties.PossibleEffect(() -> new MobEffectInstance(
                        MobEffects.LUCK,
                        MIQConfig.VERY_WANT_LUCK_DURATION_SECONDS.get() * 20, 0), 1.0f));
            }
            return new FoodProperties(nutrition, saturation, base.canAlwaysEat(), base.eatSeconds(),
                    base.usingConvertsTo(), effects);
        }

        // DON_T_WANT
        if (!MIQConfig.ENABLE_PENALTY.get()) {
            return base;
        }
        // One shared factor scales both hunger-restore and saturation.
        float scale = Math.max(0.0f, 1.0f - (float) (double) MIQConfig.DONT_WANT_HUNGER_REDUCE.get());
        int nutrition = Math.max(1, Math.round(base.nutrition() * scale));
        float saturation = Math.max(0.0f, base.saturation() * scale);
        float durFactor = Math.max(0.0f, 1.0f - (float) (double) MIQConfig.DONT_WANT_EFFECT_DURATION_REDUCE.get());
        List<FoodProperties.PossibleEffect> effects = scaleEffectDurations(base.effects(), durFactor);
        if (rng.nextFloat() < MIQConfig.DONT_WANT_PENALTY_EFFECT_CHANCE.get()) {
            boolean hunger = rng.nextBoolean();
            int ticks = (hunger
                    ? MIQConfig.DONT_WANT_HUNGER_DURATION_SECONDS.get()
                    : MIQConfig.DONT_WANT_NAUSEA_DURATION_SECONDS.get()) * 20;
            MobEffectInstance penalty = new MobEffectInstance(hunger ? MobEffects.HUNGER : MobEffects.CONFUSION, ticks, 0);
            effects.add(new FoodProperties.PossibleEffect(() -> new MobEffectInstance(penalty), 1.0f));
        }
        return new FoodProperties(nutrition, saturation, base.canAlwaysEat(), base.eatSeconds(),
                base.usingConvertsTo(), effects);
    }

    /** Scales the tick-duration of every food-granted effect. 缩放食物自带效果的时间刻数。 */
    private static List<FoodProperties.PossibleEffect> scaleEffectDurations(
            List<FoodProperties.PossibleEffect> original, float factor) {
        if (factor <= 0.0f) {
            return new ArrayList<>();
        }
        List<FoodProperties.PossibleEffect> out = new ArrayList<>(original.size());
        for (FoodProperties.PossibleEffect possible : original) {
            MobEffectInstance instance = possible.effect(); // fresh copy produced by the supplier
            int newTicks = Math.max(1, Math.round(instance.getDuration() * factor));
            MobEffectInstance scaled = new MobEffectInstance(instance.getEffect(), newTicks, instance.getAmplifier());
            out.add(new FoodProperties.PossibleEffect(() -> new MobEffectInstance(scaled), possible.probability()));
        }
        return out;
    }
}