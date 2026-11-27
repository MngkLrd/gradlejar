package dev.minemods.thermal.server;
import dev.minemods.thermal.Adaptation;
import dev.minemods.worldlogic.api.WorldLogicApi;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import java.util.*;
/** Bounded, shared thermal mass approximation. Never writes core air or loads chunks. */
final class RoomHeat {
 private record State(long tick,double delta){}
 private static final Map<ServerLevel,LinkedHashMap<BlockPos,State>> ROOMS=new WeakHashMap<>();
 private RoomHeat(){}
 static double sample(ServerLevel l,BlockPos start,double source,double cooling,boolean covered){
  return sampleAt(l,start,source,cooling,covered,l.getGameTime());
 }
 static double sampleAt(ServerLevel l,BlockPos start,double source,double cooling,boolean covered,long now){
  if(!covered||!l.hasChunkAt(start))return 0;
  Set<BlockPos> seen=new HashSet<>();ArrayDeque<BlockPos> queue=new ArrayDeque<>();seen.add(start.immutable());queue.add(start.immutable());
  BlockPos key=start;double mass=0;int boundaries=0;
  while(!queue.isEmpty()){
   BlockPos p=queue.removeFirst();
   if(p.asLong()<key.asLong())key=p;
   for(Direction d:Direction.values()){
    BlockPos next=p.relative(d);
    if(!l.hasChunkAt(next)||next.distManhattan(start)>8||next.getY()<l.getMinBuildHeight()||next.getY()>=l.getMaxBuildHeight())return 0;
    BlockState b=l.getBlockState(next);
    if(b.getCollisionShape(l,next).isEmpty()){
     // The enclosure flood fill itself detects openings (radius/volume bounds).
     // Sky light can be stale immediately after placing or removing a roof;
     // do not reject a fully enclosed room while lighting propagates.
     if(!b.getFluidState().isEmpty())return 0;
     if(seen.add(next.immutable())){if(seen.size()>256)return 0;queue.add(next.immutable());}
    }else{
     String material=WorldLogicApi.materialFamily(b).toLowerCase(java.util.Locale.ROOT);
     mass+=material.contains("stone")||material.contains("metal")?2:material.contains("wood")?1.2:.7;boundaries++;
    }
   }
  }
  if(seen.size()<4||boundaries==0)return 0;
  var cache=ROOMS.computeIfAbsent(l,k->new LinkedHashMap<>(64,.75f,true));
  State old=cache.get(key);double target=Math.max(-8,Math.min(14,(source-cooling)*.35));
  double tau=30+40*mass/boundaries;
  if(old!=null&&target>0&&target>old.delta())tau/=3.0; // heat accumulation only; preserve passive heat loss
  double delta=old==null?0:Adaptation.approach(old.delta(),target,Math.min(300,Math.max(0,(now-old.tick())/20.0)),tau);
  if(old==null||now>old.tick())cache.put(key.immutable(),new State(now,delta));
  else delta=old.delta();
  while(cache.size()>256)cache.remove(cache.keySet().iterator().next());
  return delta;
 }
}
