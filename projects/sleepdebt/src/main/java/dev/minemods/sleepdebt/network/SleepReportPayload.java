package dev.minemods.sleepdebt.network;

import dev.minemods.sleepdebt.SleepDebtMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** One on-demand snapshot: previous night AND a forecast for the current location. */
public record SleepReportPayload(int debt,int fatigue,int stress,
 int quality,int recovered,int reasons,int forecast,int forecastReasons)
 implements CustomPacketPayload {
 public static final Type<SleepReportPayload> TYPE=new Type<>(
  ResourceLocation.fromNamespaceAndPath(SleepDebtMod.MOD_ID,"sleep_report"));
 public static final StreamCodec<ByteBuf,SleepReportPayload> STREAM_CODEC=StreamCodec.of(
  (buf,p)->{
   ByteBufCodecs.VAR_INT.encode(buf,p.debt());
   ByteBufCodecs.VAR_INT.encode(buf,p.fatigue());
   ByteBufCodecs.VAR_INT.encode(buf,p.stress());
   ByteBufCodecs.VAR_INT.encode(buf,p.quality());
   ByteBufCodecs.VAR_INT.encode(buf,p.recovered());
   ByteBufCodecs.VAR_INT.encode(buf,p.reasons());
   ByteBufCodecs.VAR_INT.encode(buf,p.forecast());
   ByteBufCodecs.VAR_INT.encode(buf,p.forecastReasons());
  },
  buf->new SleepReportPayload(
   ByteBufCodecs.VAR_INT.decode(buf),ByteBufCodecs.VAR_INT.decode(buf),
   ByteBufCodecs.VAR_INT.decode(buf),ByteBufCodecs.VAR_INT.decode(buf),
   ByteBufCodecs.VAR_INT.decode(buf),ByteBufCodecs.VAR_INT.decode(buf),
   ByteBufCodecs.VAR_INT.decode(buf),ByteBufCodecs.VAR_INT.decode(buf))
 );
 @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
