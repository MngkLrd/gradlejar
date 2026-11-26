package dev.minemods.worldlogic;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
public final class Balance {
 private static final class State {
  Vec3 position,velocity=Vec3.ZERO,fallDirection=Vec3.ZERO;
  long last,downUntil,cooldown,jumpAt=-100,failedUntil,started;
  boolean sprint,ground,owned;
  float roll;
  double jumpY,jumpSpeed;
 }
 private static final Map<Player,State> STATES=new WeakHashMap<>();
 private static State state(Player p){return STATES.computeIfAbsent(p,k->new State());}
 static boolean eligible(Player p){return Survival.active(p)&&p.isAlive()&&!p.isSleeping()&&!p.isPassenger()&&!p.isFallFlying()&&!p.isInWater()&&!p.isInLava()&&!p.onClimbable()&&!p.getAbilities().flying;}
 public static boolean down(Player p){State s=STATES.get(p);return Rules.on("physical_falls")&&s!=null&&s.owned&&eligible(p)&&s.downUntil>p.level().getGameTime();}
 public static boolean canCrawl(Player p){State s=STATES.get(p);return Rules.on("desperate_recovery")&&down(p)&&s!=null&&p.level().getGameTime()-s.started>=8;}
 static Vec3 fallDirection(Player p){State s=STATES.get(p);return s==null?Vec3.ZERO:s.fallDirection;}
 static Vec3 recentMotion(Player p){State s=STATES.get(p);return s==null?Vec3.ZERO:s.velocity;}
 public static float roll(Player p){State s=STATES.get(p);return s==null?0:s.roll;}
 static double threshold(boolean injured,double fatigue,double burden){return Math.max(.15,.265-(injured?.055:0)-Math.max(0,fatigue-55)*.0008-Math.min(.035,burden*.008));}
 static double slipRisk(boolean injured,double fatigue,double burden,double weakness){return Math.max(0,Math.min(1,(injured?.65:0)+Math.max(0,fatigue-55)*.007+Math.min(.22,burden*.05)+weakness*.4));}
 static int duration(double severity,boolean injured){return (int)Math.max(16,Math.min(46,18+severity*15+(injured?8:0)));}
 static boolean blocked(ServerLevel l,Player p,AABB box,Vec3 direction,double lift){
  return l.getBlockCollisions(p,box.deflate(.001).move(direction.x,lift,direction.z)).iterator().hasNext();
 }
 static boolean dark(ServerLevel l,Player p){return Rules.on("dangerous_darkness")&&!p.hasEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION)&&l.getMaxLocalRawBrightness(p.blockPosition())<=3;}
 static boolean slick(ServerLevel l,Player p){
  BlockPos pos=p.getOnPos();var b=l.getBlockState(pos);
  return b.is(Blocks.ICE)||b.is(Blocks.PACKED_ICE)||b.is(Blocks.BLUE_ICE)||b.is(Blocks.FROSTED_ICE)||Rules.on("slippery_rain")&&Material.block(b,"slippery")&&l.isRainingAt(pos.above());
 }
 private static void fall(ServerLevel l,Player p,State s,Vec3 dir,String reason,double severity,boolean injured){
  if(s.owned||p.getForcedPose()!=null||!eligible(p))return;
  long now=l.getGameTime();s.started=now;s.downUntil=now+duration(severity,injured)+(Rules.on("ground_recovery")?GroundHazards.recoveryExtra(p):0);s.cooldown=s.downUntil+60;s.owned=true;
  s.fallDirection=dir.multiply(1,0,1).normalize();s.roll=dir.cross(p.getLookAngle()).y>=0?18:-18;
  p.stopUsingItem();p.setSprinting(false);p.setForcedPose(Pose.SWIMMING);p.setPose(Pose.SWIMMING);
  Vec3 v=dir.multiply(1,0,1).normalize().scale(.12);
  p.setDeltaMovement(v.x,Math.min(0,p.getDeltaMovement().y),v.z);p.hurtMarked=true;
  Survival.work(p,5+severity*3);
  GroundHazards.dropOnImpact(p,severity,reason);
  Trauma.impact(p,severity,reason);
  if(severity>=.75&&Rules.on("wall_collision")&&reason.equals("collision"))p.hurt(l.damageSources().generic(),(float)Math.min(3,1+severity));
  BlockPos ground=p.getOnPos();var floor=l.getBlockState(ground);
  if(!floor.isAir())l.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK,floor),p.getX(),p.getY()+.08,p.getZ(),12,.25,.04,.25,.035);
  l.playSound(null,p.blockPosition(),SoundEvents.PLAYER_SMALL_FALL,SoundSource.PLAYERS,.8f,.8f);
  p.displayClientMessage(Component.translatable("message.worldlogic.fall_"+reason),true);
  if(p instanceof ServerPlayer sp)RuleSync.send(sp);
 }
 private static void release(Player p,State s){
  if(s.owned){if(p.getForcedPose()==Pose.SWIMMING)p.setForcedPose(null);s.owned=false;s.fallDirection=Vec3.ZERO;s.downUntil=0;s.failedUntil=0;s.velocity=Vec3.ZERO;if(p instanceof ServerPlayer sp)RuleSync.send(sp);}
 }
 public static void tick(TickEvent.PlayerTickEvent.Post e){
  Player p=e.player;if(!(p.level() instanceof ServerLevel l))return;State s=state(p);long now=l.getGameTime();
  if(!eligible(p)||!Rules.on("physical_falls")){release(p,s);s.position=null;return;}
  if(s.owned){
   if(now>=s.downUntil||p.getForcedPose()!=Pose.SWIMMING){release(p,s);}
   else{p.setSprinting(false);if(p.isUsingItem())p.stopUsingItem();Vec3 v=p.getDeltaMovement();double damping=canCrawl(p)?.9:.65;p.setDeltaMovement(v.x*damping,v.y,v.z*damping);ImpactDynamics.rollDown(l,p);if(now-s.started==8&&p instanceof ServerPlayer sp)RuleSync.send(sp);}
   s.position=p.position();s.last=now;s.ground=p.onGround();s.sprint=false;return;
  }
  if(s.position==null||s.last!=now-1||p.position().distanceToSqr(s.position)>4){
   s.position=p.position();s.velocity=Vec3.ZERO;s.last=now;s.ground=p.onGround();s.sprint=p.isSprinting();s.failedUntil=0;s.jumpAt=-100;return;
  }
  Vec3 delta=p.position().subtract(s.position),previous=s.velocity;double speed=previous.horizontalDistance(),current=delta.horizontalDistance();
  boolean injured=Rules.on("injured_balance")&&Survival.injured(p,now);
  double fatigue=Rules.on("exertion")?Survival.fatigue(p):0,burden=Condition.burden(p),weak=Condition.weakness(p);
  double min=Math.max(.13,threshold(injured,fatigue,burden)-Trauma.balancePenalty(p));if(dark(l,p))min=Math.max(.13,min*.85);
  if(s.ground&&!p.onGround()&&delta.y>.075){s.jumpAt=now;s.jumpY=s.position.y;s.jumpSpeed=Math.max(speed,current);s.failedUntil=0;}
  boolean airborneJump=!p.onGround()&&now-s.jumpAt<=20;
  if(airborneJump)s.jumpSpeed=Math.max(s.jumpSpeed,speed);
  if(now>=s.cooldown&&!p.isShiftKeyDown()&&p.getForcedPose()==null&&(speed>=min&&(s.sprint||p.isSprinting())||airborneJump&&speed>=.095&&s.jumpSpeed>=.14)){
   Vec3 forward=previous.multiply(1,0,1).normalize();
   AABB box=p.getBoundingBox();
   // Test each axis independently so a glancing shoulder collision is detected too.
   boolean x=Math.abs(previous.x)>.095&&Math.abs(delta.x)<Math.abs(previous.x)*.40&&blocked(l,p,box,new Vec3(Math.copySign(.13,previous.x),0,0),0);
   boolean z=Math.abs(previous.z)>.095&&Math.abs(delta.z)<Math.abs(previous.z)*.40&&blocked(l,p,box,new Vec3(0,0,Math.copySign(.13,previous.z)),0);
   boolean front=current<speed*.5&&blocked(l,p,box,forward.scale(.18),0);
   boolean obstacle=x||z||front;
   boolean step=obstacle&&!blocked(l,p,box,forward.scale(.18),.62);
   if(obstacle&&airborneJump){s.failedUntil=now+16;}
   else if(obstacle&&!step){
    if(p.onGround()&&s.ground&&s.sprint&&delta.y<.10){
     fall(l,p,s,forward,"collision",Math.min(1.4,speed/min*.55),injured);
    }
   }
   if(!s.owned&&p.onGround()&&s.ground&&current>.12&&slick(l,p)){
    double angle=forward.dot(delta.multiply(1,0,1).normalize());
    double risk=slipRisk(injured,fatigue,burden,weak);
    if(angle<.65&&risk>=.45&&speed>=min*.95)fall(l,p,s,forward,"slip",risk,injured);
   }
  }
  if(!s.owned&&now>=s.cooldown&&p.onGround()&&s.ground&&s.sprint&&!p.isShiftKeyDown()&&Rules.on("unstable_footing")&&speed>=min*.95&&ImpactDynamics.edgeRisk(l,p,previous,delta,slipRisk(injured,fatigue,burden,weak)))fall(l,p,s,previous,"edge",.65,injured);
  if(!s.owned&&now>=s.cooldown&&p.onGround()&&s.ground&&s.sprint&&!p.isShiftKeyDown()&&Rules.on("rough_terrain")&&speed>=min*.9){
   var floor=l.getBlockState(p.getOnPos());
   double rise=Math.abs(delta.y);boolean rough=Material.block(floor,"rough_terrain");
   double risk=slipRisk(injured,fatigue,burden,weak)+(Rules.on("dangerous_darkness")&&l.getMaxLocalRawBrightness(p.blockPosition())<=3?.20:0);
   // No random falls on flat terrain: require a sudden vertical step or drop and measurable loss of speed.
   if(rough&&rise>=.23&&rise<=.65&&current<speed*.72&&risk>=.35)fall(l,p,s,previous,"rough",Math.min(1.1,risk),injured);
  }
  if(!s.owned&&p.onGround()&&!s.ground&&s.failedUntil>0&&s.failedUntil>=now&&now>=s.cooldown&&p.getY()<s.jumpY+.50&&!p.isShiftKeyDown()){
   Vec3 dir=previous.horizontalDistance()>.04?previous:p.getLookAngle();fall(l,p,s,dir,"jump",.6,injured);
  }
  if(p.onGround()&&!s.ground)s.failedUntil=0;
  s.velocity=s.owned?Vec3.ZERO:delta;s.position=p.position();s.last=now;s.ground=p.onGround();s.sprint=p.isSprinting();
 }
 static boolean forceLanding(Player p,double severity){return force(p,p.getDeltaMovement(),"landing",severity);}
 static boolean force(Player p,Vec3 direction,String reason,double severity){
  if(!eligible(p)||!Rules.on("physical_falls")||p.getForcedPose()!=null)return false;
  State s=state(p);if(s.owned||s.cooldown>p.level().getGameTime())return false;
  if(direction.horizontalDistance()<.05)direction=p.getLookAngle();
  fall((ServerLevel)p.level(),p,s,direction,reason,severity,Survival.injured(p,p.level().getGameTime()));return s.owned;
 }
 public static void damaged(net.minecraftforge.event.entity.living.LivingDamageEvent e){
  if(e.isCanceled()||e.getAmount()<2||!(e.getEntity() instanceof Player p)||!Rules.on("ground_recovery")||!down(p))return;
  // Impacts from the fall itself cannot extend the initial prone period.
  if(e.getSource().getEntity()==null)return;
  State s=state(p);long now=p.level().getGameTime();s.downUntil=Math.min(s.started+75,Math.max(s.downUntil,now+12));s.cooldown=s.downUntil+60;
 }
 public static void jump(LivingEvent.LivingJumpEvent e){if(e.getEntity() instanceof Player p&&down(p)){Vec3 v=p.getDeltaMovement();p.setDeltaMovement(v.x,0,v.z);p.hurtMarked=true;}}
 public static void attack(AttackEntityEvent e){if(down(e.getEntity()))e.setCanceled(true);}
 public static void interact(PlayerInteractEvent e){if(e.isCancelable()&&down(e.getEntity()))e.setCanceled(true);}
 public static void use(LivingEntityUseItemEvent.Start e){if(e.getEntity() instanceof Player p&&down(p))e.setCanceled(true);}
 public static void breakSpeed(PlayerEvent.BreakSpeed e){if(down(e.getEntity()))e.setNewSpeed(0);}
 public static void logout(PlayerEvent.PlayerLoggedOutEvent e){State s=STATES.remove(e.getEntity());if(s!=null&&s.owned&&e.getEntity().getForcedPose()==Pose.SWIMMING)e.getEntity().setForcedPose(null);}
 public static boolean debug(Player p){if(!eligible(p)||!Rules.on("physical_falls")||p.getForcedPose()!=null)return false;State s=state(p);fall((ServerLevel)p.level(),p,s,p.getLookAngle(),"debug",.5,Survival.injured(p,p.level().getGameTime()));return s.owned;}
}
