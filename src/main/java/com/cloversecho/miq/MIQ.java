package com.cloversecho.miq;

import com.cloversecho.miq.network.RecipeSyncPayload;
import com.cloversecho.miq.recipe.DailyRecipeManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

/**
 * Most Important Question mod entry point (Fabric).
 * 每日灵魂拷问（Most Important Question）模组主类（Fabric 版）。
 *
 * Responsibilities / 职责：
 * - register the recipe-sync network payload. / 注册食谱同步网络包。
 * - hook server lifecycle events (start/stop/tick). / 注册服务端生命周期与 tick 事件。
 * - register the {@code /miq refresh} command. / 注册 /miq refresh 指令。
 * - sync the recipe to players on login. / 玩家登录时同步食谱。
 */
public final class MIQ implements ModInitializer {

    public static final String MODID = "miq";

    @Override
    public void onInitialize() {
        // Recipe-sync payload (server -> client). 注册食谱同步网络包（服务端 -> 客户端）。
        PayloadTypeRegistry.playS2C().register(RecipeSyncPayload.TYPE, RecipeSyncPayload.STREAM_CODEC);

        // Startup/load, shutdown/save, and per-tick recipe timing.
        ServerLifecycleEvents.SERVER_STARTED.register(DailyRecipeManager::onServerStart);
        ServerLifecycleEvents.SERVER_STOPPING.register(DailyRecipeManager::onServerStopping);
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            ServerLevel level = server.overworld();
            if (level != null) {
                DailyRecipeManager.serverTick(level);
            }
        });

        // The /miq refresh command (operator permission level 2).
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(
                        Commands.literal("miq")
                                .requires(src -> src.hasPermission(2))
                                .then(Commands.literal("refresh").executes(ctx -> {
                                    ServerLevel level = ctx.getSource().getLevel();
                                    DailyRecipeManager.refreshNow(level);
                                    ctx.getSource().sendSuccess(() -> Component.translatable("command.miq.refresh"), true);
                                    return 1;
                                }))));

        // Send the current recipe to a player right after they log in.
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                DailyRecipeManager.syncTo(handler.player));
    }
}
