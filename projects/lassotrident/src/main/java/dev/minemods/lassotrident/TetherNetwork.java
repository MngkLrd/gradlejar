package dev.minemods.lassotrident;

import dev.minemods.lassotrident.client.ClientTetherEvents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.PayloadChannel;

public final class TetherNetwork {
    private TetherNetwork() {}

    public record Control(byte action) implements CustomPacketPayload {
        public static final Type<Control> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(LassoTrident.MOD_ID, "control"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Control> CODEC =
                StreamCodec.composite(ByteBufCodecs.BYTE, Control::action, Control::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record Sync(int tridentId) implements CustomPacketPayload {
        public static final Type<Sync> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(LassoTrident.MOD_ID, "sync"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Sync> CODEC =
                StreamCodec.composite(ByteBufCodecs.VAR_INT, Sync::tridentId, Sync::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    private static final PayloadChannel CHANNEL = ChannelBuilder
            .named(ResourceLocation.fromNamespaceAndPath(LassoTrident.MOD_ID, "network"))
            .networkProtocolVersion(1)
            .payloadChannel()
            .play()
            .serverbound()
            .add(Control.TYPE, Control.CODEC, TetherNetwork::receiveControl)
            .clientbound()
            .add(Sync.TYPE, Sync.CODEC, TetherNetwork::receiveSync)
            .build();

    public static void init() { /* Forces channel registration at mod construction. */ }

    private static void receiveControl(Control message, CustomPayloadEvent.Context context) {
        ServerPlayer sender = context.getSender();
        if (sender == null || message.action() < 0 || message.action() > 1) return;
        context.enqueueWork(() -> TetherEvents.onControl(sender, message.action() == 1));
        context.setPacketHandled(true);
    }

    private static void receiveSync(Sync message, CustomPayloadEvent.Context context) {
        if (context.isClientSide()) {
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                    Dist.CLIENT, () -> () -> ClientTetherEvents.setAnchor(message.tridentId())));
        }
        context.setPacketHandled(true);
    }

    public static void sendControl(boolean pressed) {
        CHANNEL.send(new Control((byte) (pressed ? 1 : 0)), PacketDistributor.SERVER.noArg());
    }

    public static void sendSync(ServerPlayer player, int id) {
        CHANNEL.send(new Sync(id), PacketDistributor.PLAYER.with(player));
    }
}
