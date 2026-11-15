package dev.minemods.traumacraft.network;

import dev.minemods.traumacraft.TraumaCraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record TreatmentProgressPacket(int durationTicks, int elapsedTicks) implements CustomPacketPayload {
    public static final Type<TreatmentProgressPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(TraumaCraft.MOD_ID, "treatment_progress"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TreatmentProgressPacket> CODEC =
            StreamCodec.of((buf, packet) -> {
                buf.writeVarInt(packet.durationTicks());
                buf.writeVarInt(packet.elapsedTicks());
            }, buf -> new TreatmentProgressPacket(buf.readVarInt(), buf.readVarInt()));
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
