package dev.minemods.aquaculture;

import dev.minemods.worldlogic.api.WorldLogicApi;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.particles.ParticleTypes;
import java.util.*;

/** Bounded server-side behavior; does not replace Core fluids or spawn a virtual catch. */
public final class FishEcology {
 private FishEcology(){}
 private static final Set<String> SCHOOL=Set.of("perch","roach","rudd","bream","crucian_carp","carp","grayling","whitefish","herring","mackerel","atlantic_cod","haddock","tilapia","piranha","pacu","tuna","snapper","mahi_mahi");
 private static final Set<String> BOTTOM=Set.of("burbot","catfish","tench","loach","rotan","halibut","flounder","haddock","grouper","electric_eel");
 private static final Set<String> SURFACE=Set.of("herring","tuna","mackerel","mahi_mahi","arapaima","rudd","whitefish");
 public static boolean schooling(Species s){return SCHOOL.contains(s.id());}
 public static boolean bottom(Species s){return BOTTOM.contains(s.id());}
 public static boolean canHunt(Fish predator,Species hunter,Fish prey,Species victim){
  return hunter!=null&&victim!=null&&predator!=prey&&prey.isAlive()&&hunter!=victim
   &&!predator.getPersistentData().getBoolean("wl_juvenile")&&hunter.diet().equals("predator")&&predator.visualLength(hunter)>=prey.visualLength(victim)*1.8f
   &&prey.visualLength(victim)<=.70f;
 }
 public static double habitatStress(Species s,Aquaculture.Habitat h){
  if(h==null)return 1;
  double t=Math.max(0,Math.max(s.minTemp()-h.temp(),h.temp()-s.maxTemp()))/10;
  double salt=Math.max(0,Math.max(s.minSalt()-h.salt(),h.salt()-s.maxSalt()))/8;
  double risk=Math.max(0,h.risk()-s.maxRisk(h.kind()))*3;
  return Math.min(1,t+salt+risk+(s.suitable(h.kind(),h.temp(),h.salt(),h.risk(),h.depth())?0:.18));
 }
 public static boolean clearWaterPath(ServerLevel l,Vec3 from,Vec3 to){
  double length=from.distanceTo(to);if(length>12||!Double.isFinite(length))return false;
  int n=Math.max(1,(int)Math.ceil(length*3));
  for(int i=0;i<=n;i++){
   BlockPos p=BlockPos.containing(from.lerp(to,i/(double)n));
   if(!Aquaculture.water(l,p)||!l.getBlockState(p).getCollisionShape(l,p).isEmpty())return false;
  }
  return true;
 }
 private static Vec3 center(Fish f){return f.position().add(0,Math.min(.2,f.getBbHeight()*.5),0);}
 private static boolean reachable(ServerLevel l,Fish fish,Vec3 target){return clearWaterPath(l,center(fish),target);}
 static void steer(ServerLevel l,Fish fish,Vec3 target,double speed){
  Vec3 here=center(fish),dir=target.subtract(here);
  if(dir.lengthSqr()<.04||!clearWaterPath(l,here,target))return;
  Vec3 next=fish.getDeltaMovement().scale(.65).add(dir.normalize().scale(speed*.35));
  if(next.length()>speed)next=next.normalize().scale(speed);
  if(!Aquaculture.water(l,BlockPos.containing(here.add(next))))return;
  if(l.noCollision(fish,fish.getBoundingBox().move(next)))fish.setDeltaMovement(next);
 }
 public static boolean feedDropped(Fish fish,Species s,ItemEntity item){
  if(!(fish.level() instanceof ServerLevel l)||!item.isAlive()||item.level()!=l||fish.distanceToSqr(item)>1.44
   ||!reachable(l,fish,item.position()))return false;
  var stack=item.getItem().copy();
  if(FishFeeding.offer(fish,s,stack,false)!=FishFeeding.Result.FED)return false;
  if(stack.isEmpty())item.discard();else item.setItem(stack);
  return true;
 }
 public static boolean hasPlants(ServerLevel l,BlockPos pos){
  for(Direction d:Direction.values()){
   BlockPos q=pos.relative(d);if(!l.hasChunkAt(q)||l.isOutsideBuildHeight(q))continue;
   Block b=l.getBlockState(q).getBlock();
   if(b instanceof KelpBlock||b instanceof KelpPlantBlock||b instanceof SeagrassBlock||b instanceof TallSeagrassBlock)return true;
  }
  Block b=l.getBlockState(pos).getBlock();
  return b instanceof KelpBlock||b instanceof KelpPlantBlock||b instanceof SeagrassBlock||b instanceof TallSeagrassBlock;
 }
 private static boolean nearBed(ServerLevel l,BlockPos p){
  for(int i=1;i<=2;i++){BlockPos q=p.below(i);if(l.hasChunkAt(q)&&!l.isOutsideBuildHeight(q)&&l.getBlockState(q).isCollisionShapeFullBlock(l,q))return true;}
  return false;
 }
 static AquaEcologyData.Food localFood(ServerLevel l,BlockPos p,Species s){
  return switch(s.diet()){
   case "grazer"->hasPlants(l,p)?AquaEcologyData.Food.PLANTS:null;
   case "plankton"->!Aquaculture.water(l,p.above(3))?AquaEcologyData.Food.PLANKTON:null;
   case "invertebrate"->nearBed(l,p)?AquaEcologyData.Food.BENTHOS:null;
   default->null;
  };
 }
 static boolean forage(ServerLevel l,Fish f,Species s,AquaEcologyData data,WorldLogicApi.WaterKind kind){
  long now=l.getGameTime();if(!FishFeeding.canFeed(f,now)||FishFeeding.satiety(f,now)>=40)return false;
  // Fry use microscopic prey rather than trying to swallow adult fish.
  var food=f.getPersistentData().getBoolean("wl_juvenile")?
   (nearBed(l,f.blockPosition())?AquaEcologyData.Food.BENTHOS:AquaEcologyData.Food.PLANKTON):localFood(l,f.blockPosition(),s);
  if(food==null||!data.takeFood(f.blockPosition(),kind,food,now))return false;
  FishFeeding.feedNatural(f,s,now);return true;
 }
 static Vec3 schoolTarget(Fish fish,Species s,List<Fish> neighbors){
  if(!schooling(s))return null;
  Fish leader=null;double best=64;
  for(Fish other:neighbors){
   if(other==fish||!other.isAlive()||other.getId()>=fish.getId()||Aquaculture.species(other)!=s)continue;
   double d=fish.distanceToSqr(other);if(d<best&&d>1.0){leader=other;best=d;}
  }
  return leader==null?null:center(leader);
 }
 private static boolean flee(ServerLevel l,Fish f,Vec3 danger){
  Vec3 here=center(f),away=here.subtract(danger);
  if(away.lengthSqr()<.001)away=new Vec3(1,0,0);
  Vec3 horizontal=new Vec3(away.x,0,away.z).normalize();
  for(Vec3 v:List.of(horizontal.scale(4).add(0,-.7,0),horizontal.scale(3),horizontal.scale(2).add(0,1,0),new Vec3(-horizontal.z,0,horizontal.x).scale(3),new Vec3(horizontal.z,0,-horizontal.x).scale(3))){
   Vec3 target=here.add(v);if(reachable(l,f,target)){steer(l,f,target,.26);return true;}
  }
  return false;
 }
 private static Vec3 refuge(ServerLevel l,Fish f,Species s){
  BlockPos p=f.blockPosition();Vec3 best=null;double score=Double.MAX_VALUE;
  for(int i=0;i<5;i++){
   BlockPos q=p.offset(f.getRandom().nextInt(9)-4,f.getRandom().nextInt(7)-3,f.getRandom().nextInt(9)-4);
   Vec3 target=Vec3.atCenterOf(q);
   if(!reachable(l,f,target))continue;
   var h=Aquaculture.habitat(l,q);if(h==null)continue;
   double v=habitatStress(s,h);
   if(v<score){score=v;best=target;}
  }
  return best;
 }
 public static void step(ServerLevel l,Fish f,Species s){
  if(!f.isAlive()||!FishWater.contact(f)){f.setEcologyPanic(false);return;}
  var tag=Aquaculture.traits(f,s);long now=l.getGameTime();
  boolean sample=(f.tickCount+f.getId())%100<10||!tag.contains("wl_fish_stress");
  if(sample){
   var h=Aquaculture.habitat(l,f.blockPosition());
   double target=habitatStress(s,h),old=tag.getDouble("wl_fish_stress");
   tag.putDouble("wl_fish_stress",Math.max(0,Math.min(1,old+(target-old)*.25)));
  }
  List<Fish> neighbors=l.getEntitiesOfClass(Fish.class,f.getBoundingBox().inflate(8));
  Vec3 danger=null;double dangerDistance=Double.MAX_VALUE;
  for(ServerPlayer player:l.getEntitiesOfClass(ServerPlayer.class,f.getBoundingBox().inflate(7))){
   if(player.isSpectator()||player.isCreative())continue;
   double d=f.distanceToSqr(player);
   boolean noisy=player.isSprinting()||player.getDeltaMovement().horizontalDistance()>.13||player.swinging;
   if(d<(noisy?36:4)&&d<dangerDistance){danger=player.position();dangerDistance=d;}
  }
  for(Fish other:neighbors){
   if(canHunt(other,Aquaculture.species(other),f,s)){
    double d=f.distanceToSqr(other);
    if(d<dangerDistance&&d<25&&reachable(l,f,center(other))){danger=center(other);dangerDistance=d;}
   }
  }
  for(MarineAnimal predator:l.getEntitiesOfClass(MarineAnimal.class,f.getBoundingBox().inflate(9))){
   if(!predator.isAlive()||!predator.species().diet.equals("hunter"))continue;
   if(f.visualLength(s)>predator.species().length*.35)continue;
   double d=f.distanceToSqr(predator);
   if(d<dangerDistance&&reachable(l,f,predator.position().add(0,.3,0))){danger=predator.position();dangerDistance=d;}
  }
  if(danger!=null){
   f.setEcologyPanic(true);tag.putLong("wl_fish_panic_until",now+60);flee(l,f,danger);tag.putString("wl_fish_activity","fleeing");return;
  }
  f.setEcologyPanic(tag.getLong("wl_fish_panic_until")>now);
  if(f.ecologyPanic())return;
  if(tag.getDouble("wl_fish_stress")>.35&&sample){
   Vec3 target=refuge(l,f,s);if(target!=null){steer(l,f,target,.16);tag.putString("wl_fish_activity","seeking_refuge");return;}
  }
  if(FishFeeding.canFeed(f,now)&&FishFeeding.satiety(f,now)<65){
   ItemEntity bait=null;double closest=36;
   for(ItemEntity item:l.getEntitiesOfClass(ItemEntity.class,f.getBoundingBox().inflate(6))){
    double d=f.distanceToSqr(item);
    if(d<closest&&item.isAlive()&&item.getItem().is(FishFeeding.foodTag(s))&&reachable(l,f,item.position())){bait=item;closest=d;}
   }
   if(bait!=null){
    tag.putString("wl_fish_activity","feeding");
    if(feedDropped(f,s,bait))l.sendParticles(ParticleTypes.BUBBLE,f.getX(),f.getY()+.1,f.getZ(),3,.1,.1,.1,.02);
    else steer(l,f,bait.position(),.15);
    return;
   }
   Fish prey=null;double distance=49;
   for(Fish other:neighbors){
    double d=f.distanceToSqr(other);
    if(d<distance&&canHunt(f,s,other,Aquaculture.species(other))&&reachable(l,f,center(other))){prey=other;distance=d;}
   }
   if(prey!=null){
    tag.putString("wl_fish_activity","hunting");
    if(distance<=.9&&now>=tag.getLong("wl_fish_next_strike")){
     tag.putLong("wl_fish_next_strike",now+100);
     if(prey.hurt(l.damageSources().mobAttack(f),3f)&&!prey.isAlive())FishFeeding.feedNatural(f,s,now);
    }else steer(l,f,center(prey),.22);
    return;
   }
   if(sample){
    var profile=WorldLogicApi.waterProfile(l,f.blockPosition());
    if(forage(l,f,s,AquaEcologyData.get(l),profile.kind())){
     tag.putString("wl_fish_activity","foraging");return;
    }
   }
  }
  if(sample&&FishLife.courtship(l,f,s)){tag.putString("wl_fish_activity","courtship");return;}
  Vec3 school=schoolTarget(f,s,neighbors);
  if(school!=null&&reachable(l,f,school)){steer(l,f,school,.12);tag.putString("wl_fish_activity","schooling");return;}
  if(sample){
   BlockPos p=f.blockPosition();
   if(bottom(s)&&!nearBed(l,p)&&Aquaculture.water(l,p.below())){
    steer(l,f,center(f).add(0,-1,0),.09);tag.putString("wl_fish_activity","bottom");return;
   }
   if(SURFACE.contains(s.id())&&Aquaculture.water(l,p.above(3))){
    steer(l,f,center(f).add(0,1,0),.09);tag.putString("wl_fish_activity","surface");return;
   }
  }
  tag.putString("wl_fish_activity","swimming");
 }
}
