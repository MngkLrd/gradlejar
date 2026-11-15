package dev.quickstacknearby.network;

import dev.quickstacknearby.QuickStackNearby;
import dev.quickstacknearby.server.QuickStackService;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.network.Channel;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.PacketDistributor;

public final class QuickStackNetwork {
    private static Channel<CustomPacketPayload> channel;
    private QuickStackNetwork() {}

    public static void register() {
        channel = ChannelBuilder
            .named(ResourceLocation.fromNamespaceAndPath(QuickStackNearby.MOD_ID, "main"))
            .networkProtocolVersion(1)
            .payloadChannel()
            .play()
            .serverbound()
            .addMain(QuickStackRequest.TYPE, QuickStackRequest.CODEC, QuickStackNetwork::handle)
            .build();
    }

    private static void handle(QuickStackRequest packet, CustomPayloadEvent.Context context) {
        if (context.getSender() != null) QuickStackService.execute(context.getSender());
    }

    public static void request() {
        if (channel != null) channel.send(QuickStackRequest.INSTANCE, PacketDistributor.SERVER.noArg());
    }
}
