package com.cloversecho.miq.network;

import com.cloversecho.miq.recipe.DailyRecipeManager;
import com.cloversecho.miq.recipe.DesireCategory;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Carries the current daily recipe from the server to the client so tooltips can be shown.
 * 将服务器端生成的每日食谱同步到客户端，用于 Tooltip 显示。
 *
 * <p>Items are encoded by their registry id and the desire category by its ordinal, so the
 * packet stays forward/backward tolerant: unknown items or out-of-range ordinals are skipped
 * on decode. 物品以注册表 ID、分类以枚举序号编码；解码时未知物品与越界序号会被跳过。
 */
public final class RecipeSyncPayload {

    private final Map<Item, DesireCategory> recipe;

    public RecipeSyncPayload(Map<Item, DesireCategory> recipe) {
        this.recipe = recipe == null ? Map.of() : new HashMap<>(recipe);
    }

    /** Returns the recipe carried by this packet. 获取包内携带的食谱。 */
    public Map<Item, DesireCategory> recipe() {
        return recipe;
    }

    public static void encode(RecipeSyncPayload payload, FriendlyByteBuf buf) {
        buf.writeVarInt(payload.recipe.size());
        for (Map.Entry<Item, DesireCategory> e : payload.recipe.entrySet()) {
            buf.writeUtf(BuiltInRegistries.ITEM.getKey(e.getKey()).toString());
            buf.writeByte(e.getValue().ordinal());
        }
    }

    public static RecipeSyncPayload decode(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        Map<Item, DesireCategory> map = new HashMap<>();
        for (int i = 0; i < size; i++) {
            String key = buf.readUtf();
            int ord = buf.readUnsignedByte();
            Item item = BuiltInRegistries.ITEM.get(ResourceLocation.tryParse(key));
            if (item != null && item != Items.AIR && ord >= 0 && ord < DesireCategory.values().length) {
                map.put(item, DesireCategory.values()[ord]);
            }
        }
        return new RecipeSyncPayload(map);
    }

    /** Applies the synced recipe on the client. 客户端收到后更新本地食谱。 */
    public static void handle(RecipeSyncPayload payload, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DailyRecipeManager.setClientRecipe(payload.recipe()));
        ctx.get().setPacketHandled(true);
    }
}
