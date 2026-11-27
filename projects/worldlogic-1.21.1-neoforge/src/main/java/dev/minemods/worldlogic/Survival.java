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
import net.neoforged.neoforge.event.tick.*;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
public final class Survival {
 private static final String FATIGUE="worldlogic_exertion",INJURY="worldlogic_injury",SMOKE="worldlogic_smoke";
 private static final Map<Player,Long> LAST_WORK=new WeakHashMap<>();
 private record RestSample(ServerLevel level,double x,double z,long time){}
 private static final Map<Player,RestSample> REST_SAMPLE=new WeakHashMap<>();
 static void clearExertionTracking(Player p){LAST_WORK.remove(p);REST_SAMPLE.remove(p);}
 static double recoveryRate(int food,double speed,boolean sneak,boolean warm){
  double rate=speed<.25?4.0:sneak?3.0:2.4;
  if(food<12)rate*=.30;
  if(warm&&speed<.25)rate*=1.6;
  return rate;
 }
 static void updateExertion(Player p,ServerLevel l,long now){
  if(!active(p)||!Rules.on("exertion"))return;
  RestSample previous=REST_SAMPLE.put(p,new RestSample(l,p.getX(),p.getZ(),now));
  double speed=previous!=null&&previous.level()==l&&now>previous.time()&&now-previous.time()<=40
   ?Math.hypot(p.getX()-previous.x(),p.getZ()-previous.z())*20.0/(now-previous.time()):4.3;
  boolean effort=!p.isPassenger()&&(p.isSprinting()||p.isSwimming()||p.onClimbable()&&p.getDeltaMovement().y>.05);
  if(effort){
   double mass=Material.mass(p),armor=Rules.on("armor_fatigue")?CombatConsequences.armorWork(Expedition.armorLoad(p)):1;
   work(p,(p.isSwimming()?2.4:1.45)*Math.max(.8,mass)*armor*Condition.workFactor(p));
   p.causeFoodExhaustion((float)(.08*Math.max(1,mass)*armor*Condition.workFactor(p)*Rules.strength("movement")));
  }else if(!p.isPassenger()&&p.onGround()&&!p.isInWater()&&!p.isInLava()&&!p.isUsingItem()&&!p.onClimbable()
       &&now-LAST_WORK.getOrDefault(p,now-20)>=20&&fatigue(p)>0){
   boolean warm=speed<.25&&AirAndFire.nearHeat(l,p.blockPosition(),2);
   rest(p,recoveryRate(p.getFoodData().getFoodLevel(),speed,p.isShiftKeyDown(),warm));
  }
  if(fatigue(p)>=75){p.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN,35,0,false,false,false));if(fatigue(p)>=92)p.setSprinting(false);}
 }
 static boolean active(Player p){return p.level() instanceof ServerLevel&&!p.isCreative()&&!p.isSpectator();}
 public static double fatigue(Player p){return Math.max(0,Math.min(100,p.getPersistentData().getDouble(FATIGUE)));}
 public static void rest(Player p,double amount){if(active(p)&&Rules.on("exertion"))p.getPersistentData().putDouble(FATIGUE,Math.max(0,fatigue(p)-amount));}
 public static void work(Player p,double amount){if(active(p)&&Rules.on("exertion")){LAST_WORK.put(p,p.level().getGameTime());p.getPersistentData().putDouble(FATIGUE,Math.min(100,fatigue(p)+amount*Rules.strength("movement")));}}
 public static void mine(BlockEvent.BreakEvent e){if(!e.isCanceled()&&active(e.getPlayer())){float hardness=e.getState().getDestroySpeed(e.getLevel(),e.getPos());if(hardness>0)work(e.getPlayer(),Math.min(5,.6+hardness*.5));}}
 public static void jump(LivingEvent.LivingJumpEvent e){if(e.getEntity() instanceof Player p)work(p,1.2);}
 public static void hit(LivingIncomingDamageEvent e){if(!e.isCanceled()&&e.getSource().getEntity() instanceof Player p)work(p,1.8);}
 public static void fall(LivingFallEvent e){if(!(e.getEntity() instanceof Player p)||!active(p)||!Rules.on("injuries")||e.getDistance()<6||e.getDamageMultiplier()<=0)return;var l=(ServerLevel)p.level();long duration=(long)Math.min(1800,(e.getDistance()-5)*75*Rules.strength("impact"));p.getPersistentData().putLong(INJURY,Math.max(p.getPersistentData().getLong(INJURY),l.getGameTime()+duration));p.displayClientMessage(Component.literal("Your legs are injured. Rest and avoid heavy work."),true);}
 static void aggravate(Player p,int ticks){long now=p.level().getGameTime(),old=p.getPersistentData().getLong(INJURY);if(old>now)p.getPersistentData().putLong(INJURY,Math.min(now+1800,old+ticks));}
 static boolean injured(Player p,long now){return Rules.on("injuries")&&p.getPersistentData().getLong(INJURY)>now;}
 public static double penalty(Player p,long now){
  double result=0;
  if(Rules.on("exertion"))result=Math.max(result,Math.max(0,fatigue(p)-40)*.005);
  if(Rules.on("injuries")&&p.getPersistentData().getLong(INJURY)>now)result=Math.max(result,.25);
  return result;
 }
 public static void logout(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent e){clearExertionTracking(e.getEntity());}
 public static void tick(PlayerTickEvent.Post e){
  Player p=e.getEntity();if(!active(p)||!(p.level() instanceof ServerLevel l))return;long now=l.getGameTime();
  if(now%20!=0)return;
  var tag=p.getPersistentData();
  updateExertion(p,l,now);
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
  if(now%80==0&&(Rules.on("injuries")&&tag.getLong(INJURY)>now||Rules.on("smoke_inhalation")&&tag.getInt(SMOKE)>=40)){
   String message=(Rules.on("injuries")&&tag.getLong(INJURY)>now?"Injured  ":"")+(Rules.on("smoke_inhalation")&&tag.getInt(SMOKE)>=40?"Smoke! Get fresh air":"");p.displayClientMessage(Component.literal(message.strip()),true);
  }
 }
}
