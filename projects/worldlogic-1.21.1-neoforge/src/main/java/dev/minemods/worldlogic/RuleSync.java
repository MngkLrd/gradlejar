package dev.minemods.worldlogic;
import java.util.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
public final class RuleSync {
 record State(boolean friction,boolean momentum,double weather,int streak,String family,String tool,double boatLimit,boolean down,float roll,boolean crawl,boolean hanging,float armFactor,boolean cramped,int fatiguePercent) implements CustomPacketPayload {
  static final Type<State> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("worldlogic","rules"));
  @Override public Type<State> type(){return TYPE;}
 }
 private static volatile State client;
 private static volatile long received;
 private static final Map<ServerPlayer,State> SENT=new WeakHashMap<>();
 static final StreamCodec<RegistryFriendlyByteBuf,State> CODEC=StreamCodec.of((b,m)->{b.writeBoolean(m.friction);b.writeBoolean(m.momentum);b.writeDouble(m.weather);b.writeVarInt(m.streak);b.writeUtf(m.family,256);b.writeUtf(m.tool,256);b.writeDouble(m.boatLimit);b.writeBoolean(m.down);b.writeFloat(m.roll);b.writeBoolean(m.crawl);b.writeBoolean(m.hanging);b.writeFloat(m.armFactor);b.writeBoolean(m.cramped);b.writeVarInt(m.fatiguePercent);},
  b->new State(b.readBoolean(),b.readBoolean(),b.readDouble(),b.readVarInt(),b.readUtf(256),b.readUtf(256),b.readDouble(),b.readBoolean(),b.readFloat(),b.readBoolean(),b.readBoolean(),b.readFloat(),b.readBoolean(),b.readVarInt()));
 public static void init(RegisterPayloadHandlersEvent e){
  var registrar=e.registrar("11");
  registrar.playToClient(State.TYPE,CODEC,(m,c)->{client=m;received=System.nanoTime();});
  registrar.playToClient(LocalWeatherSync.State.TYPE,LocalWeatherSync.CODEC,(m,c)->LocalWeatherSync.receive(m));
  registrar.playToClient(AnalyticsSync.State.TYPE,AnalyticsSync.CODEC,(m,c)->AnalyticsSync.receive(m));
  registrar.playToClient(DashboardSync.State.TYPE,DashboardSync.CODEC,(m,c)->DashboardSync.receive(m));
 }
 private static State get(ServerPlayer p){
  var b=Body.state(p);int streak=p.level().getGameTime()-b.lastBreak<=40?b.streak:0;
  return new State(Rules.on("slippery_rain"),Rules.on("tool_momentum"),Rules.strength("weather"),streak,b.lastFamily==null?"":b.lastFamily,b.lastTool==null?"":BuiltInRegistries.ITEM.getKey(b.lastTool).toString(),TravelPhysics.boatLimit(p),Balance.down(p),Balance.down(p)?Balance.roll(p):0,Balance.canCrawl(p),ImpactDynamics.hanging(p),(float)Trauma.mining(p),Rules.on("cramped_mining"),fatiguePercent(p));
 }
 private static int fatiguePercent(ServerPlayer p){return !Rules.on("exertion")||p.isCreative()||p.isSpectator()?-1:(int)Math.round(Survival.fatigue(p));}
 public static java.util.OptionalInt clientFatiguePercent(){State s=client;return s==null||s.fatiguePercent<0?java.util.OptionalInt.empty():java.util.OptionalInt.of(s.fatiguePercent);}
 public static void send(ServerPlayer p){State s=get(p);if(!s.equals(SENT.get(p))){if(p instanceof net.neoforged.neoforge.common.util.FakePlayer)return;PacketDistributor.sendToPlayer(p,s);SENT.put(p,s);}}
 public static void login(PlayerEvent.PlayerLoggedInEvent e){if(e.getEntity() instanceof ServerPlayer p){SENT.remove(p);send(p);}}
 public static void tick(PlayerTickEvent.Post e){if(e.getEntity() instanceof ServerPlayer p&&p.tickCount%10==0)send(p);}
 public static void logout(PlayerEvent.PlayerLoggedOutEvent e){if(e.getEntity() instanceof ServerPlayer p)SENT.remove(p);}
 public static void clearClient(){client=null;received=0;LocalWeatherSync.clear();AnalyticsSync.clear();DashboardSync.clear();}
 public static boolean friction(boolean clientSide){State s=client;return clientSide&&s!=null?s.friction:Rules.on("slippery_rain");}
 public static double weather(boolean clientSide){State s=client;return clientSide&&s!=null?s.weather:Rules.strength("weather");}
 public static boolean clientCramped(){State s=client;return s!=null&&s.cramped;}
 public static boolean clientDown(){State s=client;return s!=null&&s.down;}
 public static boolean clientCrawl(){State s=client;return s!=null&&s.crawl;}
 public static boolean clientHanging(){State s=client;return s!=null&&s.hanging;}
 public static float clientRoll(){State s=client;return s==null?0:s.roll;}
 public static double boatLimit(){State s=client;return s==null?0:s.boatLimit;}
 public static float mining(Player p,BlockState block){State s=client;if(s==null)return 1;if(!s.momentum)return s.armFactor;
  return s.armFactor*(Material.family(block).equals(s.family)&&BuiltInRegistries.ITEM.getKey(p.getMainHandItem().getItem()).toString().equals(s.tool)?1+.03f*Math.max(0,Math.min(4,s.streak)):1);
 }
}
