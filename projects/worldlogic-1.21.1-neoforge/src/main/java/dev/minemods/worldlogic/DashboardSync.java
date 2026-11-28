package dev.minemods.worldlogic;
import dev.minemods.worldlogic.api.WorldLogicDashboards.*;
import java.util.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
public final class DashboardSync {
 private DashboardSync(){}
 public record State(String dimension,long tick,List<Snapshot> dashboards)implements CustomPacketPayload{
  public static final Type<State> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("worldlogic","dashboards"));
  public State{dashboards=List.copyOf(dashboards);}
  @Override public Type<State> type(){return TYPE;}
 }
 private static int size(RegistryFriendlyByteBuf b,int max){int n=b.readVarInt();if(n<0||n>max)throw new IllegalArgumentException("dashboard packet size");return n;}
 public static final StreamCodec<RegistryFriendlyByteBuf,State> CODEC=StreamCodec.of((b,s)->{
  b.writeUtf(s.dimension(),256);b.writeLong(s.tick());b.writeVarInt(s.dashboards().size());
  for(var d:s.dashboards()){
   b.writeResourceLocation(d.id());b.writeInt(d.order());b.writeUtf(d.title(),128);b.writeResourceLocation(d.icon());b.writeVarInt(d.sections().size());
   for(var section:d.sections()){
    b.writeUtf(section.title(),128);b.writeVarInt(section.rows().size());
    for(var r:section.rows()){b.writeUtf(r.label(),128);b.writeUtf(r.value(),128);b.writeDouble(r.fraction());}
   }
  }
 },b->{
  String dimension=b.readUtf(256);long tick=b.readLong();int n=size(b,12);var dashboards=new ArrayList<Snapshot>();
  for(int i=0;i<n;i++){
   var id=b.readResourceLocation();int order=b.readInt();String title=b.readUtf(128);var icon=b.readResourceLocation();int sn=size(b,16);var sections=new ArrayList<Section>();
   for(int a=0;a<sn;a++){String st=b.readUtf(128);int rn=size(b,128);var rows=new ArrayList<Row>();for(int r=0;r<rn;r++)rows.add(new Row(b.readUtf(128),b.readUtf(128),b.readDouble()));sections.add(new Section(st,rows));}
   dashboards.add(new Snapshot(id,order,title,icon,sections));
  }
  return new State(dimension,tick,dashboards);
 });
 private static volatile State client;
 static void receive(State s){client=s;}
 public static State client(){return client;}
 static void clear(){client=null;}
 public static void tick(PlayerTickEvent.Post e){
  if(e.getEntity() instanceof ServerPlayer p&&!(p instanceof net.neoforged.neoforge.common.util.FakePlayer)&&p.tickCount%40==0)
   PacketDistributor.sendToPlayer(p,new State(p.level().dimension().location().toString(),p.level().getGameTime(),PeerDashboardSnapshots.collect(p)));
 }
}
