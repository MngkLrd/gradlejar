package dev.minemods.traumacraft.network;

import dev.minemods.traumacraft.TraumaCraft;
import dev.minemods.traumacraft.health.BodyPart;
import dev.minemods.traumacraft.health.BodyState;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Seven local HP values and injury flags; server -> player only. */
public record SyncBodyStatePacket(float[] hp, byte[] bleeding, boolean[] fracture, int concussion, int painkiller) implements CustomPacketPayload {
    public static final Type<SyncBodyStatePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(TraumaCraft.MOD_ID, "body_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncBodyStatePacket> STREAM_CODEC =
            StreamCodec.of((buf, packet) -> {
                for (int i = 0; i < BodyPart.values().length; i++) {
                    buf.writeFloat(packet.hp[i]);
                    buf.writeByte(packet.bleeding[i]);
                    buf.writeBoolean(packet.fracture[i]);
                }
                buf.writeVarInt(packet.concussion);
                buf.writeVarInt(packet.painkiller);
            }, buf -> {
                int n = BodyPart.values().length;
                float[] h = new float[n];
                byte[] b = new byte[n];
                boolean[] f = new boolean[n];
                for (int i = 0; i < n; i++) {
                    h[i] = buf.readFloat();
                    b[i] = buf.readByte();
                    f[i] = buf.readBoolean();
                }
                return new SyncBodyStatePacket(h, b, f, buf.readVarInt(), buf.readVarInt());
            });

    public static SyncBodyStatePacket from(BodyState state) {
        int n = BodyPart.values().length;
        float[] h = new float[n];
        byte[] b = new byte[n];
        boolean[] f = new boolean[n];
        for (BodyPart part : BodyPart.values()) {
            int i = part.ordinal();
            h[i] = state.health(part);
            b[i] = (byte) state.bleeding(part);
            f[i] = state.fractured(part);
        }
        return new SyncBodyStatePacket(h, b, f, state.concussionTicks(), state.painkillerTicks());
    }
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
