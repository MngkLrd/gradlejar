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
import java.util.Locale;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.ChatFormatting;
@Mod("metabolism")
public final class Metabolism {
 private static final String KEY="worldlogic_metabolism";
 private static final WeakHashMap<ServerPlayer,Vec3> POS=new WeakHashMap<>();
 public Metabolism(IEventBus modBus){
  modBus.addListener(MetabolismSync::register);
  var bus=NeoForge.EVENT_BUS;
  bus.addListener(Metabolism::tick);bus.addListener(Metabolism::finish);bus.addListener(Metabolism::clonePlayer);
  bus.addListener(Metabolism::login);bus.addListener(Metabolism::logout);bus.addListener(Metabolism::commands);bus.addListener(Metabolism::foodTooltip);
 }
 static boolean active(ServerPlayer p){return p.isAlive()&&!p.isCreative()&&!p.isSpectator()&&!(p instanceof FakePlayer);}
 public static MetabolicModel.State state(ServerPlayer p){
  var s=new MetabolicModel.State();var root=p.getPersistentData();if(!root.contains(KEY))return s;return decode(root.getCompound(KEY));
 }
 static CompoundTag encode(MetabolicModel.State s){
  var n=new CompoundTag();n.putInt("schema",3);n.putDouble("fast",s.fast);n.putDouble("reserve",s.reserve);n.putDouble("stomach",s.stomach);
  n.putDouble("queued",s.queuedEnergy);n.putDouble("toxin",s.toxin);n.putDouble("absorption",s.absorption);
  n.putDouble("burn",s.burn);n.putDouble("activity",s.activity);n.putDouble("unknown",s.unknown);n.putDouble("observed",s.observedSeconds);n.putInt("daySeconds",s.daySeconds);n.putDouble("dayIntake",s.dayIntake);n.putDouble("dayBurn",s.dayBurn);n.putDouble("dayQuality",s.dayQuality);n.putInt("historyCount",s.historyCount);for(int i=0;i<MetabolicModel.State.HISTORY;i++){n.putDouble("hi_"+i,s.historyIntake[i]);n.putDouble("hb_"+i,s.historyBurn[i]);n.putDouble("hq_"+i,s.historyQuality[i]);}
  for(int i=0;i<MetabolicModel.N;i++){n.putDouble("s_"+i,s.stores[i]);n.putDouble("p_"+i,s.pending[i]);n.putDouble("b_"+i,s.baseline[i]);}return n;
 }
 static MetabolicModel.State decode(CompoundTag n){
  var s=new MetabolicModel.State();int schema=n.getInt("schema");if(schema<1||schema>3)return s;
  s.fast=MetabolicModel.clamp(n.getDouble("fast"),0,MetabolicModel.FAST_MAX);s.reserve=MetabolicModel.clamp(n.getDouble("reserve"),0,MetabolicModel.FAT_MAX);
  s.stomach=MetabolicModel.clamp(n.getDouble("stomach"),0,MetabolicModel.STOMACH_MAX);s.queuedEnergy=MetabolicModel.clamp(n.getDouble("queued"),0,s.stomach);
  s.toxin=MetabolicModel.clamp(n.getDouble("toxin"),0,1);s.absorption=MetabolicModel.clamp(n.getDouble("absorption"),.15,1);s.burn=MetabolicModel.clamp(n.getDouble("burn"),90,500);
  s.activity=MetabolicModel.clamp(n.getDouble("activity"),0,1);s.unknown=MetabolicModel.clamp(n.getDouble("unknown"),0,999);s.observedSeconds=schema>=2?MetabolicModel.clamp(n.getDouble("observed"),0,MetabolicModel.GAME_DAY_SECONDS):0;
  for(int i=0;i<MetabolicModel.N;i++){s.stores[i]=MetabolicModel.clamp(n.getDouble("s_"+i),0,1.5);s.pending[i]=MetabolicModel.clamp(n.getDouble("p_"+i),0,8);s.baseline[i]=schema>=2&&n.contains("b_"+i)?MetabolicModel.clamp(n.getDouble("b_"+i),0,1.5):s.stores[i];}if(schema>=3){s.daySeconds=(int)MetabolicModel.clamp(n.getInt("daySeconds"),0,1199);s.dayIntake=MetabolicModel.clamp(n.getDouble("dayIntake"),0,10000000);s.dayBurn=MetabolicModel.clamp(n.getDouble("dayBurn"),0,100000);s.dayQuality=MetabolicModel.clamp(n.getDouble("dayQuality"),0,s.daySeconds);s.historyCount=(int)MetabolicModel.clamp(n.getInt("historyCount"),0,7);for(int i=0;i<7;i++){s.historyIntake[i]=MetabolicModel.clamp(n.getDouble("hi_"+i),0,10000000);s.historyBurn[i]=MetabolicModel.clamp(n.getDouble("hb_"+i),0,100000);s.historyQuality[i]=MetabolicModel.clamp(n.getDouble("hq_"+i),0,1);}}return s;
 }
 static void store(ServerPlayer p,MetabolicModel.State s){p.getPersistentData().put(KEY,encode(s));}
 private static void tick(PlayerTickEvent.Post e){
  if(!(e.getEntity() instanceof ServerPlayer p)||p.tickCount%20!=0)return;
  if(!active(p)){POS.remove(p);if(p.tickCount%40==0)MetabolismSync.send(p);return;}
  Vec3 old=POS.put(p,p.position());double distance=old==null?0:Math.sqrt(p.position().distanceToSqr(old));
  double intensity=distance>20?0:Math.min(1,distance/5.5);if(distance>.2&&p.isSprinting())intensity=Math.max(intensity,.8);
  if(distance>.2&&p.isSwimming())intensity=Math.max(intensity,.65);
  var s=state(p);EnvironmentBridge.sample(p,s);MetabolicModel.tick(s,intensity);store(p,s);
  if(intensity>.1&&p.tickCount%100==0){double extra=Math.max(0,.15-s.energy())*3+(1-s.physicalFactor())*.35*intensity;if(extra>0)WorldLogicApi.addExertion(p,extra);}
  if(p.tickCount%40==0)MetabolismSync.send(p);
 }
 private static void finish(LivingEntityUseItemEvent.Finish e){
  if(!(e.getEntity() instanceof ServerPlayer p)||!active(p))return;
  FoodProfiles.profile(e.getItem()).ifPresent(f->{var s=state(p);MetabolicModel.ingest(s,f.meal(),f.known());store(p,s);MetabolismSync.send(p);});
 }
 private static void clonePlayer(PlayerEvent.Clone e){if(e.getEntity() instanceof ServerPlayer p&&e.getOriginal() instanceof ServerPlayer old)store(p,e.isWasDeath()?new MetabolicModel.State():state(old));}
 private static void login(PlayerEvent.PlayerLoggedInEvent e){if(e.getEntity() instanceof ServerPlayer p)MetabolismSync.send(p);}
 private static void logout(PlayerEvent.PlayerLoggedOutEvent e){if(e.getEntity() instanceof ServerPlayer p)POS.remove(p);}
 private static String status(ServerPlayer p){var s=state(p);return String.format(Locale.ROOT,"Metabolism quality=%.0f energy=%.0f stomach=%.1f toxin=%.1f deficient=%d critical=%d recovery=%.3f",100*s.quality(),100*s.energy(),s.stomach,100*s.toxin,s.deficient(),s.critical(),s.recovery());}
 private static void foodTooltip(ItemTooltipEvent e){FoodProfiles.profile(e.getItemStack()).ifPresent(f->{var meal=f.meal();var n=meal.nutrients();var list=e.getToolTip();
  list.add(Component.translatable("metabolism.food.energy",Math.round(meal.kcal())).withStyle(ChatFormatting.GOLD));
  if(!f.known()){list.add(Component.translatable("metabolism.food.unknown").withStyle(ChatFormatting.GRAY));return;}
  list.add(Component.translatable("metabolism.food.macros",fmt(n[0]),fmt(n[1]),fmt(n[2]),fmt(n[3])).withStyle(ChatFormatting.GRAY));
  for(int i=4;i<MetabolicModel.N;i++)if(n[i]/MetabolicModel.DAILY[i]>=.05)list.add(Component.translatable("metabolism.stat."+MetabolicModel.KEYS[i]).append(" "+Math.round(n[i]/MetabolicModel.DAILY[i]*100)+"%").withStyle(ChatFormatting.GREEN));
  if(meal.toxin()>0)list.add(Component.translatable("metabolism.food.toxin",Math.round(meal.toxin()*100)).withStyle(ChatFormatting.RED));
 });}
 private static String fmt(double n){return String.format(Locale.ROOT,"%.1f",n);}
 private static void commands(RegisterCommandsEvent e){
  e.getDispatcher().register(Commands.literal("metabolism").then(Commands.literal("status").executes(c->{var p=c.getSource().getPlayerOrException();c.getSource().sendSuccess(()->Component.literal(status(p)),false);return 1;}).then(Commands.argument("player",EntityArgument.player()).requires(s->s.hasPermission(2)).executes(c->{var p=EntityArgument.getPlayer(c,"player");c.getSource().sendSuccess(()->Component.literal(status(p)),false);return 1;}))).then(Commands.literal("selftest").requires(s->s.hasPermission(2)).executes(c->{ModelChecks.run();ModelChecks.verifyCore(c.getSource().getServer());c.getSource().sendSuccess(()->Component.literal("METABOLISM_MODEL_CHECKS_PASSED"),false);return 1;})));
 }
}
