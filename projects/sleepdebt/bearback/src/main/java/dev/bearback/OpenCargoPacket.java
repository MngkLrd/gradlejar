package dev.bearback;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.animal.PolarBear;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client requests only; item contents and permissions stay server-authoritative. */
public record OpenCargoPacket(int bearEntityId) implements CustomPacketPayload {
    public static final Type<OpenCargoPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(Bearback.MOD_ID, "open_cargo"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenCargoPacket> CODEC =
            StreamCodec.composite(ByteBufCodecs.VAR_INT, OpenCargoPacket::bearEntityId,
                    OpenCargoPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void receive(OpenCargoPacket packet, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        if (player.level().getEntity(packet.bearEntityId()) instanceof PolarBear bear
                && player.getVehicle() == bear) {
            BearCargo.open(player, bear);
        }
    }
}
