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
 static final class State {long recoverUntil,lastBreak,lastSprint;int streak,wet,web,ladderTicks;long shieldUntil,sharpUntil;String lastFamily;net.minecraft.world.item.Item lastTool;boolean water;int lastAir=300;long pocketCheck;AirAndFire.Pocket pocket;}
 private static final Map<Player,State> STATES=new WeakHashMap<>();
 static State state(Player p){return STATES.computeIfAbsent(p,k->new State());}
 public static void logout(PlayerEvent.PlayerLoggedOutEvent e){STATES.remove(e.getEntity());}
 public static void onBreak(BlockEvent.BreakEvent e){if(e.isCanceled()||!(e.getLevel() instanceof ServerLevel l)||!Rules.on("tool_momentum"))return;var p=e.getPlayer();var s=state(p);var tool=p.getMainHandItem().getItem();var block=Material.family(e.getState());s.streak=(s.lastTool==tool&&block.equals(s.lastFamily)&&l.getGameTime()-s.lastBreak<=40)?Math.min(4,s.streak+1):1;s.lastBreak=l.getGameTime();s.lastTool=tool;s.lastFamily=block;}
 public static void breakSpeed(PlayerEvent.BreakSpeed e){var p=e.getEntity();if(!(p.level() instanceof ServerLevel l)||!Rules.on("tool_momentum"))return;var s=state(p);if(Material.family(e.getState()).equals(s.lastFamily)&&s.lastTool==p.getMainHandItem().getItem()&&l.getGameTime()-s.lastBreak<=40)e.setNewSpeed(e.getNewSpeed()*(1+.03f*s.streak));}
 public static void jump(LivingEvent.LivingJumpEvent e){if(Rules.on("landing_recovery")&&e.getEntity() instanceof Player p&&p.level() instanceof ServerLevel l&&state(p).recoverUntil>l.getGameTime()){Vec3 v=p.getDeltaMovement();p.setDeltaMovement(v.x,v.y*.45,v.z);p.hurtMarked=true;}}
 public static void tick(TickEvent.PlayerTickEvent.Post e){
  Player p=e.player;if(!(p.level() instanceof ServerLevel l))return;if(p.isCreative()||p.isSpectator()){speedPenalty(p,0);return;}State s=state(p);long now=l.getGameTime();
  if(p.isSprinting())s.lastSprint=now;
  if(Rules.on("landing_recovery")&&s.recoverUntil>now)p.setSprinting(false);
  double penalty=0;
  if(Rules.on("exhausted_sprint")&&p.isSprinting())penalty=Math.max(penalty,Math.max(0,10-p.getFoodData().getFoodLevel())*.045);
  if(Rules.on("landing_recovery")&&s.recoverUntil>now)penalty=Math.max(penalty,Math.min(.24,(s.recoverUntil-now)*.014));
  if(Rules.on("armor_noise")&&p.isSprinting()&&p.onGround()&&now%12==0&&Material.armorNoise(p)>1.15)l.playSound(null,p.blockPosition(),net.minecraft.sounds.SoundEvents.CHAIN_STEP,net.minecraft.sounds.SoundSource.PLAYERS,.22f,.9f+l.random.nextFloat()*.2f);
  Combat.contact(l,p,s,now);
  boolean water=p.isEyeInFluid(net.minecraft.tags.FluidTags.WATER);
  if(Rules.on("cold_shock")&&water&&!s.water&&l.getBiome(p.blockPosition()).value().getBaseTemperature()<.15){p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,60,0,false,false,false));p.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN,60,0,false,false,false));}
  int air=p.getAirSupply(),max=p.getMaxAirSupply();
  if(Rules.on("exhausted_breath")&&water&&s.lastSprint>0&&now-s.lastSprint<80&&now%12==0&&air>0&&!p.hasEffect(MobEffects.WATER_BREATHING))p.setAirSupply(air-1);
  if(!water&&s.lastAir<max*.75&&Rules.on("breath_debt")&&air>s.lastAir)p.setAirSupply(Math.min(air,Math.max(0,s.lastAir)+2));
  if(Rules.on("finite_air"))AirAndFire.breathe(l,p,s,water,now);
  s.water=water;s.lastAir=p.getAirSupply();
  if(now%10==0)WetGear.tick(l,p,s);
  if(Rules.on("wet_gear")){double armor=0;for(ItemStack piece:p.getArmorSlots())if(WetGear.absorbent(piece))armor+=WetGear.wet(piece)*.00025;penalty=Math.max(penalty,armor);}
  speedPenalty(p,penalty);
  if(Rules.on("water_extinguish")&&water&&p.isOnFire())p.clearFire();
  if(Rules.on("ladder_grip")&&p.onClimbable()&&p.isShiftKeyDown()&&Math.abs(p.getDeltaMovement().y)<.01){s.ladderTicks++;if(s.ladderTicks>100){Vec3 v=p.getDeltaMovement();p.setDeltaMovement(v.x,-.035,v.z);p.hurtMarked=true;}}else s.ladderTicks=0;
  if(Rules.on("grip")&&p.onClimbable()&&p.getDeltaMovement().y>0&&!p.getMainHandItem().isEmpty()&&!p.getOffhandItem().isEmpty()){Vec3 v=p.getDeltaMovement();p.setDeltaMovement(v.x,v.y*.84,v.z);p.hurtMarked=true;}
  if(Rules.on("cobweb")){if(l.getBlockState(p.blockPosition()).is(Blocks.COBWEB)){s.web=Math.min(30,s.web+1);boolean cutting=Material.item(p.getMainHandItem(),"web_tools")||Material.item(p.getOffhandItem(),"web_tools");if(cutting)s.web=Math.max(0,s.web-5);else if(s.web>10){Vec3 v=p.getDeltaMovement();double grip=1-Math.min(.35,s.web*.009);p.setDeltaMovement(v.multiply(grip,grip,grip));}}else s.web=0;}
 }
 private static final net.minecraft.resources.ResourceLocation SPEED=net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("worldlogic","body_slowdown");
 static void speedPenalty(Player p,double penalty){var a=p.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);if(a==null)return;var old=a.getModifier(SPEED);if(old!=null&&Math.abs(old.amount()+penalty)<.001)return;if(old!=null)a.removeModifier(SPEED);if(penalty>.001)a.addTransientModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(SPEED,-penalty,net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));}
 static void bow(AbstractArrow a){if(Rules.on("wet_bow")&&a.getOwner() instanceof Player p&&WetGear.wet(p.getMainHandItem())>30&&p.getMainHandItem().is(net.minecraft.world.item.Items.BOW)){a.setDeltaMovement(a.getDeltaMovement().scale(WetGear.wet(p.getMainHandItem())>70?.90:.95));a.hurtMarked=true;}}
}
