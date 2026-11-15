package dev.bearback;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record BearRidePacket(float forward, float strafe, boolean jump, boolean attack, float yaw)
        implements CustomPacketPayload {
    public static final Type<BearRidePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Bearback.MOD_ID, "ride"));
    public static final StreamCodec<RegistryFriendlyByteBuf, BearRidePacket> CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, BearRidePacket::forward,
            ByteBufCodecs.FLOAT, BearRidePacket::strafe,
            ByteBufCodecs.BOOL, BearRidePacket::jump,
            ByteBufCodecs.BOOL, BearRidePacket::attack,
            ByteBufCodecs.FLOAT, BearRidePacket::yaw,
            BearRidePacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void receive(BearRidePacket packet, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            BearEvents.ride(player, packet.forward, packet.strafe, packet.jump, packet.attack, packet.yaw);
        }
    }
}
