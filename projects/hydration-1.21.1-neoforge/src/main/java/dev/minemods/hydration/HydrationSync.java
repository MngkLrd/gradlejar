package dev.minemods.hydration;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import java.util.WeakHashMap;

public final class HydrationSync {
 public record State(int units) implements CustomPacketPayload {
  static final Type<State> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("hydration","state"));
  public Type<State> type(){return TYPE;}
 }
 public record Sip() implements CustomPacketPayload {
  static final Type<Sip> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("hydration","sip"));
  public Type<Sip> type(){return TYPE;}
 }
 public record Fill() implements CustomPacketPayload {
  static final Type<Fill> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("hydration","fill"));
  public Type<Fill> type(){return TYPE;}
 }
 private static final WeakHashMap<ServerPlayer,Long> LAST_FILL=new WeakHashMap<>();
 private static final WeakHashMap<ServerPlayer,Integer> SENT=new WeakHashMap<>();
 public static volatile int clientUnits=-1;
 public static void register(RegisterPayloadHandlersEvent e){
  var r=e.registrar("2");
  r.playToClient(State.TYPE,StreamCodec.<RegistryFriendlyByteBuf,State>of((b,s)->b.writeVarInt(s.units()),b->new State(b.readVarInt())),(s,c)->c.enqueueWork(()->clientUnits=s.units()));
  r.playToServer(Sip.TYPE,StreamCodec.<RegistryFriendlyByteBuf,Sip>of((b,s)->{},b->new Sip()),(s,c)->c.enqueueWork(()->{if(c.player() instanceof ServerPlayer p)Hydration.sip(p);}));
  r.playToServer(Fill.TYPE,StreamCodec.<RegistryFriendlyByteBuf,Fill>of((b,s)->{},b->new Fill()),(s,c)->c.enqueueWork(()->{if(c.player() instanceof ServerPlayer p&&Hydration.active(p)){
   long now=p.serverLevel().getGameTime(),last=LAST_FILL.getOrDefault(p,now-20);
   if(now>=last&&now-last<10)return;LAST_FILL.put(p,now);
   if(p.getMainHandItem().getItem() instanceof VesselItem v)v.fillFromSource(p,p.getMainHandItem());
  }}));
 }
 public static void send(ServerPlayer p){
  if(p instanceof FakePlayer)return;
  int units=Hydration.active(p)?WaterBalance.halfUnits(Hydration.state(p).water()):-1;
  if(!Integer.valueOf(units).equals(SENT.get(p))){PacketDistributor.sendToPlayer(p,new State(units));SENT.put(p,units);}
 }
 public static void forget(ServerPlayer p){SENT.remove(p);LAST_FILL.remove(p);}
}
