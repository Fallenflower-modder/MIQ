package com.cloversecho.miq.client;

import com.cloversecho.miq.network.RecipeSyncPayload;
import com.cloversecho.miq.recipe.DailyRecipeManager;
import com.cloversecho.miq.recipe.DesireCategory;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

/**
 * Client-side entry point (Fabric).
 * 客户端入口点（Fabric）。
 *
 * Responsibilities / 职责：
 * - receive the recipe-sync payload from the server. / 接收服务端同步的每日食谱。
 * - render the dynamic desire tooltip line on food items. / 在食物物品上渲染动态欲望 Tooltip。
 */
public final class MIQClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // Apply the recipe synced from the server so tooltips work immediately.
        ClientPlayNetworking.registerGlobalReceiver(RecipeSyncPayload.TYPE, (payload, context) ->
                context.client().execute(() -> RecipeSyncPayload.handleOnClient(payload)));

        // Dynamic desire tooltip line, inserted right after the item name.
        ItemTooltipCallback.EVENT.register((stack, context, flag, lines) -> {
            DesireCategory cat = DailyRecipeManager.getDesireForStack(stack);
            if (cat == null) {
                return;
            }
            Component line = switch (cat) {
                case VERY_WANT -> Component.translatable("tooltip.miq.very_want").withStyle(ChatFormatting.GOLD);
                case WILLING -> Component.translatable("tooltip.miq.willing").withStyle(ChatFormatting.GRAY);
                case DON_T_WANT -> Component.translatable("tooltip.miq.dont_want").withStyle(ChatFormatting.DARK_GRAY);
            };
            if (!lines.contains(line)) {
                lines.add(Math.min(1, lines.size()), line);
            }
        });
    }
}
