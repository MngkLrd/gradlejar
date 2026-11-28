package dev.minemods.aquaculture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Server-authoritative swimming, feeding, breathing and defensive encounters. */
public final class MarineAnimal extends WaterAnimal {
 private final MarineSpecies species;
 private long nextAttack,nextWander,chargeUntil;
 private Vec3 destination;
 private UUID chargingPlayer;
 private int breath=1200;
 private long fedUntil;
 public MarineAnimal(EntityType<? extends MarineAnimal> type,Level level,MarineSpecies species){
  super(type,level);this.species=species;setPersistenceRequired();
 }
 public MarineSpecies species(){return species;}
 public static AttributeSupplier.Builder attributes(MarineSpecies s){
  return Mob.createMobAttributes().add(Attributes.MAX_HEALTH,s.hp).add(Attributes.MOVEMENT_SPEED,.24).add(Attributes.ATTACK_DAMAGE,s.damage);
 }
 @Override protected void registerGoals(){}
 @Override public boolean isInWater(){return super.isInWater()||FishWater.contact(this);}
 @Override public boolean isInWaterOrBubble(){return super.isInWaterOrBubble()||FishWater.contact(this);}
 @Override public boolean canBreatheUnderwater(){return true;}
 @Override protected void handleAirSupply(int previous){
  if(!(level() instanceof ServerLevel l)||!isAlive())return;
  boolean wet=FishWater.contact(this);
  if(species.airBreather){
   BlockPos blowhole=BlockPos.containing(getX(),getY()+getBbHeight()*.92,getZ());
   if(!Aquaculture.water(l,blowhole))breath=1200;
   else if(--breath<=0&&tickCount%20==0)hurt(damageSources().drown(),2f);
  }else if(wet)breath=300;
  else if(--breath<=0&&tickCount%20==0)hurt(damageSources().drown(),2f);
  setAirSupply(Math.max(0,Math.min(getMaxAirSupply(),breath)));
 }
 @Override public void travel(Vec3 input){
  if(isInWater()){
   move(MoverType.SELF,getDeltaMovement());setDeltaMovement(getDeltaMovement().scale(.89));
  }else super.travel(input);
 }
 @Override public void addAdditionalSaveData(CompoundTag n){
  super.addAdditionalSaveData(n);n.putInt("marine_breath",breath);n.putLong("marine_fed",fedUntil);n.putLong("marine_attack",nextAttack);
 }
 @Override public void readAdditionalSaveData(CompoundTag n){
  super.readAdditionalSaveData(n);if(n.contains("marine_breath"))breath=n.getInt("marine_breath");fedUntil=n.getLong("marine_fed");nextAttack=n.getLong("marine_attack");
 }
 @Override public void tick(){
  super.tick();
  if(!(level() instanceof ServerLevel l)||!isAlive()||(tickCount+getId())%10!=0)return;
  boolean wet=FishWater.contact(this);setNoGravity(wet);
  if(!wet)return;
  long now=l.getGameTime();
  if(species.airBreather&&breath<500){surface(l);return;}
  if(species==MarineSpecies.ELECTRIC_RAY){electric(l,now);}
  if(species.diet.equals("hunter")||species.diet.equals("electric")){
   if(now>=fedUntil&&hunt(l,now))return;
   if((species.shark()||species==MarineSpecies.SEA_SNAKE)&&defend(l,now))return;
  }else if(now>=fedUntil){
   var h=Aquaculture.habitat(l,blockPosition());
   if(h!=null&&AquaEcologyData.get(l).takeFood(blockPosition(),h.kind(),AquaEcologyData.Food.PLANKTON,now)){
    fedUntil=now+1200;l.sendParticles(ParticleTypes.BUBBLE,getX(),getY()+.2,getZ(),5,.3,.2,.3,.03);
   }
  }
  if(destination==null||now>=nextWander||position().distanceToSqr(destination)<2){
   nextWander=now+80+l.random.nextInt(100);destination=null;
   for(int i=0;i<8;i++){
    Vec3 p=position().add(l.random.nextInt(17)-8,l.random.nextInt(7)-3,l.random.nextInt(17)-8);
    if(species.ray()&&species!=MarineSpecies.MANTA_RAY)p=p.add(0,-1,0);
    if(route(l,p)){destination=p;break;}
   }
  }
  if(destination!=null)steer(l,destination,species.whale()?.20:.16);
 }
 private Vec3 center(){return position().add(0,getBbHeight()*.4,0);}
 private boolean route(ServerLevel l,Vec3 target){
  Vec3 here=center();double length=here.distanceTo(target);if(length>18||!Double.isFinite(length))return false;
  int n=Math.max(1,(int)Math.ceil(length/1.5));
  for(int i=0;i<=n;i++){
   Vec3 p=here.lerp(target,i/(double)n);BlockPos b=BlockPos.containing(p);
   if(!Aquaculture.water(l,b)||!l.noCollision(this,getBoundingBox().move(p.subtract(here))))return false;
  }
  return true;
 }
 private void steer(ServerLevel l,Vec3 target,double speed){
  Vec3 v=target.subtract(center());if(v.lengthSqr()<.08)return;
  Vec3 next=v.normalize().scale(speed);
  BlockPos p=BlockPos.containing(center().add(next));
  if(!Aquaculture.water(l,p)||!l.noCollision(this,getBoundingBox().move(next)))return;
  setDeltaMovement(getDeltaMovement().scale(.35).add(next.scale(.65)));
  float yaw=(float)(Math.atan2(-next.x,next.z)*180/Math.PI);setYRot(yaw);yBodyRot=yaw;yHeadRot=yaw;
 }
 private void surface(ServerLevel l){
  BlockPos p=blockPosition();int n=0;while(n<18&&Aquaculture.water(l,p.above(n+1)))n++;
  Vec3 target=new Vec3(getX(),p.getY()+n+.65,getZ());steer(l,target,.25);
  if(n<=1)l.sendParticles(ParticleTypes.BUBBLE,getX(),getY()+getBbHeight(),getZ(),6,.2,.2,.2,.02);
 }
 private boolean hunt(ServerLevel l,long now){
  Fish prey=null;double best=144;
  for(Fish f:l.getEntitiesOfClass(Fish.class,getBoundingBox().inflate(12))){
   Species s=Aquaculture.species(f);if(s==null||!f.isAlive()||f.visualLength(s)>species.length*.35)continue;
   double d=distanceToSqr(f);if(d<best&&route(l,f.position().add(0,.15,0))){best=d;prey=f;}
  }
  if(prey==null)return false;
  if(best<Math.pow(Math.max(1,species.width*.7),2)&&now>=nextAttack){
   nextAttack=now+70;
   if(prey.hurt(damageSources().mobAttack(this),Math.max(3,species.damage))&&!prey.isAlive())fedUntil=now+2400;
  }else steer(l,prey.position().add(0,.15,0),.24);
  return true;
 }
 private boolean defend(ServerLevel l,long now){
  if(now<nextAttack)return false;
  double radius=species.shark()?2.5:1.0;
  for(ServerPlayer p:l.getEntitiesOfClass(ServerPlayer.class,getBoundingBox().inflate(radius))){
   if(p.isCreative()||p.isSpectator()||!p.isAlive()||!FishWater.contact(p))continue;
   // Sea snakes only strike in close self-defense after being injured. Sharks react to close injured/splashing prey.
   if(species==MarineSpecies.SEA_SNAKE&&getLastHurtByMob()!=p)continue;
   if(species.shark()&&getLastHurtByMob()!=p&&p.getHealth()>p.getMaxHealth()*.5&&p.getDeltaMovement().horizontalDistance()<.16)continue;
   if(distanceToSqr(p)>radius*radius||!route(l,p.position().add(0,.4,0)))continue;
   if(p.hurt(damageSources().mobAttack(this),species.damage)){
    nextAttack=now+100;if(species==MarineSpecies.SEA_SNAKE)p.addEffect(new MobEffectInstance(MobEffects.POISON,100,0));return true;
   }
  }
  return false;
 }
 private void electric(ServerLevel l,long now){
  if(chargingPlayer!=null){
   var e=l.getEntity(chargingPlayer);
   if(!(e instanceof ServerPlayer p)||!p.isAlive()||!FishWater.contact(p)||distanceToSqr(p)>4||!route(l,p.position().add(0,.2,0))){chargingPlayer=null;return;}
   l.sendParticles(ParticleTypes.ELECTRIC_SPARK,getX(),getY()+.25,getZ(),4,.35,.1,.35,.03);
   if(now>=chargeUntil){MarineTrauma.shock(p,this);chargingPlayer=null;nextAttack=now+200;}
   return;
  }
  if(now<nextAttack)return;
  for(ServerPlayer p:l.getEntitiesOfClass(ServerPlayer.class,getBoundingBox().inflate(1.3))){
   if(!p.isCreative()&&!p.isSpectator()&&p.isAlive()&&distanceToSqr(p)<2.25&&FishWater.contact(p)&&route(l,p.position().add(0,.2,0))){
    chargingPlayer=p.getUUID();chargeUntil=now+20;break;
   }
  }
 }
}
