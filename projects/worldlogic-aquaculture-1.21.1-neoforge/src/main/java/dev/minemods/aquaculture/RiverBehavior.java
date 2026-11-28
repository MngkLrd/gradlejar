package dev.minemods.aquaculture;

import dev.minemods.worldlogic.api.WorldLogicApi;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import java.util.Set;
import java.util.Optional;

/** Active swimming intentions only. Core owns velocity and all passive advection. */
final class RiverBehavior {
 private RiverBehavior(){}
 private static final Set<String> FAST=Set.of("trout","grayling","char","taimen","whitefish");
 private static final Set<String> QUIET=Set.of("crucian_carp","carp","tench","rotan","rudd","loach","catfish","bowfin","piranha","pacu","tilapia","snakehead","arapaima","electric_eel","bream");
 static double tolerance(Species s){return FAST.contains(s.id())?3.8:QUIET.contains(s.id())?2.2:3.0;}
 static Optional<WorldLogicApi.RiverCurrent> sample(ServerLevel l,BlockPos p){
  return WorldLogicApi.riverCurrent(l,p);
 }
 static double weight(Species s,Optional<WorldLogicApi.RiverCurrent> flow){
  if(flow.isEmpty())return 1;
  double speed=flow.get().speedMps(),limit=tolerance(s);
  if(speed>limit+.35)return 0;
  if(FAST.contains(s.id()))return .55+speed/3.8;
  return Math.max(.15,1-.75*speed/limit);
 }
 static Vec3 intention(WorldLogicApi.RiverCurrent c,boolean downstream){
  return c.velocityMps().normalize().scale(downstream?1:-1);
 }
 static boolean step(ServerLevel l,Fish f,Species s){
  var opt=sample(l,f.blockPosition());if(opt.isEmpty())return false;
  var c=opt.get();Vec3 center=f.position().add(0,Math.min(.2,f.getBbHeight()*.5),0);
  Vec3 dir=c.velocityMps().normalize();
  if(c.speedMps()>tolerance(s)){
   Vec3 side=new Vec3(-dir.z,0,dir.x);Vec3 best=null;double score=c.speedMps();
   for(int sign:new int[]{-1,1})for(int distance:new int[]{2,4,6}){
    Vec3 dest=center.add(side.scale(sign*distance));BlockPos p=BlockPos.containing(dest);
    if(!FishEcology.clearWaterPath(l,center,dest))continue;
    var flow=sample(l,p);
    // Undirected water is unknown; allow a physical bank refuge but do not assign it zero flow.
    double speed=flow.map(WorldLogicApi.RiverCurrent::speedMps).orElse(c.speedMps());
    if(flow.isEmpty()&&CoastalHabitat.nearBank(l,p))speed=c.speedMps()-.15;
    if(speed<score){score=speed;best=dest;}
   }
   if(best!=null){f.setRiverIntent(best,.23);f.getPersistentData().putString("wl_fish_activity","current_shelter");return true;}
   Vec3 dest=center.add(dir.scale(3));
   if(FishEcology.clearWaterPath(l,center,dest)){f.setRiverIntent(dest,.14);f.getPersistentData().putString("wl_fish_activity","current_downstream");return true;}
   return false;
  }
  boolean downstream=FishFeeding.satiety(f,l.getGameTime())<25||Math.floorMod(l.getGameTime()/200+f.getId(),5)==0;
  Vec3 dest=center.add(intention(c,downstream).scale(3));
  if(!FishEcology.clearWaterPath(l,center,dest))return false;
  f.setRiverIntent(dest,downstream?.13:Math.min(.30,.16+c.speedMps()/20.0));
  f.getPersistentData().putString("wl_fish_activity",downstream?"current_downstream":"current_upstream");return true;
 }
}
