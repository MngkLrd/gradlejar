package dev.minemods.traumacraft.network;

import dev.minemods.traumacraft.TraumaCraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import java.util.UUID;

public record ClimbPosePacket(UUID player,int arms,int elapsed,int duration) implements CustomPacketPayload {
    public static final Type<ClimbPosePacket> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath(TraumaCraft.MOD_ID,"climb_pose"));
    public static final StreamCodec<RegistryFriendlyByteBuf,ClimbPosePacket> CODEC=StreamCodec.of(
        (b,m)->{b.writeUUID(m.player);b.writeByte(m.arms);b.writeVarInt(m.elapsed);b.writeVarInt(m.duration);},
        b->new ClimbPosePacket(b.readUUID(),b.readUnsignedByte(),b.readVarInt(),b.readVarInt()));
    @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
