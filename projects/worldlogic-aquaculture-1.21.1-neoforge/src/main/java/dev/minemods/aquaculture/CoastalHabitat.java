package dev.minemods.aquaculture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import java.util.Set;

/** Loaded, shallow bank habitat. Existing Core climate/salinity and Aqua species constraints stay authoritative. */
public final class CoastalHabitat {
 private CoastalHabitat(){}
 private static final Set<String> SHORE=Set.of("perch","roach","crucian_carp","carp","rudd","pike","trout","grayling","char","whitefish","tench","loach","rotan","tilapia","piranha","snakehead","herring","flounder","snapper","parrotfish","barracuda");
 public static boolean eligible(Species s,Aquaculture.Habitat h){
  return s!=null&&h!=null&&SHORE.contains(s.id())&&h.depth()<=6&&s.suitable(h.kind(),h.temp(),h.salt(),h.risk(),h.depth());
 }
 public static boolean nearBank(ServerLevel level,BlockPos p){
  if(!Aquaculture.water(level,p))return false;
  BlockPos surface=p;
  for(int n=0;n<3&&Aquaculture.water(level,surface.above());n++)surface=surface.above();
  if(Aquaculture.water(level,surface.above())||Aquaculture.depth(level,p)>6)return false;
  for(Direction d:Direction.Plane.HORIZONTAL){
   for(int n=1;n<=6;n++){
    BlockPos q=surface.relative(d,n);
    if(!level.hasChunkAt(q)||level.isOutsideBuildHeight(q))break;
    if(Aquaculture.water(level,q))continue;
    if(level.getBlockState(q).isCollisionShapeFullBlock(level,q)||
      (!Aquaculture.water(level,q.below())&&level.getBlockState(q.below()).isCollisionShapeFullBlock(level,q.below())))return true;
    break;
   }
  }
  return false;
 }
 static boolean canFound(PopulationData data,PopulationData.Area area,Species s){
  return !data.shoreSeeded(area,s)&&data.room(area,s)>=2&&(!data.seeded(area,s)||data.total(area,s.id())>0);
 }
}
