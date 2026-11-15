package dev.minemods.sleepdebt.network;
import dev.minemods.sleepdebt.client.ClientState;
import dev.minemods.sleepdebt.client.SleepJournalScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
public final class SleepNetworking {
 public static void register(RegisterPayloadHandlersEvent e){
  var registrar=e.registrar("4");
  if(FMLEnvironment.dist==Dist.CLIENT){
   registrar.playToClient(SleepStatusPayload.TYPE,SleepStatusPayload.STREAM_CODEC,
    (payload,ctx)->ClientState.update(payload));
   registrar.playToClient(SleepReportPayload.TYPE,SleepReportPayload.STREAM_CODEC,
    (payload,ctx)->SleepJournalScreen.open(payload));
  }else{
   registrar.playToClient(SleepStatusPayload.TYPE,SleepStatusPayload.STREAM_CODEC,
    (payload,ctx)->{});
   registrar.playToClient(SleepReportPayload.TYPE,SleepReportPayload.STREAM_CODEC,
    (payload,ctx)->{});
  }
 }
 private SleepNetworking(){}
}
