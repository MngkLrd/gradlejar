package dev.minemods.sleepdebt.network;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
public record JournalRequest() implements CustomPacketPayload {
 public static final Type<JournalRequest> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("sleepdebt","journal_request"));
 public static final StreamCodec<ByteBuf,JournalRequest> STREAM_CODEC=StreamCodec.unit(new JournalRequest());
 @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
 public static void handle(ServerPlayer p){
  var t=p.getPersistentData();long now=p.serverLevel().getGameTime();
  if(t.contains("sleepdebt_journal_request")&&now>=t.getLong("sleepdebt_journal_request")&&now-t.getLong("sleepdebt_journal_request")<10)return;
  t.putLong("sleepdebt_journal_request",now);SleepReportPayload.send(p);
 }
}
