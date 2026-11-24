package dev.minemods.sleepdebt.network;
import dev.minemods.sleepdebt.SleepDebtMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
public record SleepStatusPayload(int debt,int maskTicks,int crashTicks,int caffeineLoad,int fatigue,int stress,int[] penalties) implements CustomPacketPayload{
 public static final Type<SleepStatusPayload> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath(SleepDebtMod.MOD_ID,"sleep_status"));
 public static final StreamCodec<ByteBuf,SleepStatusPayload> STREAM_CODEC=StreamCodec.of(
  (b,p)->{
   for(int v:new int[]{p.debt,p.maskTicks,p.crashTicks,p.caffeineLoad,p.fatigue,p.stress})ByteBufCodecs.VAR_INT.encode(b,v);
   for(int i=0;i<5;i++)ByteBufCodecs.VAR_INT.encode(b,i<p.penalties.length?p.penalties[i]:0);
  },b->{
   int[] a=new int[6],v=new int[5];
   for(int i=0;i<a.length;i++)a[i]=ByteBufCodecs.VAR_INT.decode(b);
   for(int i=0;i<v.length;i++)v[i]=ByteBufCodecs.VAR_INT.decode(b);
   return new SleepStatusPayload(a[0],a[1],a[2],a[3],a[4],a[5],v);
  });
 @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
