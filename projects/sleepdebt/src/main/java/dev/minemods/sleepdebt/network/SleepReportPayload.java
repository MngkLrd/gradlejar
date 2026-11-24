package dev.minemods.sleepdebt.network;
import dev.minemods.sleepdebt.SleepDebtMod;
import dev.minemods.sleepdebt.gameplay.SleepEngine;
import dev.minemods.sleepdebt.gameplay.RestEnvironment;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
public record SleepReportPayload(int debt,int fatigue,int stress,int quality,int recovered,
 int reasons,int forecast,int forecastReasons,int caffeine,int tolerance,int maskTicks,
 int crashTicks,int maskPower,int days,int[] history,int[] activity,int[] penalties) implements CustomPacketPayload {
 public static final Type<SleepReportPayload> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath(SleepDebtMod.MOD_ID,"sleep_report"));
 private static void writeArray(ByteBuf b,int[] a,int max){
  int n=Math.min(max,a.length);ByteBufCodecs.VAR_INT.encode(b,n);
  for(int i=0;i<n;i++)ByteBufCodecs.VAR_INT.encode(b,a[i]);
 }
 private static int[] readArray(ByteBuf b,int max){
  int n=ByteBufCodecs.VAR_INT.decode(b);
  if(n<0||n>max)throw new IllegalArgumentException("Sleep report array size");
  int[] a=new int[n];for(int i=0;i<n;i++)a[i]=ByteBufCodecs.VAR_INT.decode(b);return a;
 }
 public static final StreamCodec<ByteBuf,SleepReportPayload> STREAM_CODEC=StreamCodec.of(
  (b,p)->{
   for(int v:new int[]{p.debt,p.fatigue,p.stress,p.quality,p.recovered,p.reasons,p.forecast,p.forecastReasons,
     p.caffeine,p.tolerance,p.maskTicks,p.crashTicks,p.maskPower,p.days})ByteBufCodecs.VAR_INT.encode(b,v);
   writeArray(b,p.history,240);writeArray(b,p.activity,6);writeArray(b,p.penalties,5);
  },b->{
   int[] a=new int[14];for(int i=0;i<a.length;i++)a[i]=ByteBufCodecs.VAR_INT.decode(b);
   return new SleepReportPayload(a[0],a[1],a[2],a[3],a[4],a[5],a[6],a[7],a[8],a[9],a[10],a[11],a[12],a[13],
    readArray(b,240),readArray(b,6),readArray(b,5));
  });
 public static void send(ServerPlayer p){
  var q=RestEnvironment.evaluate(p,p.blockPosition());
  PacketDistributor.sendToPlayer(p,new SleepReportPayload(SleepEngine.getDebt(p),SleepEngine.getFatigue(p),
   SleepEngine.getStress(p),SleepEngine.getLastQuality(p),SleepEngine.getLastRecovery(p),SleepEngine.getLastReasons(p),
   q.quality(),q.reasons(),SleepEngine.getCaffeine(p),SleepEngine.getTolerance(p),SleepEngine.getMaskTicks(p),
   SleepEngine.getCrashTicks(p),SleepEngine.getMaskPower(p),SleepEngine.daysSinceSleep(p),
   SleepEngine.history(p),SleepEngine.activity(p),SleepEngine.penalties(p)));
 }
 @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
