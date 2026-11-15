package dev.minemods.sleepdebt.entity;

import dev.minemods.sleepdebt.gameplay.SleepEngine;
import dev.minemods.sleepdebt.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.Mth;
import java.util.EnumSet;
import java.util.UUID;

/**
 * A dedicated server-authoritative nightmare hunter, not a Zombie AI clone.
 * No generic melee/target goals to take over player selection or navigation.
 * WATCH -> STALK -> FLANK -> PURSUE -> RUSH, with rare loaded-chunk relocation.
 *
 * Explicit debug spawn can pursue the invoker even in Creative, but attacks
 * remain Survival-only. Natural hunting respects debt, bed, grace and daylight.
 */
public final class SleeplessEntity extends Monster {
 private enum Mode { WATCH, STALK, FLANK, PURSUE, RUSH, VANISH }
 private Mode mode=Mode.WATCH;
 private int modeTicks=54, lookPause, lookCooldown;
 private int hitPause, hits, lastStrike=-120;
 private int teleportCooldown=90, stuckTicks;
 private boolean relocatedDuringVanish;
 private double lastDistance=999;
 private UUID debugTarget;

 public SleeplessEntity(EntityType<? extends Monster> type,Level level){super(type,level);}

 public static AttributeSupplier.Builder createAttributes(){
  return Monster.createMonsterAttributes()
   .add(Attributes.MAX_HEALTH,26)
   .add(Attributes.ATTACK_DAMAGE,4.2)
   .add(Attributes.MOVEMENT_SPEED,.30)
   .add(Attributes.FOLLOW_RANGE,56)
   .add(Attributes.ARMOR,2)
   .add(Attributes.KNOCKBACK_RESISTANCE,.22);
 }

 @Override protected void registerGoals(){
  goalSelector.addGoal(1,new FloatGoal(this));
  goalSelector.addGoal(2,new DreadPathGoal(this));
  // The rest of its movement/combat decisions are owned by the class.
 }

 public void beginDebugHunt(ServerPlayer player){
  debugTarget=player.getUUID();
  setTarget(player);
  mode=Mode.PURSUE;
  modeTicks=90;
  teleportCooldown=44;
  lookCooldown=110;
  getNavigation().stop();
 }

 public void beginNaturalHunt(ServerPlayer player){
  if(player.isCreative()||player.isSpectator()||
     SleepEngine.hasSleeplessGrace(player))return;
  setTarget(player);
  mode=Mode.WATCH;
  modeTicks=40+random.nextInt(36);
  teleportCooldown=75+random.nextInt(30);
 }

 private boolean isForcedTarget(ServerPlayer p){
  return debugTarget!=null && debugTarget.equals(p.getUUID());
 }
 private boolean eligible(ServerPlayer p){
  if(!p.isAlive()||p.isSpectator()||p.isSleeping()||
     p.level()!=level())return false;
  if(isForcedTarget(p))return true; // Debug deliberately bypasses grace.
  return !p.isCreative() && SleepEngine.getDebt(p)>=600 &&
     !SleepEngine.hasSleeplessGrace(p);
 }

 private void acquireTarget(){
  if(!(level() instanceof ServerLevel server))return;
  ServerPlayer best=null;
  double bestDist=56*56;
  for(ServerPlayer p:server.players()){
   if(!eligible(p))continue;
   double dist=distanceToSqr(p);
   if(dist<bestDist){best=p;bestDist=dist;}
  }
  if(best!=null){
   setTarget(best);
   if(modeTicks<=0){mode=Mode.STALK;modeTicks=65;}
  }
 }

 private void shiftMode(Mode next,int ticks){
  mode=next;
  modeTicks=ticks;
  getNavigation().stop();
 }

 public void forceVanish(ServerPlayer player){
  // A deterministic debug path to verify displacement even while in Creative.
  debugTarget=player.getUUID();
  setTarget(player);
  beginVanish();
 }

