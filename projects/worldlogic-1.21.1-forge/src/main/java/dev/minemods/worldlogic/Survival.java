package dev.minemods.worldlogic;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
public final class Survival {
 private static final String FATIGUE="worldlogic_exertion",INJURY="worldlogic_injury",SMOKE="worldlogic_smoke";
 static boolean active(Player p){return p.level() instanceof ServerLevel&&!p.isCreative()&&!p.isSpectator();}
 public static double fatigue(Player p){return Math.max(0,Math.min(100,p.getPersistentData().getDouble(FATIGUE)));}
 static void work(Player p,double amount){if(active(p)&&Rules.on("exertion"))p.getPersistentData().putDouble(FATIGUE,Math.min(100,fatigue(p)+amount*Rules.strength("movement")));}
 public static void mine(BlockEvent.BreakEvent e){if(!e.isCanceled()&&active(e.getPlayer())){float hardness=e.getState().getDestroySpeed(e.getLevel(),e.getPos());if(hardness>0)work(e.getPlayer(),Math.min(5,.6+hardness*.5));}}
 public static void jump(LivingEvent.LivingJumpEvent e){if(e.getEntity() instanceof Player p)work(p,1.2);}
 public static void hit(LivingHurtEvent e){if(!e.isCanceled()&&e.getSource().getEntity() instanceof Player p)work(p,1.8);}
 public static void fall(LivingFallEvent e){if(!(e.getEntity() instanceof Player p)||!active(p)||!Rules.on("injuries")||e.getDistance()<6||e.getDamageMultiplier()<=0)return;var l=(ServerLevel)p.level();long duration=(long)Math.min(1800,(e.getDistance()-5)*75*Rules.strength("impact"));p.getPersistentData().putLong(INJURY,Math.max(p.getPersistentData().getLong(INJURY),l.getGameTime()+duration));p.displayClientMessage(Component.literal("Your legs are injured. Rest and avoid heavy work."),true);}
 public static double penalty(Player p,long now){
  double result=0;
  if(Rules.on("exertion"))result=Math.max(result,Math.max(0,fatigue(p)-40)*.005);
  if(Rules.on("injuries")&&p.getPersistentData().getLong(INJURY)>now)result=Math.max(result,.25);
  return result;
 }
 public static void tick(TickEvent.PlayerTickEvent.Post e){
  Player p=e.player;if(!active(p)||!(p.level() instanceof ServerLevel l))return;long now=l.getGameTime();
  if(now%20!=0)return;
  var tag=p.getPersistentData();
  if(Rules.on("exertion")){
   boolean moving=p.isSprinting()||p.isSwimming()||p.onClimbable()&&p.getDeltaMovement().y>.05;
   double mass=Material.mass(p);
   if(moving){work(p,(p.isSwimming()?2.4:1.45)*Math.max(.8,mass));p.causeFoodExhaustion((float)(.08*Math.max(1,mass)*Rules.strength("movement")));}
   else if(p.onGround()&&!p.isInWater()){double recovery=p.getFoodData().getFoodLevel()>=12?2.0:.6;if(p.getDeltaMovement().horizontalDistance()<.04)recovery*=2;if(AirAndFire.nearHeat(l,p.blockPosition(),2)&&p.getDeltaMovement().horizontalDistance()<.04)recovery*=1.6;tag.putDouble(FATIGUE,Math.max(0,fatigue(p)-recovery));}
   if(fatigue(p)>=75){p.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN,35,0,false,false,false));if(fatigue(p)>=92)p.setSprinting(false);}
  }
  if(Rules.on("injuries")&&tag.getLong(INJURY)>now&&p.onGround()&&!p.isSprinting()&&p.getFoodData().getFoodLevel()>=14&&p.getDeltaMovement().horizontalDistance()<.04&&AirAndFire.nearHeat(l,p.blockPosition(),2))tag.putLong(INJURY,Math.max(now,tag.getLong(INJURY)-30));
  if(Rules.on("smoke_inhalation")){
   int smoke=tag.getInt(SMOKE);boolean exposed=false;
   if(now%40==0){
    BlockPos at=p.blockPosition();int checked=0;
    for(BlockPos q:BlockPos.betweenClosed(at.offset(-3,-2,-3),at.offset(3,2,3))){
     if(!l.hasChunkAt(q))continue;var s=l.getBlockState(q);if(!(s.getBlock() instanceof CampfireBlock)||!s.getValue(BlockStateProperties.LIT))continue;
     if(++checked>4)break;
     if(AirAndFire.ventPath(l,q.above())==null&&AirAndFire.ventPath(l,BlockPos.containing(p.getX(),p.getEyeY(),p.getZ()))==null){exposed=true;break;}
    }
    smoke=exposed?Math.min(100,smoke+12):Math.max(0,smoke-16);tag.putInt(SMOKE,smoke);
   }
   if(smoke>=40){p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,35,0,false,false,false));if(smoke>=76){p.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN,35,0,false,false,false));if(now%80==0)p.hurt(l.damageSources().generic(),1);}}
  }
  if(now%80==0&&(Rules.on("exertion")&&fatigue(p)>=50||Rules.on("injuries")&&tag.getLong(INJURY)>now||Rules.on("smoke_inhalation")&&tag.getInt(SMOKE)>=40)){
   String message=(Rules.on("exertion")?"Fatigue "+(int)fatigue(p)+"%  ":"")+(Rules.on("injuries")&&tag.getLong(INJURY)>now?"Injured  ":"")+(Rules.on("smoke_inhalation")&&tag.getInt(SMOKE)>=40?"Smoke! Get fresh air":"");p.displayClientMessage(Component.literal(message.strip()),true);
  }
 }
}
