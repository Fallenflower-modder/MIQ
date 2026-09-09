package com.cloversecho.miq;

import com.cloversecho.miq.compat.AppleSkinCompat;
import com.cloversecho.miq.config.MIQConfig;
import com.cloversecho.miq.event.GameEventHandlers;
import com.cloversecho.miq.network.MIQNetwork;
import com.cloversecho.miq.recipe.DailyRecipeManager;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/**
 * Most Important Question mod entry point.
 * 每日灵魂拷问（Most Important Question）模组主类。
 *
 * Responsibilities / 职责：
 * - register the {@code miq-common.toml} config. / 注册配置文件。
 * - register the recipe-sync network channel. / 注册食谱同步网络通道。
 * - hook the game event bus handlers. / 注册游戏事件总线监听。
 */
@Mod(MIQ.MODID)
public final class MIQ {

    public static final String MODID = "miq";

    public MIQ() {
        // Forge 1.20.1 requires a public no-arg constructor; the mod event bus is
        // obtained from FMLJavaModLoadingContext (unlike Neoforge's injected constructor args).
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Register the common config (generated at config/miq-common.toml).
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, MIQConfig.SPEC);

        // Register the recipe-sync channel (server -> client) during common setup.
        modBus.addListener(this::onCommonSetup);

        // Game-bus handlers: server tick timing, tooltips, join sync.
        MinecraftForge.EVENT_BUS.register(new GameEventHandlers());

        // Register AppleSkin food-value compatibility only when AppleSkin is installed,
        // so the mod stays functional (and starts cleanly) without it.
        if (ModList.get().isLoaded("appleskin")) {
            MinecraftForge.EVENT_BUS.register(new AppleSkinCompat());
        }

        // Startup/load, shutdown/save, and the /miq refresh command.
        MinecraftForge.EVENT_BUS.addListener(this::onServerStart);
        MinecraftForge.EVENT_BUS.addListener(this::onServerStopping);
        MinecraftForge.EVENT_BUS.addListener(this::registerCommands);
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        MIQNetwork.register();
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
}
