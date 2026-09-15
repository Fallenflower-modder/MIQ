package com.cloversecho.miq;

import com.cloversecho.miq.compat.AppleSkinCompat;
import com.cloversecho.miq.compat.SarCompat;
import com.cloversecho.miq.config.MIQConfig;
import com.cloversecho.miq.event.GameEventHandlers;
import com.cloversecho.miq.network.RecipeSyncPayload;
import com.cloversecho.miq.recipe.DailyRecipeManager;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

/**
 * Most Important Question mod entry point.
 * 每日灵魂拷问（Most Important Question）模组主类。
 *
 * Responsibilities / 职责：
 * - register the {@code miq-common.toml} config. / 注册配置文件。
 * - register the recipe-sync network payload. / 注册食谱同步网络包。
 * - hook the game event bus handlers. / 注册游戏事件总线监听。
 */
@Mod(MIQ.MODID)
public final class MIQ {

    public static final String MODID = "miq";

    public MIQ(IEventBus modBus, ModContainer container) {
        // Register the common config (generated at config/miq-common.toml).
        container.registerConfig(ModConfig.Type.COMMON, MIQConfig.SPEC);

        // Register the recipe-sync payload (server -> client).
        modBus.addListener(this::registerPayloads);

        // Game-bus handlers: server tick timing, tooltips, join sync.
        NeoForge.EVENT_BUS.register(new GameEventHandlers());

        // Register AppleSkin food-value compatibility only when AppleSkin is installed,
        // so the mod stays functional (and starts cleanly) without it.
        if (ModList.get().isLoaded("appleskin")) {
            NeoForge.EVENT_BUS.register(new AppleSkinCompat());
        }

        // Install a stack-based desire resolver for Some Assembly Required sandwiches only when SAR is
        // loaded, so tooltips/eats derive the sandwich's whole desire from its ingredients (dynamic per refresh).
        if (ModList.get().isLoaded("someassemblyrequired")) {
            DailyRecipeManager.registerStackDesireResolver(SarCompat.RESOLVER);
        }

        // Startup/load, shutdown/save, and the /miq refresh command.
        NeoForge.EVENT_BUS.addListener(this::onServerStart);
        NeoForge.EVENT_BUS.addListener(this::onServerStopping);
        NeoForge.EVENT_BUS.addListener(this::registerCommands);
    }

    /** On server start: load the persisted daily recipe or generate a fresh one if none exists. */
    private void onServerStart(ServerStartedEvent event) {
        DailyRecipeManager.onServerStart(event.getServer());
    }

    /** On server stop: write the current daily recipe into the world data. */
    private void onServerStopping(ServerStoppingEvent event) {
        DailyRecipeManager.onServerStopping(event.getServer());
    }

    /** Registers the {@code /miq refresh} command (operator permission). */
    private void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("miq")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.literal("refresh").executes(ctx -> {
                            ServerLevel level = ctx.getSource().getLevel();
                            DailyRecipeManager.refreshNow(level);
                            ctx.getSource().sendSuccess(() -> Component.translatable("command.miq.refresh"), true);
                            return 1;
                        })));
    }

    private void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(MODID).versioned("1");
        registrar.playToClient(RecipeSyncPayload.TYPE, RecipeSyncPayload.STREAM_CODEC, RecipeSyncPayload::handleOnClient);
    }
}