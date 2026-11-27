package dev.minemods.traumacraft.network;

import dev.minemods.traumacraft.TraumaCraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import java.util.UUID;

/** Server-owned route. Zero arms: completed if elapsed=duration>0, otherwise interrupted. */
public record ClimbPosePacket(UUID player,int arms,int elapsed,int duration,Vec3 start,Vec3 end) implements CustomPacketPayload {
    public static final Type<ClimbPosePacket> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath(TraumaCraft.MOD_ID,"climb_pose"));
    private static void write(RegistryFriendlyByteBuf b,Vec3 v){b.writeDouble(v.x);b.writeDouble(v.y);b.writeDouble(v.z);}
    private static Vec3 read(RegistryFriendlyByteBuf b){return new Vec3(b.readDouble(),b.readDouble(),b.readDouble());}
    public static final StreamCodec<RegistryFriendlyByteBuf,ClimbPosePacket> CODEC=StreamCodec.of(
        (b,m)->{b.writeUUID(m.player);b.writeByte(m.arms);b.writeVarInt(m.elapsed);b.writeVarInt(m.duration);write(b,m.start);write(b,m.end);},
        b->new ClimbPosePacket(b.readUUID(),b.readUnsignedByte(),b.readVarInt(),b.readVarInt(),read(b),read(b)));
    @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