 private void beginVanish(){
  mode=Mode.VANISH;
  modeTicks=27;
  relocatedDuringVanish=false;
  setInvisible(false);
  teleportCooldown=dev.minemods.sleepdebt.SleepSettings.TELEPORT_INTERVAL_TICKS.get()
    +random.nextInt(49);
  getNavigation().stop();
 }

 private void advanceVanish(ServerPlayer target){
  // 27..19: retreat a few metres, deliberately letting the player hear/see it.
  if(modeTicks>18 && modeTicks%5==0){
   Vec3 away=position().subtract(target.position());
   if(away.horizontalDistanceSqr()>.1){
    Vec3 dir=new Vec3(away.x,0,away.z).normalize();
    getNavigation().moveTo(getX()+dir.x*4,getY(),getZ()+dir.z*4,.94);
   }
  }
  if(modeTicks==18){
   setInvisible(true);
   getNavigation().stop();
   if(level() instanceof ServerLevel server)
    server.sendParticles(ParticleTypes.LARGE_SMOKE,getX(),getY()+1.2,getZ(),
      38,.55,1.35,.55,.009);
  }
  if(modeTicks==9 && !relocatedDuringVanish){
   // Ahead is deliberately less common; the result is never an unavoidable
   // point-blank attack. Front 40%, behind 60%, retry the opposite arc.
   boolean front=random.nextFloat()<.40f;
   relocatedDuringVanish=relocateNear(target,front)
      ||relocateNear(target,!front);
   if(!relocatedDuringVanish){
    setInvisible(false);
    shiftMode(Mode.PURSUE,45);
    teleportCooldown=28;
    return;
   }
  }
  if(modeTicks<=3){
   setInvisible(false);
  }
  if(modeTicks<=0){
   setInvisible(false);
   shiftMode(Mode.RUSH,42+random.nextInt(39));
  }
 }

