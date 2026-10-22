package dev.minemods.sleepdebt.network;
import dev.minemods.sleepdebt.client.ClientState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
public final class SleepNetworking {
 public static void register(RegisterPayloadHandlersEvent e){
  var registrar=e.registrar("3");
  if(FMLEnvironment.dist==Dist.CLIENT){
   registrar.playToClient(SleepStatusPayload.TYPE,SleepStatusPayload.STREAM_CODEC,
    (payload,ctx)->ClientState.update(payload));
  }else{
   registrar.playToClient(SleepStatusPayload.TYPE,SleepStatusPayload.STREAM_CODEC,
    (payload,ctx)->{});
  }
 }
 private SleepNetworking(){}
}
