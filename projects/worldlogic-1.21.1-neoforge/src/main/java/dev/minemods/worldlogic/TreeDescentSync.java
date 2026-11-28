package dev.minemods.worldlogic;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.network.PacketDistributor;
final public class TreeDescentSync{
 private TreeDescentSync(){}
 record State(UUID player,boolean gripping) implements CustomPacketPayload{
  static final Type<State> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("worldlogic","tree_grip"));
  @Override public Type<State> type(){return TYPE;}
 }
 static final StreamCodec<RegistryFriendlyByteBuf,State> CODEC=StreamCodec.of((b,s)->{b.writeUUID(s.player);b.writeBoolean(s.gripping);},b->new State(b.readUUID(),b.readBoolean()));
 private static final ConcurrentHashMap<UUID,Long> CLIENT=new ConcurrentHashMap<>();
 static void receive(State s){
  long now=System.nanoTime();CLIENT.entrySet().removeIf(e->now-e.getValue()>1_500_000_000L);
  if(s.gripping)CLIENT.put(s.player,now);else CLIENT.remove(s.player);
 }
 static void send(Player p,boolean gripping){
  if(p instanceof ServerPlayer server&&!(server instanceof FakePlayer))PacketDistributor.sendToPlayersTrackingEntityAndSelf(server,new State(p.getUUID(),gripping));
 }
 public static boolean clientGripping(Player p){
  var last=CLIENT.get(p.getUUID());return last!=null&&System.nanoTime()-last<1_500_000_000L;
 }
 static void clear(){CLIENT.clear();}
}