 @Override public boolean doHurtTarget(Entity victim){
  if(!(victim instanceof ServerPlayer player) || player.isCreative()||
     player.isSpectator() || (!isForcedTarget(player) &&
       SleepEngine.hasSleeplessGrace(player)))return false;
  if(tickCount-lastStrike<40 || hitPause>0)return false;
  boolean hit=super.doHurtTarget(victim);
  if(!hit)return false;
  hits++;
  lastStrike=tickCount;
  hitPause=18+random.nextInt(25);
  if(hits%3==0){
   player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,115,0,false,false,true));
   player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN,80,0,false,false,true));
   SleepEngine.addFatigue(player,65);
   level().playSound(null,blockPosition(),ModSounds.GROWL.get(),SoundSource.HOSTILE,1f,.66f);
  }else{
   player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,42,0,false,false,true));
   level().playSound(null,blockPosition(),ModSounds.STRIKE.get(),SoundSource.HOSTILE,.86f,.69f);
  }
  if(hits%4==0)
   level().playSound(null,blockPosition(),ModSounds.SCREECH.get(),
     SoundSource.HOSTILE,.65f,.74f+random.nextFloat()*.08f);
  SleepEngine.addStress(player,94);
  if(level() instanceof ServerLevel server)
   server.sendParticles(ParticleTypes.LARGE_SMOKE,player.getX(),player.getY()+1,
      player.getZ(),20,.7,.8,.7,.012);
  // The hunter withdraws, then comes back from a new angle.
  shiftMode(Mode.FLANK,30+random.nextInt(32));
  return true;
 }

 @Override protected SoundEvent getAmbientSound(){return ModSounds.BREATH.get();}
 @Override protected SoundEvent getHurtSound(DamageSource damage){return ModSounds.GROWL.get();}
 @Override protected SoundEvent getDeathSound(){return ModSounds.STRIKE.get();}
 @Override protected void playStepSound(BlockPos pos,BlockState state){}
 @Override public boolean shouldDespawnInPeaceful(){return true;}

 @Override public void tick(){
  super.tick();
  if(level().isClientSide){
   if(tickCount%4==0){
    for(int i=0;i<4;i++){
     double dx=(random.nextDouble()-.5)*1.5,dz=(random.nextDouble()-.5)*1.5;
     level().addParticle(ParticleTypes.LARGE_SMOKE,getX()+dx,
       getY()+random.nextDouble()*2.75,getZ()+dz,
       dx*.012,random.nextDouble()*.008,dz*.012);
    }
   }
   return;
  }
  if(level().isDay()){
   discard();return;
  }
  if(tickCount%20==0 && level().getMaxLocalRawBrightness(blockPosition())>=14){
   discard();return;
  }

  if(hitPause>0)hitPause--;
  if(lookPause>0)lookPause--;
  if(lookCooldown>0)lookCooldown--;
  if(teleportCooldown>0)teleportCooldown--;

  if(!(getTarget() instanceof ServerPlayer target) || !eligible(target)){
   setInvisible(false);
   setTarget(null);
   getNavigation().stop();
   if(tickCount%15==0)acquireTarget();
   return;
  }

  double distance=Math.sqrt(distanceToSqr(target));
  if(distance>64){
   setInvisible(false);
   setTarget(null);getNavigation().stop();return;
  }
  if(tickCount%7==0){
   getLookControl().setLookAt(target,35,35);
   Vec3 to=position().add(0,2.1,0).subtract(target.getEyePosition());
   boolean observed=to.lengthSqr()>0.01 && target.getLookAngle().normalize()
     .dot(to.normalize())>.975 && target.hasLineOfSight(this);
   // Never let staring refresh an infinite immobilization timer.
   if(observed&&distance>6.5&&distance<29&&lookCooldown==0){
    lookPause=13+random.nextInt(13);
    lookCooldown=110+random.nextInt(95);
    getNavigation().stop();
   }
  }
  if(modeTicks>0)modeTicks--;
  if(mode==Mode.VANISH){
   advanceVanish(target);
   return;
  }
  if(modeTicks<=0){
   switch(mode){
    case WATCH -> shiftMode(Mode.STALK,55+random.nextInt(52));
    case STALK -> shiftMode(Mode.FLANK,42+random.nextInt(48));
    case FLANK -> shiftMode(Mode.PURSUE,76+random.nextInt(36));
    case PURSUE -> shiftMode(Mode.RUSH,65+random.nextInt(42));
    case RUSH -> shiftMode(Mode.STALK,40+random.nextInt(55));
    case VANISH -> shiftMode(Mode.PURSUE,55);
   }
  }
  if(mode==Mode.WATCH && distance<14)shiftMode(Mode.FLANK,30);
  if(mode==Mode.STALK && distance<9)shiftMode(Mode.RUSH,88);
  if(distance>27 && mode==Mode.WATCH)shiftMode(Mode.STALK,75);

  if(distance>6 && tickCount%18==0){
   if(distance>lastDistance-.65)stuckTicks+=18;
   else stuckTicks=Math.max(0,stuckTicks-25);
   lastDistance=distance;
  }else if(distance<=6)stuckTicks=0;

  // This is now a deliberate behavioural phase, NOT a rare stuck-AI recovery.
  // Near the player, it withdraws into the same blackness covering the screen,
  // disappears and relocates ahead or behind after a brief suspense beat.
  if(teleportCooldown<=0 && distance>3 && distance<36 &&
     (mode==Mode.FLANK || mode==Mode.PURSUE || mode==Mode.RUSH
       || stuckTicks>65)){
   beginVanish();
   return;
  }

  if((mode==Mode.PURSUE||mode==Mode.RUSH) && distance<3.05 &&
     hitPause==0 && tickCount-lastStrike>=40){
   doHurtTarget(target);
  }
  if(distance<19 && tickCount%55==0)
   SleepEngine.addStress(target,15);
  if(distance<19 && getDeltaMovement().horizontalDistanceSqr()>.0008 &&
     tickCount%30==0 && random.nextInt(4)!=0){
   level().playSound(null,blockPosition(),ModSounds.SKITTER.get(),
    SoundSource.HOSTILE,.44f+random.nextFloat()*.19f,
    .72f+random.nextFloat()*.16f);
  }
 }


 /**
  * Reposition near the player's horizontal direction. Search nearby Y levels
  * rather than only the heightmap surface, so caves and uneven terrain work.
  * Reject unloaded chunks and any collision/fluid/headroom conflicts.
  */
 private boolean relocateNear(ServerPlayer target,boolean inFront){
  if(!(level() instanceof ServerLevel server))return false;
  Vec3 look=target.getLookAngle();
  double heading=Math.atan2(look.z,look.x)+(inFront?0:Math.PI);
  for(int i=0;i<34;i++){
   double arc=heading+(random.nextDouble()-.5)*(inFront?1.25:2.0);
   int radius=(inFront?8:7)+random.nextInt(inFront?7:8);
   int x=Mth.floor(target.getX()+Math.cos(arc)*radius);
   int z=Mth.floor(target.getZ()+Math.sin(arc)*radius);
   if(!server.getChunkSource().hasChunk(x>>4,z>>4))continue;
   int baseline=Mth.floor(target.getY());
   for(int delta:new int[]{0,-1,1,-2,2,-3,3,-4,4}){
    int y=baseline+delta;
    BlockPos foot=new BlockPos(x,y,z);
    if(!server.getBlockState(foot).isAir() ||
       !server.getBlockState(foot.above()).isAir() ||
       !server.getBlockState(foot.above(2)).isAir() ||
       server.getBlockState(foot.below()).getCollisionShape(server,foot.below()).isEmpty() ||
       !server.getFluidState(foot).isEmpty() ||
       !server.getFluidState(foot.below()).isEmpty())continue;
    // Actual 2.75-block entity collision, including modded half-block shapes.
    net.minecraft.world.phys.AABB destination=getBoundingBox()
      .move(x+.5-getX(),y-getY(),z+.5-getZ());
    if(!server.noCollision(this,destination))continue;
    server.sendParticles(ParticleTypes.LARGE_SMOKE,getX(),getY()+1.0,getZ(),
      13,.32,.85,.32,.008);
    teleportTo(x+.5,y,z+.5);
    server.sendParticles(ParticleTypes.LARGE_SMOKE,getX(),getY()+1.0,getZ(),
      27,.5,1.12,.5,.014);
    server.playSound(null,foot,ModSounds.WHISPER.get(),
      SoundSource.HOSTILE,.42f,.65f);
    stuckTicks=0;
    return true;
   }
  }
  return false;
 }

 private static final class DreadPathGoal extends Goal{
  private final SleeplessEntity hunter;
  private int repath;
  DreadPathGoal(SleeplessEntity hunter){
   this.hunter=hunter;
   setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));
  }
  @Override public boolean canUse(){
   return hunter.getTarget() instanceof ServerPlayer p && hunter.eligible(p);
  }
  @Override public boolean canContinueToUse(){return canUse();}
  @Override public void stop(){hunter.getNavigation().stop();}
  @Override public void tick(){
   if(!(hunter.getTarget() instanceof ServerPlayer target))return;
   hunter.getLookControl().setLookAt(target,38,38);
   if(hunter.lookPause>0 || hunter.hitPause>0 ||
      hunter.mode==Mode.WATCH || hunter.mode==Mode.VANISH){
    hunter.getNavigation().stop();return;
   }
   if(--repath>0)return;
   repath=6+hunter.random.nextInt(9);
   double speed=1.0;
   Vec3 waypoint=target.position();
   switch(hunter.mode){
    case STALK -> {
     speed=.71;
     Vec3 forward=target.getLookAngle();
     waypoint=target.position().subtract(forward.x*6,0,forward.z*6);
    }
    case FLANK -> {
     speed=1.08;
     Vec3 forward=target.getLookAngle();
     double sign=hunter.random.nextBoolean()?1:-1;
     waypoint=target.position().add(-forward.z*6*sign,0,forward.x*6*sign)
       .subtract(forward.x*2,0,forward.z*2);
    }
    case PURSUE -> speed=1.27;
    case RUSH -> speed=1.68;
    default -> speed=0;
   }
   hunter.getNavigation().moveTo(waypoint.x,waypoint.y,waypoint.z,speed);
  }
 }
}
