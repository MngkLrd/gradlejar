package dev.minemods.metabolism;
import dev.minemods.worldlogic.api.WorldLogicApi;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.alchemy.PotionContents;
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
 private static final WeakHashMap<ServerPlayer,RestSample> REST=new WeakHashMap<>();
 private static final class RestSample {final RestRecovery.Clock clock=new RestRecovery.Clock();net.minecraft.server.level.ServerLevel level;Vec3 position;double rate;boolean delegated;}
 public Metabolism(IEventBus modBus){
  modBus.addListener(MetabolismSync::register);
  var bus=NeoForge.EVENT_BUS;
  bus.addListener(Metabolism::tick);bus.addListener(Metabolism::startDrink);bus.addListener(Metabolism::stopDrink);bus.addListener(Metabolism::finish);bus.addListener(Metabolism::clonePlayer);
  bus.addListener(Metabolism::login);bus.addListener(Metabolism::logout);bus.addListener(Metabolism::commands);bus.addListener(Metabolism::foodTooltip);
 }
 static boolean active(ServerPlayer p){return p.isAlive()&&!p.isCreative()&&!p.isSpectator()&&!(p instanceof FakePlayer);}
 public static MetabolicModel.State state(ServerPlayer p){
  var s=new MetabolicModel.State();var root=p.getPersistentData();if(!root.contains(KEY))return s;return decode(root.getCompound(KEY));
 }
 static CompoundTag encode(MetabolicModel.State s){
  var n=new CompoundTag();n.putInt("schema",4);n.putDouble("overload",s.overload);n.putInt("drinkCooldown",s.drinkCooldown);n.putDouble("lastInstant",s.lastInstant);n.putDouble("lastDigest",s.lastDigest);n.putDouble("fast",s.fast);n.putDouble("reserve",s.reserve);n.putDouble("stomach",s.stomach);
  n.putDouble("queued",s.queuedEnergy);n.putDouble("toxin",s.toxin);n.putDouble("absorption",s.absorption);
  n.putDouble("burn",s.burn);n.putDouble("activity",s.activity);n.putDouble("unknown",s.unknown);n.putDouble("observed",s.observedSeconds);n.putInt("daySeconds",s.daySeconds);n.putDouble("dayIntake",s.dayIntake);n.putDouble("dayBurn",s.dayBurn);n.putDouble("dayQuality",s.dayQuality);n.putInt("historyCount",s.historyCount);for(int i=0;i<MetabolicModel.State.HISTORY;i++){n.putDouble("hi_"+i,s.historyIntake[i]);n.putDouble("hb_"+i,s.historyBurn[i]);n.putDouble("hq_"+i,s.historyQuality[i]);}
  for(int i=0;i<MetabolicModel.N;i++){n.putDouble("s_"+i,s.stores[i]);n.putDouble("p_"+i,s.pending[i]);n.putDouble("b_"+i,s.baseline[i]);}return n;
 }
 static MetabolicModel.State decode(CompoundTag n){
  var s=new MetabolicModel.State();int schema=n.getInt("schema");if(schema<1||schema>4)return s;
  if(schema>=4){s.overload=MetabolicModel.clamp(n.getDouble("overload"),0,1);s.drinkCooldown=(int)MetabolicModel.clamp(n.getInt("drinkCooldown"),0,15);s.lastInstant=MetabolicModel.clamp(n.getDouble("lastInstant"),0,8);s.lastDigest=MetabolicModel.clamp(n.getDouble("lastDigest"),0,1.8);}
  s.fast=MetabolicModel.clamp(n.getDouble("fast"),0,MetabolicModel.FAST_MAX);s.reserve=MetabolicModel.clamp(n.getDouble("reserve"),0,MetabolicModel.FAT_MAX);
  s.stomach=MetabolicModel.clamp(n.getDouble("stomach"),0,MetabolicModel.STOMACH_MAX);s.queuedEnergy=MetabolicModel.clamp(n.getDouble("queued"),0,s.stomach);
  s.toxin=MetabolicModel.clamp(n.getDouble("toxin"),0,1);s.absorption=MetabolicModel.clamp(n.getDouble("absorption"),.15,1);s.burn=MetabolicModel.clamp(n.getDouble("burn"),90,500);
  s.activity=MetabolicModel.clamp(n.getDouble("activity"),0,1);s.unknown=MetabolicModel.clamp(n.getDouble("unknown"),0,999);s.observedSeconds=schema>=2?MetabolicModel.clamp(n.getDouble("observed"),0,MetabolicModel.GAME_DAY_SECONDS):0;
  for(int i=0;i<MetabolicModel.N;i++){s.stores[i]=MetabolicModel.clamp(n.getDouble("s_"+i),0,1.5);s.pending[i]=MetabolicModel.clamp(n.getDouble("p_"+i),0,8);s.baseline[i]=schema>=2&&n.contains("b_"+i)?MetabolicModel.clamp(n.getDouble("b_"+i),0,1.5):s.stores[i];}if(schema>=3){s.daySeconds=(int)MetabolicModel.clamp(n.getInt("daySeconds"),0,1199);s.dayIntake=MetabolicModel.clamp(n.getDouble("dayIntake"),0,10000000);s.dayBurn=MetabolicModel.clamp(n.getDouble("dayBurn"),0,100000);s.dayQuality=MetabolicModel.clamp(n.getDouble("dayQuality"),0,s.daySeconds);s.historyCount=(int)MetabolicModel.clamp(n.getInt("historyCount"),0,7);for(int i=0;i<7;i++){s.historyIntake[i]=MetabolicModel.clamp(n.getDouble("hi_"+i),0,10000000);s.historyBurn[i]=MetabolicModel.clamp(n.getDouble("hb_"+i),0,100000);s.historyQuality[i]=MetabolicModel.clamp(n.getDouble("hq_"+i),0,1);}}return s;
 }
 static void store(ServerPlayer p,MetabolicModel.State s){p.getPersistentData().put(KEY,encode(s));}
 private static void tick(PlayerTickEvent.Post e){
  if(!(e.getEntity() instanceof ServerPlayer p))return;
  if(!active(p)){POS.remove(p);REST.remove(p);if(p.tickCount%40==0)MetabolismSync.send(p);return;}
  var sample=REST.computeIfAbsent(p,k->new RestSample());
  if(sample.level!=p.serverLevel()){REST.remove(p);sample=new RestSample();REST.put(p,sample);sample.level=p.serverLevel();POS.remove(p);}
  boolean second=p.tickCount%20==0;
  if(second||sample.position==null){var s=state(p);EnvironmentBridge.sample(p,s);sample.rate=s.restRate(p.getFoodData().getFoodLevel());sample.delegated=RestRecovery.coreOwns(p);}
  if(second){Vec3 old=POS.put(p,p.position());double distance=old==null?0:Math.sqrt(p.position().distanceToSqr(old));
  double intensity=distance>20?0:Math.min(1,distance/5.5);if(distance>.2&&p.isSprinting())intensity=Math.max(intensity,.8);
  if(distance>.2&&p.isSwimming())intensity=Math.max(intensity,.65);
  var s=state(p);EnvironmentBridge.sample(p,s);double absorbed=MetabolicModel.tick(s,intensity);
  boolean resting=intensity<.08&&!p.isSprinting()&&!p.isSwimming()&&!p.isUsingItem();
  s.lastDigest=recover(p,MetabolicModel.digestionPoints(s,absorbed,resting));store(p,s);
  // Core owns action costs and exertion penalties. Metabolic activity already debits kcal in the model.
  sample.rate=s.restRate(p.getFoodData().getFoodLevel());}
  // Only actual stationary/safe rest. Mining/attacking raises authoritative fatigue and resets the budget.
  Vec3 here=p.position();boolean still=sample.position!=null&&here.distanceToSqr(sample.position)<.00015625;
  sample.position=here;
  boolean safe=still&&!p.isPassenger()&&p.onGround()&&!p.isSprinting()&&!p.isSwimming()&&!p.isInWater()&&!p.isInLava()&&!p.onClimbable()&&!p.isUsingItem();
  var fatigue=WorldLogicApi.fatigue(p);
  double before=fatigue.fatiguePercent();
  double request=sample.clock.request(before,safe&&fatigue.enabled()&&!sample.delegated,sample.rate);
  if(request>1e-8){double restored=recover(p,request);sample.clock.settled(before,before-restored);}
  if(p.tickCount%20==0)MetabolismSync.send(p);
 }
 /** Core alone owns stamina. Return the actual accepted points, not the theoretical bonus. */
 static double recover(ServerPlayer p,double points){
  if(!active(p)||!Double.isFinite(points)||points<=0)return 0;
  var before=WorldLogicApi.fatigue(p);if(!before.enabled()||before.fatiguePercent()<=0)return 0;
  WorldLogicApi.recoverExertion(p,Math.min(before.fatiguePercent(),points));
  return Math.max(0,before.fatiguePercent()-WorldLogicApi.fatigue(p).fatiguePercent());
 }
 public record IntakeResult(double acceptedKcal,double immediateKcal,double staminaPoints){}
 public static IntakeResult acceptMeal(ServerPlayer p,MetabolicModel.Meal meal,boolean known){
  if(!active(p))return new IntakeResult(0,0,0);
  var s=state(p);EnvironmentBridge.sample(p,s);var result=MetabolicModel.consume(s,meal,known);
  s.lastInstant=recover(p,result.requestedPoints());store(p,s);REST.remove(p);MetabolismSync.send(p);
  return new IntakeResult(result.acceptedKcal(),result.quickKcal(),s.lastInstant);
 }
 public static double acceptWater(ServerPlayer p,int volume,double risk,double salt){
  if(!active(p))return 0;
  var s=state(p);double requested=MetabolicModel.waterBoost(s,volume,risk,salt);
  if(requested<=0)return 0;
  s.lastInstant=recover(p,requested);store(p,s);REST.remove(p);MetabolismSync.send(p);return s.lastInstant;
 }
 public static void cake(ServerPlayer p){var food=FoodProfiles.cakeSlice();acceptMeal(p,food.meal(),true);}
 /** Called from the vanilla canEat return; never blocks an otherwise permitted vanilla use. */
 public static boolean canEatMore(Player p){
  if(!p.isAlive()||p.isCreative()||p.isSpectator())return false;
  if(p instanceof ServerPlayer sp)return active(sp)&&state(sp).stomach<MetabolicModel.STOMACH_MAX-1;
  var s=MetabolismSync.client;
  return s!=null&&s.active()&&s.player().equals(p.getUUID())&&s.values()[MetabolicModel.N+2]<MetabolicModel.STOMACH_MAX-1;
 }
 private record DrinkStart(net.minecraft.world.item.Item item,WaterIntakeBridge.Water water){}
 private static final WeakHashMap<ServerPlayer,DrinkStart> DRINKS=new WeakHashMap<>();
 private static void startDrink(LivingEntityUseItemEvent.Start e){
  if(!(e.getEntity() instanceof ServerPlayer p)||!active(p))return;
  DRINKS.remove(p);var w=WaterIntakeBridge.read(e.getItem());if(w!=null&&w.volume()>0)DRINKS.put(p,new DrinkStart(e.getItem().getItem(),w));
 }
 private static void stopDrink(LivingEntityUseItemEvent.Stop e){if(e.getEntity() instanceof ServerPlayer p)DRINKS.remove(p);}
 private static boolean waterPotion(ItemStack stack){
  var potion=stack.get(DataComponents.POTION_CONTENTS);
  return stack.is(Items.POTION)&&potion!=null&&potion.is(Potions.WATER)&&!potion.hasEffects();
 }
 private static void finish(LivingEntityUseItemEvent.Finish e){
  if(!(e.getEntity() instanceof ServerPlayer p)||!active(p))return;
  var start=DRINKS.remove(p);var item=e.getItem();
  var food=FoodProfiles.profile(item,p);if(food.isPresent()){var f=food.get();acceptMeal(p,f.meal(),f.known());return;}
  // Hydration rev5+ reports actual aged/consumed batches, so the pre-use snapshot must not win its cooldown.
  if(!WaterIntakeBridge.useItemFallback(WaterIntakeBridge.callbackOwnsWater(),start!=null||waterPotion(item)||WaterIntakeBridge.read(item)!=null))return;
  if(start!=null&&item.is(start.item())){
   var after=WaterIntakeBridge.read(e.getResultStack());
   if(e.getResultStack().is(start.item())&&after!=null){int volume=start.water().volume()-after.volume();if(volume>0)acceptWater(p,volume,start.water().risk(),start.water().salt());return;}
   if(item.is(Items.POTION)&&e.getResultStack().is(Items.GLASS_BOTTLE)){acceptWater(p,start.water().volume(),start.water().risk(),start.water().salt());return;}
  }
  if(waterPotion(item)&&e.getResultStack().is(Items.GLASS_BOTTLE))acceptWater(p,250,0,0);
 }
 private static void clonePlayer(PlayerEvent.Clone e){if(e.getEntity() instanceof ServerPlayer p&&e.getOriginal() instanceof ServerPlayer old)store(p,e.isWasDeath()?new MetabolicModel.State():state(old));}
 private static void login(PlayerEvent.PlayerLoggedInEvent e){if(e.getEntity() instanceof ServerPlayer p)MetabolismSync.send(p);}
 private static void logout(PlayerEvent.PlayerLoggedOutEvent e){if(e.getEntity() instanceof ServerPlayer p){POS.remove(p);DRINKS.remove(p);REST.remove(p);}}
 private static String status(ServerPlayer p){var s=state(p);return String.format(Locale.ROOT,"Metabolism quality=%.0f energy=%.0f stomach=%.1f toxin=%.1f deficient=%d critical=%d recovery=%.3f",100*s.quality(),100*s.energy(),s.stomach,100*s.toxin,s.deficient(),s.critical(),s.recovery());}
 private static void foodTooltip(ItemTooltipEvent e){FoodProfiles.profile(e.getItemStack(),e.getEntity()).ifPresent(f->{var meal=f.meal();var n=meal.nutrients();var list=e.getToolTip();
  list.add(Component.translatable("metabolism.food.energy",Math.round(meal.kcal())).withStyle(ChatFormatting.GOLD));
  if(!f.known()){list.add(Component.translatable("metabolism.food.unknown").withStyle(ChatFormatting.GRAY));return;}
  list.add(Component.translatable("metabolism.food.macros",fmt(n[0]),fmt(n[1]),fmt(n[2]),fmt(n[3])).withStyle(ChatFormatting.GRAY));
  for(int i=4;i<MetabolicModel.N;i++)if(n[i]/MetabolicModel.DAILY[i]>=.05)list.add(Component.translatable("metabolism.stat."+MetabolicModel.KEYS[i]).append(" "+Math.round(n[i]/MetabolicModel.DAILY[i]*100)+"%").withStyle(ChatFormatting.GREEN));
  if(meal.toxin()>0)list.add(Component.translatable("metabolism.food.toxin",Math.round(meal.toxin()*100)).withStyle(ChatFormatting.RED));
 });}
 private static String fmt(double n){return String.format(Locale.ROOT,"%.1f",n);}
 public static boolean restoreNutrition(ServerPlayer p,boolean clearDisorders){
  if(!active(p))return false;
  var s=state(p);MetabolicModel.restoreNutrition(s,clearDisorders);p.getFoodData().setFoodLevel(20);p.getFoodData().setSaturation(20);p.getFoodData().setExhaustion(0);store(p,s);REST.remove(p);MetabolismSync.send(p);return true;
 }
 public static double restingRecoveryPointsPerSecond(ServerPlayer p){
  if(!active(p)||!WorldLogicApi.fatigue(p).enabled())return 0;
  // Core 1.5.1 may query each tick. Reuse the one-second authoritative sample; intake/reset invalidate it.
  var cached=REST.get(p);if(cached!=null&&cached.level==p.serverLevel()&&cached.position!=null&&cached.rate>0)return cached.rate;
  var s=state(p);EnvironmentBridge.sample(p,s);return s.restRate(p.getFoodData().getFoodLevel());
 }
 private static int feed(com.mojang.brigadier.context.CommandContext<net.minecraft.commands.CommandSourceStack> c,java.util.Collection<ServerPlayer> players){
  int count=0;for(var p:players)if(restoreNutrition(p,false))count++;
  final int done=count;c.getSource().sendSuccess(()->Component.literal("Metabolism: fed "+done+" player(s)"),true);return done;
 }
 private static com.mojang.brigadier.builder.LiteralArgumentBuilder<net.minecraft.commands.CommandSourceStack> feedCommand(String name){
  return Commands.literal(name).requires(s->s.hasPermission(2)).executes(c->feed(c,java.util.List.of(c.getSource().getPlayerOrException())))
   .then(Commands.argument("players",EntityArgument.players()).executes(c->feed(c,EntityArgument.getPlayers(c,"players"))));
 }
 public record OrganInputs(boolean available,double digestiveFunction,double hepaticFunction){}
 public static OrganInputs organInputs(ServerPlayer p){var s=state(p);EnvironmentBridge.sampleOrgans(p,s);return new OrganInputs(s.organInputsAvailable,s.digestiveFunction,s.hepaticFunction);}
 private static void commands(RegisterCommandsEvent e){
  var dispatcher=e.getDispatcher();var root=dispatcher.getRoot().getChild("worldlogic");
  if(dispatcher.getRoot().getChild("feed")==null)dispatcher.register(feedCommand("feed"));
  if(root==null||root.getChild("feed")==null)dispatcher.register(Commands.literal("worldlogic").requires(s->s.hasPermission(2)).then(feedCommand("feed")));
  e.getDispatcher().register(Commands.literal("metabolism").then(Commands.literal("status").executes(c->{var p=c.getSource().getPlayerOrException();c.getSource().sendSuccess(()->Component.literal(status(p)),false);return 1;}).then(Commands.argument("player",EntityArgument.player()).requires(s->s.hasPermission(2)).executes(c->{var p=EntityArgument.getPlayer(c,"player");c.getSource().sendSuccess(()->Component.literal(status(p)),false);return 1;}))).then(Commands.literal("selftest").requires(s->s.hasPermission(2)).executes(c->{ModelChecks.run();ModelChecks.verifyCore(c.getSource().getServer());c.getSource().sendSuccess(()->Component.literal("METABOLISM_MODEL_CHECKS_PASSED"),false);return 1;})));
 }
}
