package com.cloversecho.miq.config;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;

/**
 * Mod configuration. The generated file is {@code config/miq-common.toml}.
 * 模组配置文件，生成位置为 {@code config/miq-common.toml}。
 * All options carry bilingual (中文/English) comments for player convenience.
 * 所有配置项均带中英双语注释，便于玩家修改。
 */
public final class MIQConfig {

    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    // ---------------------------------------------------------------------
    // Daily recipe / 每日食谱
    // ---------------------------------------------------------------------
    static {
        BUILDER.comment(
                "Daily recipe / 每日食谱",
                "",
                "Controls when the daily recipe is re-generated each game day.",
                "控制每个游戏日何时重新生成每日食谱。")
                .push("recipe");
    }

    /**
     * Tick-of-day values (0~24000) at which the recipe is refreshed.
     * 每日食谱更新节点列表（0~24000 内的整数数组）。
     * System refreshes the recipe once at each such moment during each game day.
     * 系统会在每个游戏日的这些时刻各更新一次食谱。
     */
    public static final ForgeConfigSpec.ConfigValue<List<? extends Integer>> RECIPE_UPDATE_NODES = BUILDER
            .comment(
                    "Tick-of-day values at which the daily recipe updates (0 to 24000, integer array). ",
                    "One update is fired per crossed node per game day; the update-only-once rule applies even when",
                    "several nodes are skipped over in a single tick.",
                    "每日食谱更新时刻（0~24000 的整数数组，允许多个）。",
                    "系统会在每个游戏日的这些时刻各更新一次食谱；即使一次 tick 越过多个节点也只会触发一次更新。")
            .defineListAllowEmpty("recipeUpdateNodes", List.of(3000),
                    obj -> obj instanceof Integer i && i >= 0 && i < 24000);

    static {
        BUILDER.pop();
    }

    // ---------------------------------------------------------------------
    // Desire reward (very-want-to-eat) / 欲望奖励（今天很想吃）
    // ---------------------------------------------------------------------
    static {
        BUILDER.comment(
                "Desire reward, applied when eating an item marked 'very want to eat'.",
                "欲望奖励参数：当食用被划分为“今天很想吃”的食物时生效。")
                .push("desireReward");
    }

    /**
     * How much hunger restore is boosted. 饱食度恢复提升量（小数）。
     */
    public static final ForgeConfigSpec.DoubleValue VERY_WANT_HUNGER_BOOST = BUILDER
            .comment("Hunger-restore increase as a fraction (0.25 = +25%). / 饱食度恢复提升量（0.25 表示提高 25%）。")
            .defineInRange("hungerBoost", 0.25, 0.0, 10.0);

    /**
     * How much the duration of effects granted by the food is boosted. 食用食物获得的效果时长提升量。
     */
    public static final ForgeConfigSpec.DoubleValue VERY_WANT_EFFECT_DURATION_BOOST = BUILDER
            .comment("Boost to the duration of food-granted effects as a fraction (0.25 = +25%).",
                    "/ 食用食物获得的效果时长提升量（0.25 表示延长 25%）。")
            .defineInRange("effectDurationBoost", 0.25, 0.0, 10.0);

    /**
     * Whether to grant the extra Luck effect. 是否启用额外的幸运效果。
     */
    public static final ForgeConfigSpec.BooleanValue VERY_WANT_EXTRA_LUCK = BUILDER
            .comment("Whether eating a 'very want' food also grants the extra Luck effect.",
                    "/ 是否启用额外幸运效果（食用“很想吃”食物时额外获得幸运）。")
            .define("extraLuck", true);

    /**
     * Duration of the extra Luck effect, in seconds. 额外幸运效果的时长（秒）。
     */
    public static final ForgeConfigSpec.IntValue VERY_WANT_LUCK_DURATION_SECONDS = BUILDER
            .comment("Duration of the extra Luck effect in seconds. / 额外幸运效果的持续时长（秒）。")
            .defineInRange("luckDurationSeconds", 60, 1, 100000);

