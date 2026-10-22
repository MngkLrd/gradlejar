package dev.minemods.sleepdebt.network;
import dev.minemods.sleepdebt.SleepDebtMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
public record SleepStatusPayload(int debt,int maskTicks,int crashTicks,int caffeineLoad) implements CustomPacketPayload {
 public static final Type<SleepStatusPayload> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath(SleepDebtMod.MOD_ID,"sleep_status"));
 public static final StreamCodec<ByteBuf,SleepStatusPayload> STREAM_CODEC=StreamCodec.composite(
  ByteBufCodecs.VAR_INT,SleepStatusPayload::debt,
  ByteBufCodecs.VAR_INT,SleepStatusPayload::maskTicks,
  ByteBufCodecs.VAR_INT,SleepStatusPayload::crashTicks,
  ByteBufCodecs.VAR_INT,SleepStatusPayload::caffeineLoad,
  SleepStatusPayload::new);
 @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
