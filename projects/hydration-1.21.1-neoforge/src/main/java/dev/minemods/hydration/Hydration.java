package dev.minemods.hydration;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import dev.minemods.worldlogic.api.WorldLogicApi;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.ClipContext;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import java.util.Locale;
import java.util.WeakHashMap;

@Mod("hydration")
public final class Hydration {
 private static final String WATER="hydration_water", EXPOSURE="hydration_exposure", RESERVE="hydration_reserve", PENDING="hydration_pending";
 private static final WeakHashMap<ServerPlayer,Vec3> LAST_POS=new WeakHashMap<>();
 private static final WeakHashMap<ServerPlayer,Long> LAST_SIP=new WeakHashMap<>();
 public Hydration(IEventBus modBus){
  // Read both constants at runtime rather than inlining compile-time values.
  try {if(WorldLogicApi.class.getField("API_VERSION").getInt(null)!=1||WorldLogicApi.class.getField("API_REVISION").getInt(null)<7)throw new IllegalStateException("Hydration requires World Logic API v1 revision 7 or later");}
  catch(ReflectiveOperationException e){throw new IllegalStateException("World Logic API revision unavailable",e);}
  HydrationItems.register(modBus);
  modBus.addListener(HydrationSync::register);
  var bus=NeoForge.EVENT_BUS;
  bus.addListener(Hydration::tick);
  bus.addListener(Hydration::fill);
  bus.addListener(Hydration::finished);
  bus.addListener(Hydration::clonePlayer);
  bus.addListener(Hydration::login);
  bus.addListener(Hydration::logout);
  bus.addListener(Hydration::tooltip);
  bus.addListener(Hydration::commands);
  bus.addListener(VesselTreatment::interact);
  bus.addListener(VesselTreatment::tick);
  bus.addListener(VesselTreatment::logout);
  bus.addListener(WaterInfrastructure::interact);
 }
 public static boolean active(ServerPlayer p){return p.isAlive()&&!p.isCreative()&&!p.isSpectator()&&!(p instanceof FakePlayer);}
 public static WaterBalance.State state(ServerPlayer p){
  var n=p.getPersistentData();
  return new WaterBalance.State(n.contains(WATER)?n.getDouble(WATER):100,n.getDouble(EXPOSURE),n.getDouble(RESERVE),n.getDouble(PENDING));
 }
 private static void store(ServerPlayer p,WaterBalance.State s){
  p.getPersistentData().putDouble(WATER,s.water());
  p.getPersistentData().putDouble(EXPOSURE,s.exposure());
  p.getPersistentData().putDouble(RESERVE,s.reserve());p.getPersistentData().putDouble(PENDING,s.pending());
 }
 public static void applyDrink(ServerPlayer p,WaterBalance.Quality q,double ml,double risk){
  if(active(p)) {store(p,WaterBalance.drink(state(p),q,ml,risk));HydrationSync.send(p);}
 }
 private static void tick(PlayerTickEvent.Post e){
  if(!(e.getEntity() instanceof ServerPlayer p)||p.tickCount%20!=0)return;
  if(!active(p)){LAST_POS.remove(p);HydrationSync.send(p);return;}
  Vec3 previous=LAST_POS.put(p,p.position());
  double distance=previous==null?0:p.position().distanceToSqr(previous);
  boolean moving=distance>.025&&distance<225;
  var at=WorldLogicApi.atmosphere(p.serverLevel(),p.blockPosition());
  var env=at.map(WorldLogicApi.Atmosphere::environment).or(()->WorldLogicApi.environment(p.serverLevel(),p.blockPosition()));
  double c=env.map(WorldLogicApi.Environment::ambientCelsiusEstimate).orElse(20.0);
  double rh=at.map(WorldLogicApi.Atmosphere::relativeHumidityPercent).orElse(50.0);
  double solar=env.map(WorldLogicApi.Environment::solarExposure).orElse(0.0);
  boolean covered=env.map(WorldLogicApi.Environment::covered).orElse(true);
  var body=PhysiologyBridge.sample(p);
  var s=WaterBalance.tick(state(p),WaterBalance.loss(c,rh,moving,p.isSprinting()&&moving,p.isSwimming()&&moving,covered,solar,body.bodyC(),body.bleeding()));
  store(p,s);
  if(p.tickCount%100==0&&s.water()<25)WorldLogicApi.addExertion(p,s.water()<10?2.0:.7);
  if(p.tickCount%200==0&&s.water()<=0)p.hurt(p.damageSources().dryOut(),1f);
  if(p.tickCount%100==0&&s.exposure()>=25)p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,120,0,false,true));
  HydrationSync.send(p);
 }
 static WorldLogicApi.WaterProfile targeted(ServerPlayer p){
  Vec3 eye=p.getEyePosition();
  var hit=p.serverLevel().clip(new ClipContext(eye,eye.add(p.getLookAngle().scale(Math.min(4.5,p.blockInteractionRange()))),
   ClipContext.Block.OUTLINE,ClipContext.Fluid.ANY,p));
  if(hit.getType()!=HitResult.Type.BLOCK)return null;
  BlockPos pos=hit.getBlockPos();
  if(!p.serverLevel().hasChunkAt(pos)||!p.serverLevel().getFluidState(pos).is(net.minecraft.tags.FluidTags.WATER)||!(p.serverLevel().getBlockState(pos).getBlock() instanceof net.minecraft.world.level.block.LiquidBlock))return null;
  var profile=WorldLogicApi.waterProfile(p.serverLevel(),pos);
  return profile.quality()==WorldLogicApi.WaterQuality.NOT_WATER?null:profile;
 }
 private static WaterBalance.Quality quality(WorldLogicApi.WaterProfile profile){
  return WaterBalance.Quality.valueOf(profile.quality().name());
 }
 private static void fill(PlayerInteractEvent.RightClickItem e){
  if(!(e.getEntity() instanceof ServerPlayer p)||!e.getItemStack().is(Items.GLASS_BOTTLE)||p.isSpectator())return;
  var profile=targeted(p);
  if(profile==null||!profile.sourceBlock())return;
  ItemStack bottle=WaterBottle.create(profile);
  p.setItemInHand(e.getHand(),ItemUtils.createFilledResult(e.getItemStack(),p,bottle));
  p.level().playSound(null,p.getX(),p.getY(),p.getZ(),SoundEvents.BOTTLE_FILL,SoundSource.PLAYERS,1f,1f);
  e.setCancellationResult(InteractionResult.SUCCESS);e.setCanceled(true);
 }
 private static void finished(LivingEntityUseItemEvent.Finish e){
  if(!(e.getEntity() instanceof ServerPlayer p)||!active(p))return;
  var item=e.getItem();
  var batch=WaterBottle.read(item);
  if(batch.isPresent()){
   var d=batch.get();applyDrink(p,d.quality(),d.ml(),d.risk());return;
  }
  // Vanilla moisture-bearing foods; only after the actual consumption event.
  double ml=item.is(Items.MELON_SLICE)?65:item.is(Items.APPLE)||item.is(Items.BEETROOT)?35:
   item.is(Items.MUSHROOM_STEW)||item.is(Items.BEETROOT_SOUP)||item.is(Items.RABBIT_STEW)?100:
   item.is(Items.MILK_BUCKET)?150:0;
  if(ml>0)applyDrink(p,WaterBalance.Quality.DRINKABLE,ml,0);
 }
 static void sip(ServerPlayer p){
  if(!active(p)||!p.getMainHandItem().isEmpty()||!p.getOffhandItem().isEmpty())return;
  long now=p.serverLevel().getGameTime();
  long previous=LAST_SIP.getOrDefault(p,now-30);
  if(now>=previous&&now-previous<30)return;
  LAST_SIP.put(p,now);
  if(!WaterBalance.canDrink(state(p)))return;
  var profile=targeted(p);
  if(profile==null)return;
  applyDrink(p,quality(profile),250,profile.contaminationRisk());
  p.level().playSound(null,p.getX(),p.getY(),p.getZ(),SoundEvents.GENERIC_DRINK,SoundSource.PLAYERS,.6f,1f);
 }
 private static void tooltip(net.neoforged.neoforge.event.entity.player.ItemTooltipEvent e){
  var batch=WaterBottle.read(e.getItemStack());if(batch.isEmpty())return;
  var d=batch.get();for(String kind:d.origins().stream().sorted().toList())e.getToolTip().add(Component.translatable("hydration.water.kind."+kind.toLowerCase(Locale.ROOT)));
  e.getToolTip().add(Component.translatable("hydration.water.quality."+d.quality().name().toLowerCase(Locale.ROOT)));
 }
 private static void clonePlayer(PlayerEvent.Clone e){
  if(!(e.getEntity() instanceof ServerPlayer p))return;
  if(!e.isWasDeath()&&e.getOriginal() instanceof ServerPlayer old)store(p,state(old));
  else store(p,new WaterBalance.State(100,0));
 }
 private static void login(PlayerEvent.PlayerLoggedInEvent e){if(e.getEntity() instanceof ServerPlayer p)HydrationSync.send(p);}
 private static void logout(PlayerEvent.PlayerLoggedOutEvent e){
  if(e.getEntity() instanceof ServerPlayer p){LAST_POS.remove(p);LAST_SIP.remove(p);HydrationSync.forget(p);}
 }
 private static String status(ServerPlayer p){
  var s=state(p);return String.format(Locale.ROOT,"Hydration water=%d exposure=%d reserve=%d pending=%d",(int)Math.round(s.water()),(int)Math.round(s.exposure()),(int)Math.round(s.reserve()),(int)Math.round(s.pending()));
 }
 private static void commands(RegisterCommandsEvent e){
  e.getDispatcher().register(Commands.literal("hydration")
   .then(Commands.literal("status").executes(c->{var p=c.getSource().getPlayerOrException();c.getSource().sendSuccess(()->Component.literal(status(p)),false);return 1;})
    .then(Commands.argument("player",EntityArgument.player()).requires(s->s.hasPermission(2)).executes(c->{var p=EntityArgument.getPlayer(c,"player");c.getSource().sendSuccess(()->Component.literal(status(p)),false);return 1;})))
   .then(Commands.literal("set").requires(s->s.hasPermission(2)).then(Commands.argument("player",EntityArgument.player()).then(Commands.argument("water",DoubleArgumentType.doubleArg(0,100)).executes(c->{var p=EntityArgument.getPlayer(c,"player");store(p,new WaterBalance.State(DoubleArgumentType.getDouble(c,"water"),0));HydrationSync.send(p);c.getSource().sendSuccess(()->Component.literal(status(p)),false);return 1;}))))
   .then(Commands.literal("inspect").requires(s->s.hasPermission(2)).then(Commands.argument("pos",BlockPosArgument.blockPos()).executes(c->{var p=WorldLogicApi.waterProfile(c.getSource().getLevel(),BlockPosArgument.getLoadedBlockPos(c,"pos"));c.getSource().sendSuccess(()->Component.literal("Hydration source="+p.kind()+" quality="+p.quality()),false);return 1;})))
   .then(Commands.literal("tankinspect").requires(s->s.hasPermission(2)).then(Commands.argument("pos",BlockPosArgument.blockPos()).executes(c->{var be=c.getSource().getLevel().getBlockEntity(BlockPosArgument.getLoadedBlockPos(c,"pos"));if(!(be instanceof TankEntity tank))throw new com.mojang.brigadier.exceptions.SimpleCommandExceptionType(Component.literal("Not a hydration tank")).create();c.getSource().sendSuccess(()->Component.literal(tank.diagnostic()),false);return 1;})))
   .then(Commands.literal("infrastructuretest").requires(s->s.hasPermission(2)).executes(c->{InfrastructureChecks.run(c.getSource().getLevel());c.getSource().sendSuccess(()->Component.literal("HYDRATION_INFRASTRUCTURE_CHECKS_PASSED"),false);return 1;}))
   .then(Commands.literal("vesseltest").requires(s->s.hasPermission(2)).executes(c->{VesselChecks.run();c.getSource().sendSuccess(()->Component.literal("HYDRATION_VESSEL_CHECKS_PASSED"),false);return 1;}))
   .then(Commands.literal("selftest").requires(s->s.hasPermission(2)).executes(c->{ModelChecks.run();c.getSource().sendSuccess(()->Component.literal("HYDRATION_MODEL_CHECKS_PASSED"),false);return 1;})));
 }
}
