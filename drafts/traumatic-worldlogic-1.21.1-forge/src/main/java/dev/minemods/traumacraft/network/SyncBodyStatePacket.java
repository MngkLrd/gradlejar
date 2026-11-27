package dev.minemods.traumacraft.network;
import dev.minemods.traumacraft.health.*;
import net.minecraft.network.FriendlyByteBuf;
public record SyncBodyStatePacket(float[] hp,byte[] bleeding,boolean[] fracture,boolean[] stabilized,int concussion,int painkiller) {
    public static SyncBodyStatePacket from(BodyState b){
        int n=BodyPart.values().length;
        float[] h=new float[n];byte[] bleed=new byte[n];boolean[] f=new boolean[n],s=new boolean[n];
        for(BodyPart p:BodyPart.values()){int i=p.ordinal();h[i]=b.health(p);bleed[i]=(byte)b.bleeding(p);f[i]=b.fractured(p);s[i]=b.stabilized(p);}
        return new SyncBodyStatePacket(h,bleed,f,s,b.concussionTicks(),b.painkillerTicks());
    }
    public static void encode(SyncBodyStatePacket m,FriendlyByteBuf b) {
        for(int i=0;i<BodyPart.values().length;i++){b.writeFloat(m.hp[i]);b.writeByte(m.bleeding[i]);b.writeBoolean(m.fracture[i]);b.writeBoolean(m.stabilized[i]);}
        b.writeVarInt(m.concussion);b.writeVarInt(m.painkiller);
    }
    public static SyncBodyStatePacket decode(FriendlyByteBuf b){
        int n=BodyPart.values().length;
        float[] h=new float[n];byte[] bleed=new byte[n];boolean[] f=new boolean[n],s=new boolean[n];
        for(int i=0;i<n;i++){h[i]=b.readFloat();bleed[i]=b.readByte();f[i]=b.readBoolean();s[i]=b.readBoolean();}
        return new SyncBodyStatePacket(h,bleed,f,s,b.readVarInt(),b.readVarInt());
    }
}
