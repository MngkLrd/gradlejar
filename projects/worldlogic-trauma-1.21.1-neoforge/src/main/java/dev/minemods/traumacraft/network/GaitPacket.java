package dev.minemods.traumacraft.network;
import dev.minemods.traumacraft.TraumaCraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import java.util.UUID;
/** Minimal public animation state; no private medical data sent to observers. */
public record GaitPacket(UUID player,float left,float right) implements CustomPacketPayload {
    public static final Type<GaitPacket> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath(TraumaCraft.MOD_ID,"gait"));
    public static final StreamCodec<RegistryFriendlyByteBuf,GaitPacket> CODEC=StreamCodec.of(
        (b,m)->{b.writeUUID(m.player);b.writeFloat(m.left);b.writeFloat(m.right);},
        b->new GaitPacket(b.readUUID(),b.readFloat(),b.readFloat()));
    @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
