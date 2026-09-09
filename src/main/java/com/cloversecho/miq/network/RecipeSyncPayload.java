package com.cloversecho.miq.network;

import com.cloversecho.miq.MIQ;
import com.cloversecho.miq.recipe.DailyRecipeManager;
import com.cloversecho.miq.recipe.DesireCategory;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.HashMap;
import java.util.Map;

/**
 * Carries the current daily recipe from the server to the client so tooltips can be shown.
 * 将服务器端生成的每日食谱同步到客户端，用于 Tooltip 显示。
 */
public record RecipeSyncPayload(Map<Item, DesireCategory> recipe) implements CustomPacketPayload {

    public static final Type<RecipeSyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(MIQ.MODID, "recipe_sync"));

    public static final StreamCodec<FriendlyByteBuf, RecipeSyncPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public RecipeSyncPayload decode(FriendlyByteBuf buf) {
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

        @Override
        public void encode(FriendlyByteBuf buf, RecipeSyncPayload payload) {
            buf.writeVarInt(payload.recipe.size());
            for (Map.Entry<Item, DesireCategory> e : payload.recipe.entrySet()) {
                buf.writeUtf(BuiltInRegistries.ITEM.getKey(e.getKey()).toString());
                buf.writeByte(e.getValue().ordinal());
            }
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** Applies the synced recipe on the client. 客户端收到后更新本地食谱。 */
    public static void handleOnClient(RecipeSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> DailyRecipeManager.setClientRecipe(payload.recipe()));
    }
}