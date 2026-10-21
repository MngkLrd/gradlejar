package dev.minemods.traumacraft.network;
import dev.minemods.traumacraft.TraumaCraft;
import dev.minemods.traumacraft.health.BodyPart;
import dev.minemods.traumacraft.health.BodyState;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record SyncBodyStatePacket(float[] hp,byte[] bleeding,boolean[] fracture) implements CustomPacketPayload {
    public static final Type<SyncBodyStatePacket> TYPE=new Type<>(Identifier.fromNamespaceAndPath(TraumaCraft.MOD_ID,"body_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf,SyncBodyStatePacket> STREAM_CODEC=StreamCodec.of(
        (b,p)->{for(int i=0;i<7;i++){b.writeFloat(p.hp[i]);b.writeByte(p.bleeding[i]);b.writeBoolean(p.fracture[i]);}},
        b->{float[] h=new float[7];byte[] d=new byte[7];boolean[] f=new boolean[7];
            for(int i=0;i<7;i++){h[i]=b.readFloat();d[i]=b.readByte();f[i]=b.readBoolean();}
            return new SyncBodyStatePacket(h,d,f);});
    public static SyncBodyStatePacket from(BodyState s){
        float[] h=new float[7];byte[] d=new byte[7];boolean[] f=new boolean[7];
        for(BodyPart p:BodyPart.values()){int i=p.ordinal();h[i]=s.health(p);d[i]=(byte)s.bleeding(p);f[i]=s.fractured(p);}
        return new SyncBodyStatePacket(h,d,f);
    }
    @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
