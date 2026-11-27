package dev.minemods.traumacraft.network;
import dev.minemods.traumacraft.TraumaCraft;
import dev.minemods.traumacraft.health.*;
import dev.minemods.traumacraft.disease.DiseaseModel;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
public record SyncBodyStatePacket(float[] hp,byte[] bleeding,byte[] wounds,boolean[] fracture,boolean[] stabilized,int concussion,int painkiller,int thermalStage,float bloodMl,float pain,int shock,int illnessKind,int illnessStage,boolean illnessTreated) implements CustomPacketPayload {
    public static final Type<SyncBodyStatePacket> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath(TraumaCraft.MOD_ID,"body_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf,SyncBodyStatePacket> STREAM_CODEC=StreamCodec.of((buf,m)->{
        for(int i=0;i<BodyPart.values().length;i++){
            buf.writeFloat(m.hp[i]);buf.writeByte(m.bleeding[i]);buf.writeByte(m.wounds[i]);buf.writeBoolean(m.fracture[i]);buf.writeBoolean(m.stabilized[i]);
        }
        buf.writeVarInt(m.concussion);buf.writeVarInt(m.painkiller);buf.writeVarInt(m.thermalStage);buf.writeFloat(m.bloodMl);buf.writeFloat(m.pain);buf.writeVarInt(m.shock);buf.writeVarInt(m.illnessKind);buf.writeVarInt(m.illnessStage);buf.writeBoolean(m.illnessTreated);
    },buf->{
        int n=BodyPart.values().length;
        float[] h=new float[n];byte[] bleed=new byte[n],wounds=new byte[n];boolean[] f=new boolean[n],s=new boolean[n];
        for(int i=0;i<n;i++){h[i]=buf.readFloat();bleed[i]=buf.readByte();wounds[i]=buf.readByte();f[i]=buf.readBoolean();s[i]=buf.readBoolean();}
        return new SyncBodyStatePacket(h,bleed,wounds,f,s,buf.readVarInt(),buf.readVarInt(),buf.readVarInt(),buf.readFloat(),buf.readFloat(),buf.readVarInt(),buf.readVarInt(),buf.readVarInt(),buf.readBoolean());
    });
    @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    public static SyncBodyStatePacket from(BodyState b,int thermalStage){return from(b,thermalStage,DiseaseModel.State.HEALTHY);}
    public static SyncBodyStatePacket from(BodyState b,int thermalStage,DiseaseModel.State illness){
        int n=BodyPart.values().length;
        float[] h=new float[n];byte[] bleed=new byte[n],wounds=new byte[n];boolean[] f=new boolean[n],s=new boolean[n];
        for(BodyPart p:BodyPart.values()){int i=p.ordinal();h[i]=b.health(p);bleed[i]=(byte)b.bleeding(p);wounds[i]=(byte)b.wounds(p);f[i]=b.fractured(p);s[i]=b.stabilized(p);}
        return new SyncBodyStatePacket(h,bleed,wounds,f,s,b.concussionTicks(),b.painkillerTicks(),thermalStage,b.bloodMl(),b.pain(),b.shockStage(),illness.symptomatic()?illness.kind().ordinal():0,illness.symptomatic()?illness.stage().ordinal():0,illness.symptomatic()&&illness.support()>0);
    }
}
