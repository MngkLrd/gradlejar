package dev.minemods.worldlogic;
import java.util.*;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.*;
/** Edge correction on the unchanged regional climate. Cardinal neighbours are
 * queried from the same world-coordinate sampler, never from loaded chunks.
 * Ocean, coast, cave and relief-controlled biomes cannot be replaced here. */
final class RegionalBiomeEdges{
 private static final Set<String> DRY=Set.of("polar_desert","polar_gravel_desert","temperate_desert","subtropical_desert","hot_desert","rock_desert");
 private static final Set<String> OPEN=Set.of("dry_tundra","shrub_tundra","cold_steppe","forest_tundra","temperate_steppe","shortgrass_steppe","prairie","mediterranean_scrub","sclerophyll_woodland","subtropical_grassland","thorn_scrub","dry_savanna","humid_savanna");
 private static final Set<String> FOREST=Set.of("moist_tundra","continental_taiga","light_conifer_taiga","dark_conifer_taiga","mixed_forest","temperate_deciduous_forest","warm_broadleaf_forest","seasonal_tropical_forest","tropical_monsoon_forest");
 private static final Set<String> WET=Set.of("arctic_wetland","boreal_peatland","temperate_rainforest","subtropical_rainforest","equatorial_rainforest","temperate_marsh","tropical_floodplain");
 static int rank(String id){return DRY.contains(id)?0:OPEN.contains(id)?1:FOREST.contains(id)?2:WET.contains(id)?3:-1;}
 private static String id(Holder<Biome> b){return b.unwrapKey().orElseThrow().location().getPath();}
 static boolean incompatible(int a,int b){return a>=0&&b>=0&&Math.abs(a-b)>=3;}
 static Climate.TargetPoint surface(Climate.TargetPoint t,long continental){
  return new Climate.TargetPoint(t.temperature(),t.humidity(),continental,t.erosion(),0L,t.weirdness());
 }
 static Holder<Biome> resolve(MultiNoiseBiomeSource source,Climate.Sampler sampler,int qx,int qz,long continental){
  var center=surface(sampler.sample(qx,0,qz),continental);
  var base=source.getNoiseBiome(center);int rank=rank(id(base));
  if(continental<4000||rank<0||rank==1||rank==2)return base;
  long sum=0;int count=0;
  for(int[] d:new int[][]{{1,0},{-1,0},{0,1},{0,-1}}){
   var n=sampler.sample(qx+d[0],0,qz+d[1]);
   if(n.continentalness()<4000)continue;
   var neighbour=source.getNoiseBiome(surface(n,n.continentalness()));
   if(incompatible(rank,rank(id(neighbour)))){sum+=n.humidity();count++;}
  }
  if(count==0)return base;
  // Move toward the midpoint only on an extreme edge. Preserve temperature,
  // erosion, depth and the physical land constraint from the original point.
  long humidity=(center.humidity()+sum/count)/2;
  var transition=source.getNoiseBiome(new Climate.TargetPoint(center.temperature(),humidity,continental,center.erosion(),0L,center.weirdness()));
  int r=rank(id(transition));
  return r==1||r==2?transition:base;
 }
 static void verify(){
  if(!incompatible(rank("hot_desert"),rank("equatorial_rainforest"))||
     !incompatible(rank("temperate_desert"),rank("temperate_rainforest"))||
     incompatible(rank("prairie"),rank("mixed_forest"))||
     incompatible(rank("polar_ocean"),rank("hot_desert"))||
     incompatible(rank("nival_zone"),rank("equatorial_rainforest"))||
     incompatible(rank("limestone_cavern"),rank("hot_desert")))throw new IllegalStateException("biome edge policy");
 }
}
