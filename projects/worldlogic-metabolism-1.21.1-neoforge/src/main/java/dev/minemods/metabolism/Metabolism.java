package dev.minemods.metabolism;
import dev.minemods.worldlogic.api.WorldLogicApi;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import java.util.WeakHashMap;
@Mod("metabolism")
public final class Metabolism {
 private static final String KEY="worldlogic_metabolism";
 private static final WeakHashMap<ServerPlayer,Vec3> POS=new WeakHashMap<>();
 public Metabolism(IEventBus modBus){
  modBus.addListener(MetabolismSync::register);
  var bus=NeoForge.EVENT_BUS;
  bus.addListener(Metabolism::tick);bus.addListener(Metabolism::finish);bus.addListener(Metabolism::clonePlayer);
  bus.addListener(Metabolism::login);bus.addListener(Metabolism::logout);bus.addListener(Metabolism::commands);
 }
 static boolean active(ServerPlayer p){return p.isAlive()&&!p.isCreative()&&!p.isSpectator()&&!(p instanceof FakePlayer);}
 public static MetabolicModel.State state(ServerPlayer p){
  var s=new MetabolicModel.State();var root=p.getPersistentData();if(!root.contains(KEY))return s;return decode(root.getCompound(KEY));
 }
 static CompoundTag encode(MetabolicModel.State s){
  var n=new CompoundTag();n.putInt("schema",1);n.putDouble("fast",s.fast);n.putDouble("reserve",s.reserve);n.putDouble("stomach",s.stomach);
  n.putDouble("queued",s.queuedEnergy);n.putDouble("toxin",s.toxin);n.putDouble("absorption",s.absorption);
  n.putDouble("burn",s.burn);n.putDouble("activity",s.activity);n.putDouble("unknown",s.unknown);
  for(int i=0;i<MetabolicModel.N;i++){n.putDouble("s_"+i,s.stores[i]);n.putDouble("p_"+i,s.pending[i]);}return n;
 }
 static MetabolicModel.State decode(CompoundTag n){
  var s=new MetabolicModel.State();if(n.getInt("schema")!=1)return s;
  s.fast=MetabolicModel.clamp(n.getDouble("fast"),0,MetabolicModel.FAST_MAX);s.reserve=MetabolicModel.clamp(n.getDouble("reserve"),0,MetabolicModel.FAT_MAX);
  s.stomach=MetabolicModel.clamp(n.getDouble("stomach"),0,MetabolicModel.STOMACH_MAX);s.queuedEnergy=MetabolicModel.clamp(n.getDouble("queued"),0,s.stomach);
  s.toxin=MetabolicModel.clamp(n.getDouble("toxin"),0,1);s.absorption=MetabolicModel.clamp(n.getDouble("absorption"),.15,1);s.burn=MetabolicModel.clamp(n.getDouble("burn"),90,320);
  s.activity=MetabolicModel.clamp(n.getDouble("activity"),0,1);s.unknown=MetabolicModel.clamp(n.getDouble("unknown"),0,999);
  for(int i=0;i<MetabolicModel.N;i++){s.stores[i]=MetabolicModel.clamp(n.getDouble("s_"+i),0,1.5);s.pending[i]=MetabolicModel.clamp(n.getDouble("p_"+i),0,8);}return s;
 }
 static void store(ServerPlayer p,MetabolicModel.State s){p.getPersistentData().put(KEY,encode(s));}
 private static void tick(PlayerTickEvent.Post e){
  if(!(e.getEntity() instanceof ServerPlayer p)||p.tickCount%20!=0)return;
  if(!active(p)){POS.remove(p);if(p.tickCount%40==0)MetabolismSync.send(p);return;}
  Vec3 old=POS.put(p,p.position());double distance=old==null?0:Math.sqrt(p.position().distanceToSqr(old));
  double intensity=distance>20?0:Math.min(1,distance/5.5);if(distance>.2&&p.isSprinting())intensity=Math.max(intensity,.8);
  if(distance>.2&&p.isSwimming())intensity=Math.max(intensity,.65);
  var s=state(p);MetabolicModel.tick(s,intensity);store(p,s);
  if(s.energy()<.15&&intensity>.1&&p.tickCount%100==0)WorldLogicApi.addExertion(p,(.15-s.energy())*3);
  if(p.tickCount%40==0)MetabolismSync.send(p);
 }
 private static void finish(LivingEntityUseItemEvent.Finish e){
  if(!(e.getEntity() instanceof ServerPlayer p)||!active(p))return;
  FoodProfiles.profile(e.getItem()).ifPresent(f->{var s=state(p);MetabolicModel.ingest(s,f.meal(),f.known());store(p,s);MetabolismSync.send(p);});
 }
 private static void clonePlayer(PlayerEvent.Clone e){if(e.getEntity() instanceof ServerPlayer p&&e.getOriginal() instanceof ServerPlayer old)store(p,e.isWasDeath()?new MetabolicModel.State():state(old));}
 private static void login(PlayerEvent.PlayerLoggedInEvent e){if(e.getEntity() instanceof ServerPlayer p)MetabolismSync.send(p);}
 private static void logout(PlayerEvent.PlayerLoggedOutEvent e){if(e.getEntity() instanceof ServerPlayer p)POS.remove(p);}
 private static void commands(RegisterCommandsEvent e){
  e.getDispatcher().register(Commands.literal("metabolism").then(Commands.literal("selftest").requires(s->s.hasPermission(2)).executes(c->{ModelChecks.run();c.getSource().sendSuccess(()->Component.literal("METABOLISM_MODEL_CHECKS_PASSED"),false);return 1;})));
 }
}
