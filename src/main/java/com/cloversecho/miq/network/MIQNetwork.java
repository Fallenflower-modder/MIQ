package com.cloversecho.miq.network;

import com.cloversecho.miq.MIQ;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * Forge {@link SimpleChannel} for the MIQ recipe-sync packets (server -> client).
 * Registered during {@code FMLCommonSetupEvent}; {@code PacketDistributor.PLAYER} restricts
 * sending to the play phase so the channel never needs login packets.
 *
 * Forge 1.20.1 的 SimpleChannel 通道，用于 MIQ 的食谱同步包（服务器 -> 客户端）。
 * 在 FMLCommonSetupEvent 中注册；仅使用 PLAYER 分发器（游戏内阶段），无需登录包。
 */
public final class MIQNetwork {

    public static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(MIQ.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals);

    private MIQNetwork() {
    }

    /** Registers the {@link RecipeSyncPayload} message on the channel. 注册食谱同步消息。 */
    public static void register() {
        CHANNEL.messageBuilder(RecipeSyncPayload.class, 0)
                .encoder(RecipeSyncPayload::encode)
                .decoder(RecipeSyncPayload::decode)
                .consumerMainThread(RecipeSyncPayload::handle)
                .add();
    }

    /** Sends the payload to a single connected player. 向指定玩家发送同步包。 */
    public static void sendToPlayer(ServerPlayer player, RecipeSyncPayload payload) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), payload);
    }
}
