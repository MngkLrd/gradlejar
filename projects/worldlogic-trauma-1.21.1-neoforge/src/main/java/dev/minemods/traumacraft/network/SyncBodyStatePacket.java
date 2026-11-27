package dev.minemods.traumacraft.network;
import dev.minemods.traumacraft.TraumaCraft;
import dev.minemods.traumacraft.health.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
public record SyncBodyStatePacket(float[] hp,byte[] bleeding,boolean[] fracture,boolean[] stabilized,int concussion,int painkiller) implements CustomPacketPayload {
    public static final Type<SyncBodyStatePacket> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath(TraumaCraft.MOD_ID,"body_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf,SyncBodyStatePacket> STREAM_CODEC=StreamCodec.of((buf,m)->{
        for(int i=0;i<BodyPart.values().length;i++){
            buf.writeFloat(m.hp[i]);buf.writeByte(m.bleeding[i]);buf.writeBoolean(m.fracture[i]);buf.writeBoolean(m.stabilized[i]);
        }
        buf.writeVarInt(m.concussion);buf.writeVarInt(m.painkiller);
    },buf->{
        int n=BodyPart.values().length;
        float[] h=new float[n];byte[] bleed=new byte[n];boolean[] f=new boolean[n],s=new boolean[n];
        for(int i=0;i<n;i++){h[i]=buf.readFloat();bleed[i]=buf.readByte();f[i]=buf.readBoolean();s[i]=buf.readBoolean();}
        return new SyncBodyStatePacket(h,bleed,f,s,buf.readVarInt(),buf.readVarInt());
    });
    @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    public static SyncBodyStatePacket from(BodyState b){
        int n=BodyPart.values().length;
        float[] h=new float[n];byte[] bleed=new byte[n];boolean[] f=new boolean[n],s=new boolean[n];
        for(BodyPart p:BodyPart.values()){int i=p.ordinal();h[i]=b.health(p);bleed[i]=(byte)b.bleeding(p);f[i]=b.fractured(p);s[i]=b.stabilized(p);}
        return new SyncBodyStatePacket(h,bleed,f,s,b.concussionTicks(),b.painkillerTicks());
    }
}
