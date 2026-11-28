package dev.minemods.aquaculture;
import java.util.*;
import net.minecraft.util.RandomSource;

/** Habitat-constrained rarity with explicit local saturation. No population ledger yet. */
public final class SpawnSelection {
 public static final int LOCAL_CAP=36;
 private static final double DIVERSITY_SHARE=.15;
 private SpawnSelection(){}
 public static int speciesCap(Species s){return Math.max(3,Math.min(10,2+s.weight()/4));}
 private record Candidate(Species species,double weight){}
 private static List<Candidate> candidates(Aquaculture.Habitat h,Map<String,Integer> counts,java.util.Optional<dev.minemods.worldlogic.api.WorldLogicApi.RiverCurrent> flow){
  if(h==null)return List.of();
  List<Candidate> list=new ArrayList<>();
  for(Species s:Species.ALL){
   int n=counts.getOrDefault(s.id(),0);
   int cap=speciesCap(s);
   int weight=s.habitatWeight(h.kind(),h.temp(),h.salt(),h.risk(),h.depth());
   if(weight<=0||n>=cap)continue;
   double suitability=RiverBehavior.weight(s,flow);if(suitability>0)list.add(new Candidate(s,suitability*weight/(1.0+.9*n)));
  }
  return list;
 }
 public static Map<String,Double> probabilities(Aquaculture.Habitat h,Map<String,Integer> counts){
  var candidates=candidates(h,counts,java.util.Optional.empty());
  Map<String,Double> result=new LinkedHashMap<>();
  if(candidates.isEmpty())return result;
  double total=candidates.stream().mapToDouble(Candidate::weight).sum();
  for(var c:candidates)result.put(c.species.id(),(1-DIVERSITY_SHARE)*c.weight/total+DIVERSITY_SHARE/candidates.size());
  return result;
 }
 public static Species choose(Aquaculture.Habitat h,Map<String,Integer> counts,RandomSource random){return choose(h,counts,random,Optional.empty());}
 public static Species choose(Aquaculture.Habitat h,Map<String,Integer> counts,RandomSource random,Optional<dev.minemods.worldlogic.api.WorldLogicApi.RiverCurrent> flow){
  var candidates=candidates(h,counts,flow);
  if(candidates.isEmpty())return null;
  if(random.nextDouble()<DIVERSITY_SHARE)return candidates.get(random.nextInt(candidates.size())).species;
  double total=candidates.stream().mapToDouble(Candidate::weight).sum();
  double draw=random.nextDouble()*total;
  for(var c:candidates){draw-=c.weight;if(draw<0)return c.species;}
  return candidates.getLast().species;
 }
}
