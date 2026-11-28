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
 private static final int[][] NEIGHBOURS={{1,0},{-1,0},{0,1},{0,-1},{3,0},{-3,0},{0,3},{0,-3}};
 private record Vote(String id,int rank,long humidity,long temperature,int weight){}
 static long transitionHumidity(long center,List<Vote> votes){
  long total=0;int weights=0;
  for(var v:votes){total+=v.humidity*v.weight;weights+=v.weight;}
  return weights==0?center:center+Math.max(-1800L,Math.min(1800L,(total/weights-center)*2/3));
 }
 static Holder<Biome> resolve(MultiNoiseBiomeSource source,java.util.function.BiFunction<Integer,Integer,Climate.TargetPoint> sampler,int qx,int qz,long continental){
  var center=surface(sampler.apply(qx,qz),continental);
  var base=source.getNoiseBiome(center);String name=id(base);int rank=rank(name);
  if(continental<4000||rank<0)return base;
  var votes=new ArrayList<Vote>();var extremes=new ArrayList<Vote>();var counts=new HashMap<String,Integer>();
  int own=0,total=0;
  for(int i=0;i<NEIGHBOURS.length;i++){
   var d=NEIGHBOURS[i];var n=sampler.apply(qx+d[0],qz+d[1]);
   if(n.continentalness()<4000||Math.abs(n.temperature()-center.temperature())>850)continue;
   var neighbour=source.getNoiseBiome(surface(n,n.continentalness()));String nid=id(neighbour);int r=rank(nid);
   if(r<0)continue;int w=i<4?2:1;
   var v=new Vote(nid,r,n.humidity(),n.temperature(),w);votes.add(v);
   counts.merge(nid,w,Integer::sum);total+=w;if(name.equals(nid))own+=w;
   if(incompatible(rank,r)&&i<4)extremes.add(v);
  }
  // Strong moisture transitions take precedence, but keep the center's
  // thermal belt and relief controls. No mutable neighbour palettes.
  if(!extremes.isEmpty()){
   long humidity=(center.humidity()+extremes.stream().mapToLong(Vote::humidity).sum()/extremes.size())/2;
   var t=source.getNoiseBiome(new Climate.TargetPoint(center.temperature(),humidity,continental,center.erosion(),0L,center.weirdness()));
   int r=rank(id(t));if(r==1||r==2)return t;
  }
  // Suppress only small isolated slivers backed by a strong, compatible
  // surrounding consensus. Never replace a rare climate core simply because
  // it is unusual; the changed point must remain near its original climate.
  if(total>=8&&own<=1){
   long humidity=transitionHumidity(center.humidity(),votes);
   var candidate=source.getNoiseBiome(new Climate.TargetPoint(center.temperature(),humidity,continental,center.erosion(),0L,center.weirdness()));
   int r=rank(id(candidate));int support=counts.getOrDefault(id(candidate),0);
   if(r>=0&&Math.abs(rank-r)<=1&&support>=Math.max(6,total*2/3))return candidate;
  }
  return base;
 }
 static void verify(){
  if(transitionHumidity(0,List.of(new Vote("prairie",1,10000,0,2)))!=1800||transitionHumidity(0,List.of(new Vote("prairie",1,-10000,0,2)))!=-1800||transitionHumidity(420,List.of())!=420)throw new IllegalStateException("bounded climate transition");
  if(!incompatible(rank("hot_desert"),rank("equatorial_rainforest"))||
     !incompatible(rank("temperate_desert"),rank("temperate_rainforest"))||
     incompatible(rank("prairie"),rank("mixed_forest"))||
     incompatible(rank("polar_ocean"),rank("hot_desert"))||
     incompatible(rank("nival_zone"),rank("equatorial_rainforest"))||
     incompatible(rank("limestone_cavern"),rank("hot_desert")))throw new IllegalStateException("biome edge policy");
 }
}
