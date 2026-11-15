package dev.voidchisel;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Heartbeat while attack is held. The server independently checks position and permissions. */
public record MiningInputPacket(long pos, boolean active) implements CustomPacketPayload {
    public static final Type<MiningInputPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(VoidChisel.MOD_ID, "mining_input"));

    public static final StreamCodec<RegistryFriendlyByteBuf, MiningInputPacket> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_LONG, MiningInputPacket::pos,
                    ByteBufCodecs.BOOL, MiningInputPacket::active,
                    MiningInputPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void receive(MiningInputPacket packet, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            BedrockMiningServer.receive(player, packet.active() ? BlockPos.of(packet.pos()) : null);
        }
    }
}
