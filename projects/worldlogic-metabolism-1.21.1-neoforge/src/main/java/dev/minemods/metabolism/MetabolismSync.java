package dev.minemods.metabolism;
import dev.minemods.worldlogic.api.WorldLogicApi;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import java.util.UUID;
public final class MetabolismSync {
 public static final int SIZE=2*MetabolicModel.N+18+33;
 public record State(UUID player,boolean active,boolean anatomical,float[] values) implements CustomPacketPayload{
  static final Type<State> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("metabolism","state"));
  public State{values=values.clone();if(values.length!=SIZE)throw new IllegalArgumentException("size");}
  @Override public float[] values(){return values.clone();}
  public Type<State> type(){return TYPE;}
 }
 public static volatile State client;
 public static void register(RegisterPayloadHandlersEvent e){
  e.registrar("4").playToClient(State.TYPE,StreamCodec.<RegistryFriendlyByteBuf,State>of((b,s)->{
   b.writeUUID(s.player);b.writeBoolean(s.active);b.writeBoolean(s.anatomical);for(float v:s.values)b.writeFloat(v);
  },b->{UUID id=b.readUUID();boolean active=b.readBoolean(),anatomical=b.readBoolean();float[] v=new float[SIZE];for(int i=0;i<SIZE;i++)v[i]=b.readFloat();return new State(id,active,anatomical,v);}),
  (s,c)->c.enqueueWork(()->client=s));
 }
 public static void send(ServerPlayer p){
  if(p instanceof FakePlayer)return;
  var s=Metabolism.state(p);EnvironmentBridge.sample(p,s);var h=MedicalBridge.read(p);var f=WorldLogicApi.fatigue(p);
  float[] v=new float[SIZE];for(int i=0;i<MetabolicModel.N;i++)v[i]=(float)s.adequacy(i);
  double[] extra={s.fast,s.reserve,s.stomach,s.toxin,s.quality(),s.energy(),s.recovery(),s.resistance(),s.absorptionEfficiency(),s.burn,h.current(),h.maximum(),f.enabled()?f.fatiguePercent():-1,s.deficient(),s.critical(),s.fastHours(),s.totalHours(),s.digestiveHours()};
  for(int i=0;i<extra.length;i++)v[MetabolicModel.N+i]=(float)extra[i];
  for(int i=0;i<MetabolicModel.N;i++)v[MetabolicModel.N+18+i]=(float)s.trend(i);
  int k=2*MetabolicModel.N+18;v[k++]=(float)s.water;v[k++]=(float)s.salts;v[k++]=(float)s.bodyC;v[k++]=(float)s.thermalCost();v[k++]=(float)s.hydrationFactor();
  v[k++]=(float)s.tissueFactor();v[k++]=(float)s.boneFactor();v[k++]=(float)s.bloodFactor();
  v[k++]=(float)s.dayIntake;v[k++]=(float)s.dayBurn;v[k++]=(float)s.daySeconds;
  for(int i=0;i<7;i++){v[k++]=i<s.historyCount?(float)s.historyIntake[i]:-1;v[k++]=i<s.historyCount?(float)s.historyBurn[i]:-1;v[k++]=i<s.historyCount?(float)s.historyQuality[i]:-1;}
  v[k]=(float)MedicalBridge.passiveRecovery(p);
  PacketDistributor.sendToPlayer(p,new State(p.getUUID(),Metabolism.active(p),h.anatomical(),v));
 }
 private MetabolismSync(){}
}
