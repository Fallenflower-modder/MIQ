package com.cloversecho.miq.event;

import com.cloversecho.miq.recipe.DailyRecipeManager;
import com.cloversecho.miq.recipe.DesireCategory;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Registers listeners on the game event bus ({@link net.neoforged.neoforge.common.NeoForge#EVENT_BUS}):
 * server tick (recipe timing), dynamic tooltips and join-time sync.
 *
 * 在游戏事件总线（NeoForge#EVENT_BUS）上注册监听：
 * 服务器 tick（食谱计时）、动态 Tooltip、玩家加入时同步食谱。
 */
public final class GameEventHandlers {

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        ServerLevel level = event.getServer().overworld();
        if (level != null) {
            DailyRecipeManager.serverTick(level);
        }
    }

    /** Adds a dynamic desire tooltip line to food items. 为食物物品动态添加欲望 Tooltip。 */
    @SubscribeEvent
    public void onItemTooltip(ItemTooltipEvent event) {
        if (event.getEntity() == null) {
            return;
        }
        DesireCategory cat = DailyRecipeManager.getDesireForStack(event.getItemStack());
        if (cat == null) {
            return;
        }
        Component line = switch (cat) {
            case VERY_WANT -> Component.translatable("tooltip.miq.very_want").withStyle(ChatFormatting.GOLD);
            case WILLING -> Component.translatable("tooltip.miq.willing").withStyle(ChatFormatting.GRAY);
            case DON_T_WANT -> Component.translatable("tooltip.miq.dont_want").withStyle(ChatFormatting.DARK_GRAY);
        };
        // Insert right after the item name for visibility; avoid duplicate when the line already exists.
        java.util.List<Component> tooltip = event.getToolTip();
        if (!tooltip.contains(line)) {
            tooltip.add(Math.min(1, tooltip.size()), line);
        }
    }

    /** Sends the current recipe to a player right after they log in. 玩家登录后立即同步当前食谱。 */
    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            DailyRecipeManager.syncTo(serverPlayer);
        }
    }
}