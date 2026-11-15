package dev.minemods.sleepdebt.network;

import dev.minemods.sleepdebt.SleepDebtMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Server snapshot for the native journal screen, sent only on item use. */
public record SleepReportPayload(int debt,int fatigue,int stress,int quality,int recovered,int reasons)
 implements CustomPacketPayload {
 public static final Type<SleepReportPayload> TYPE=new Type<>(
  ResourceLocation.fromNamespaceAndPath(SleepDebtMod.MOD_ID,"sleep_report"));
 public static final StreamCodec<ByteBuf,SleepReportPayload> STREAM_CODEC=StreamCodec.composite(
  ByteBufCodecs.VAR_INT,SleepReportPayload::debt,
  ByteBufCodecs.VAR_INT,SleepReportPayload::fatigue,
  ByteBufCodecs.VAR_INT,SleepReportPayload::stress,
  ByteBufCodecs.VAR_INT,SleepReportPayload::quality,
  ByteBufCodecs.VAR_INT,SleepReportPayload::recovered,
  ByteBufCodecs.VAR_INT,SleepReportPayload::reasons,
  SleepReportPayload::new);
 @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