    static {
        BUILDER.pop();
    }

    // ---------------------------------------------------------------------
    // Desire penalty / 欲望惩罚（今天不想吃）
    // ---------------------------------------------------------------------
    static {
        BUILDER.comment(
                "Desire penalty, applied when eating an item marked 'don't want to eat'.",
                "欲望惩罚参数：当食用被划分为“今天不想吃”的食物时生效。")
                .push("desirePenalty");
    }

    /**
     * Master switch for the penalty. When disabled, no item is ever put in the 'don't want' bucket.
     * 欲望惩罚总开关：关闭后不会有食物被划分到“不想吃”列表。
     */
    public static final ForgeConfigSpec.BooleanValue ENABLE_PENALTY = BUILDER
            .comment("Master switch for the 'don't want to eat' penalty. When disabled, no food is put into the",
                    "'don't want' category.",
                    "/ 是否启用欲望惩罚。关闭后不会有食物被划分到“不想吃”列表。")
            .define("enablePenalty", true);

    /**
     * How much hunger restore is reduced. 饱食度恢复降低量（小数）。
     */
    public static final ForgeConfigSpec.DoubleValue DONT_WANT_HUNGER_REDUCE = BUILDER
            .comment("Hunger-restore reduction as a fraction (0.5 = -50%). / 饱食度恢复降低量（0.5 表示降低 50%）。")
            .defineInRange("hungerReduce", 0.5, 0.0, 1.0);

    /**
     * Effect-duration reduction. 食用食物获得的效果时长缩减量（小数）。
     */
    public static final ForgeConfigSpec.DoubleValue DONT_WANT_EFFECT_DURATION_REDUCE = BUILDER
            .comment("Reduction to the duration of food-granted effects as a fraction (0.25 = -25%).",
                    "/ 食用食物获得的效果时长缩减量（0.25 表示缩短 25%）。")
            .defineInRange("effectDurationReduce", 0.25, 0.0, 1.0);

    /**
     * Chance to get the negative effect (hunger or nausea) when eating 'don't want' food.
     * 食用“不想吃”食物时触发负面效果（饥饿或反胃）的概率。
     */
    public static final ForgeConfigSpec.DoubleValue DONT_WANT_PENALTY_EFFECT_CHANCE = BUILDER
            .comment("Chance (0.05 = 5%) to gain Hunger I or Nausea I for a short time when eating 'don't want' food.",
                    "/ 食用“不想吃”食物时，有该概率（0.05 表示 5%）获得短暂的饥饿或反胃效果。")
            .defineInRange("penaltyEffectChance", 0.05, 0.0, 1.0);

    /**
     * Duration of the granted Hunger effect, in seconds. 获得饥饿效果的时长（秒）。
     */
    public static final ForgeConfigSpec.IntValue DONT_WANT_HUNGER_DURATION_SECONDS = BUILDER
            .comment("Duration of the granted Hunger I effect in seconds. / 获得饥饿 I 效果的时长（秒）。")
            .defineInRange("hungerDurationSeconds", 3, 1, 100000);

    /**
     * Duration of the granted Nausea effect, in seconds. 获得反胃效果的时长（秒）。
     */
    public static final ForgeConfigSpec.IntValue DONT_WANT_NAUSEA_DURATION_SECONDS = BUILDER
            .comment("Duration of the granted Nausea I effect in seconds. / 获得反胃 I 效果的时长（秒）。")
            .defineInRange("nauseaDurationSeconds", 3, 1, 100000);

    static {
        BUILDER.pop();
    }

    // ---------------------------------------------------------------------
    // Desire generation weights / 欲望生成比例
    // ---------------------------------------------------------------------
    static {
        BUILDER.comment(
                "Random weights used when assigning each food to a desire category.",
                "用于把每种食物随机划分到某一欲望分类的权重。")
                .push("desireWeights");
    }

