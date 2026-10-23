package dev.minemods.lassotrident;

import dev.minemods.lassotrident.client.ClientTetherEvents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class TetherNetwork {
    private TetherNetwork() {}

    public record Control(byte action) implements CustomPacketPayload {
        public static final Type<Control> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(LassoTrident.MOD_ID, "control"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Control> CODEC =
                StreamCodec.composite(ByteBufCodecs.BYTE, Control::action, Control::new);
        @Override public Type<Control> type() { return TYPE; }
    }

    public record Sync(int tridentId) implements CustomPacketPayload {
        public static final Type<Sync> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(LassoTrident.MOD_ID, "sync"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Sync> CODEC =
                StreamCodec.composite(ByteBufCodecs.VAR_INT, Sync::tridentId, Sync::new);
        @Override public Type<Sync> type() { return TYPE; }
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");
        registrar.playToServer(Control.TYPE, Control.CODEC, (message, context) -> {
            if (context.player() instanceof ServerPlayer player && message.action() <= 1 && message.action() >= 0) {
                TetherEvents.onControl(player, message.action() == 1);
            }
        });
        registrar.playToClient(Sync.TYPE, Sync.CODEC,
                (message, context) -> ClientTetherEvents.setAnchor(message.tridentId()));
    }

    public static void sendControl(boolean pressed) {
        PacketDistributor.sendToServer(new Control((byte) (pressed ? 1 : 0)));
    }

    public static void sendSync(ServerPlayer player, int id) {
        PacketDistributor.sendToPlayer(player, new Sync(id));
    }
}
