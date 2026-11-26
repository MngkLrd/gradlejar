package dev.minemods.worldlogic;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.effect.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
public final class Body {
 static final class State {long recoverUntil,lastBreak,lastSprint;int streak,wet,web;Block lastBlock;net.minecraft.world.item.Item lastTool;boolean water;int lastAir=300;long pocketCheck;AirAndFire.Pocket pocket;}
 private static final Map<Player,State> STATES=new WeakHashMap<>();
 static State state(Player p){return STATES.computeIfAbsent(p,k->new State());}
 public static void logout(PlayerEvent.PlayerLoggedOutEvent e){STATES.remove(e.getEntity());}
 public static void onBreak(BlockEvent.BreakEvent e){if(e.isCanceled()||!(e.getLevel() instanceof ServerLevel l)||!Rules.on("tool_momentum"))return;var p=e.getPlayer();var s=state(p);var tool=p.getMainHandItem().getItem();var block=e.getState().getBlock();s.streak=(s.lastTool==tool&&s.lastBlock==block&&l.getGameTime()-s.lastBreak<=40)?Math.min(4,s.streak+1):1;s.lastBreak=l.getGameTime();s.lastTool=tool;s.lastBlock=block;}
 public static void breakSpeed(PlayerEvent.BreakSpeed e){var p=e.getEntity();if(!(p.level() instanceof ServerLevel l)||!Rules.on("tool_momentum"))return;var s=state(p);if(s.lastBlock==e.getState().getBlock()&&s.lastTool==p.getMainHandItem().getItem()&&l.getGameTime()-s.lastBreak<=40)e.setNewSpeed(e.getNewSpeed()*(1+.03f*s.streak));}
 public static void jump(LivingEvent.LivingJumpEvent e){if(Rules.on("landing_recovery")&&e.getEntity() instanceof Player p&&p.level() instanceof ServerLevel l&&state(p).recoverUntil>l.getGameTime()){Vec3 v=p.getDeltaMovement();p.setDeltaMovement(v.x,v.y*.45,v.z);p.hurtMarked=true;}}
 public static void tick(TickEvent.PlayerTickEvent.Post e){
  Player p=e.player;if(!(p.level() instanceof ServerLevel l)||p.isCreative()||p.isSpectator())return;State s=state(p);long now=l.getGameTime();
  if(p.isSprinting())s.lastSprint=now;
  if(Rules.on("landing_recovery")&&s.recoverUntil>now)p.setSprinting(false);
  if(Rules.on("exhausted_sprint")&&p.isSprinting()&&p.getFoodData().getFoodLevel()<=9){p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,8,0,false,false,false));}
  boolean water=p.isEyeInFluid(net.minecraft.tags.FluidTags.WATER);
  if(Rules.on("cold_shock")&&water&&!s.water&&l.getBiome(p.blockPosition()).value().getBaseTemperature()<.15){p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,60,0,false,false,false));p.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN,60,0,false,false,false));}
  int air=p.getAirSupply(),max=p.getMaxAirSupply();
  if(Rules.on("exhausted_breath")&&water&&s.lastSprint>0&&now-s.lastSprint<80&&now%12==0&&air>0&&!p.hasEffect(MobEffects.WATER_BREATHING))p.setAirSupply(air-1);
  if(!water&&s.lastAir<max*.75&&Rules.on("breath_debt")&&air>s.lastAir)p.setAirSupply(Math.min(air,Math.max(0,s.lastAir)+2));
  if(Rules.on("finite_air"))AirAndFire.breathe(l,p,s,water,now);
  s.water=water;s.lastAir=p.getAirSupply();
  if(now%5==0){if(p.isInWater()||l.isRainingAt(p.blockPosition()))s.wet=Math.min(100,s.wet+5);else s.wet=Math.max(0,s.wet-(AirAndFire.nearHeat(l,p.blockPosition(),2)?4:1));}
  if(Rules.on("water_extinguish")&&water&&p.isOnFire())p.clearFire();
  if(Rules.on("grip")&&p.onClimbable()&&p.getDeltaMovement().y>0&&!p.getMainHandItem().isEmpty()&&!p.getOffhandItem().isEmpty()){Vec3 v=p.getDeltaMovement();p.setDeltaMovement(v.x,v.y*.84,v.z);p.hurtMarked=true;}
  if(Rules.on("cobweb")){if(l.getBlockState(p.blockPosition()).is(Blocks.COBWEB)){s.web=Math.min(30,s.web+1);if(s.web>20&&!Material.item(p.getMainHandItem(),"web_tools")){Vec3 v=p.getDeltaMovement();p.setDeltaMovement(v.multiply(.8,.8,.8));}}else s.web=0;}
 }
 static void bow(AbstractArrow a){if(Rules.on("wet_bow")&&a.getOwner() instanceof Player p&&state(p).wet>30&&p.getMainHandItem().is(net.minecraft.world.item.Items.BOW)){a.setDeltaMovement(a.getDeltaMovement().scale(state(p).wet>70?.90:.95));a.hurtMarked=true;}}
}