    /**
     * Weights in order (very-want, willing, don't-want). Default [1,5,4] ≈ 10/50/40%.
     * 三个整数，依次表示：很想吃、愿意吃、不想吃 的随机比例权重。
     */
    public static final ForgeConfigSpec.ConfigValue<List<? extends Integer>> DESIRE_WEIGHTS = BUILDER
            .comment(
                    "Three positive integers, in order: very-want, willing, don't-want.",
                    "Default [1, 5, 4] ≈ 10% / 50% / 40%. When the desire penalty is disabled,",
                    "the 'don't want' weight is ignored and the remaining two are used.",
                    "三个正整数，顺序为：很想吃、愿意吃、不想吃。",
                    "默认 [1, 5, 4]，约等于 10% / 50% / 40%。",
                    "当欲望惩罚关闭时，“不想吃”的权重会被忽略，只使用前两个。")
            .defineListAllowEmpty("weights", List.of(1, 5, 4),
                    obj -> obj instanceof Integer i && i > 0);

    /**
     * How strongly each eaten count shifts a food's category weight at refresh time.
     * E.g. 0.2 means 1 eat → weight factor 1.2, 5 eats → factor 2.0 for the "don't want" side.
     * 进食次数对食谱生成的权重影响强度：每进食一次，“不想吃”侧权重 ×(1+强度)、
     * “很想吃”侧权重 ÷(1+强度)。0 表示完全关闭该影响。
     */
    public static final ForgeConfigSpec.DoubleValue EAT_COUNT_AFFINITY = BUILDER
            .comment("Per-eat weight shift when generating the recipe. The more a food was eaten, the more it is",
                    "pushed toward 'don't want' and away from 'very want'. 0 disables this influence.",
                    "进食次数对食谱生成的权重影响强度：被吃得越多的食物，越容易被划到“不想吃”、",
                    "越难被划到“很想吃”。0 表示关闭该影响。")
            .defineInRange("eatCountAffinity", 0.2, 0.0, 10.0);

    static {
        BUILDER.pop();
    }

    // ---------------------------------------------------------------------
    // Fixed preferences / 固定喜好分类
    // ---------------------------------------------------------------------
    static {
        BUILDER.comment(
                "Item IDs that are always forced into a fixed desire category when the recipe is refreshed.",
                "固定分类配置：这些食物的物品 ID 在刷新食谱时会被强制划分到指定分类（不参与随机）。")
                .push("fixedPreferences");
    }

    /**
     * Items always placed in the 'very want' category at refresh.
     * 固定划入“很想吃”分类的物品 ID 列表。
     */
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> FIXED_VERY_WANT = BUILDER
            .comment("Item registry IDs always placed in the 'very want' category, e.g. minecraft:enchanted_golden_apple.",
                    "固定划入“很想吃”分类的物品 ID 列表。例如 minecraft:enchanted_golden_apple。")
            .defineListAllowEmpty("fixedVeryWant", List.of("minecraft:enchanted_golden_apple"),
                    obj -> obj instanceof String);

    /**
     * Items always placed in the 'willing' category at refresh.
     * 固定划入“愿意吃”分类的物品 ID 列表。
     */
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> FIXED_WILLING = BUILDER
            .comment("Item registry IDs always placed in the 'willing' category.",
                    "固定划入“愿意吃”分类的物品 ID 列表。")
            .defineListAllowEmpty("fixedWilling", List.of(),
                    obj -> obj instanceof String);

    /**
     * Items always placed in the 'don't want' category at refresh.
     * 固定划入“不想吃”分类的物品 ID 列表。
     */
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> FIXED_DONT_WANT = BUILDER
            .comment("Item registry IDs always placed in the 'don't want' category, e.g. minecraft:spider_eye.",
                    "固定划入“不想吃”分类的物品 ID 列表。例如 minecraft:spider_eye。")
            .defineListAllowEmpty("fixedDontWant", List.of("minecraft:spider_eye", "minecraft:pufferfish"),
                    obj -> obj instanceof String);

    static {
        BUILDER.pop();
    }

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    private MIQConfig() {
    }
}