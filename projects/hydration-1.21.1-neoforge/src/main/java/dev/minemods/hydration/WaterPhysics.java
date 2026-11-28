package dev.minemods.hydration;
import dev.minemods.worldlogic.api.PhysicalWater;
import dev.minemods.worldlogic.api.PhysicalMaterials;
import net.minecraft.world.level.material.Fluid;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalDouble;

/** Core-owned seawater-like solution model. Reference-temperature diagnostics, no thermal simulation. */
public final class WaterPhysics {
 private WaterPhysics(){}
 public static final double REFERENCE_C=20;
 public record Properties(double volumeM3,double massKg,double densityKgM3,
    double heatCapacityJK,double specificHeatJkgK,double viscosityPaS,String materialId) {
  public boolean known(){return Double.isFinite(massKg)&&Double.isFinite(densityKgM3);}
  public String diagnostic(){return String.format(Locale.ROOT,"volume_m3=%.6f mass_kg=%s density_kg_m3=%s material=%s",
    volumeM3,known()?String.format(Locale.ROOT,"%.6f",massKg):"unknown",
    known()?String.format(Locale.ROOT,"%.3f",densityKgM3):"unknown",materialId);}
 }
 private static Properties unknown(double volume){
  return new Properties(volume,volume==0?0:Double.NaN,Double.NaN,volume==0?0:Double.NaN,Double.NaN,Double.NaN,"UNKNOWN");
 }
 private static Properties properties(int mb,Optional<PhysicalWater.Solution> sample){
  if(mb<0)throw new IllegalArgumentException("negative volume");
  double volume=mb/1_000_000.0;
  if(mb==0||sample.isEmpty())return unknown(volume);
  var s=sample.get();double mass=volume*s.densityKgM3();
  // Retain previous material label only as a display category, never as a lookup for solution coefficients.
  String material=s.salinityGkg()<=.2?"worldlogic:fresh_water":s.salinityGkg()==12?"worldlogic:brackish_water":s.salinityGkg()==35?"worldlogic:sea_water":"worldlogic:water_solution";
  return new Properties(volume,mass,s.densityKgM3(),mass*s.specificHeatJkgK(),s.specificHeatJkgK(),s.viscosityPaS(),material);
 }
 public static Properties fluid(Fluid fluid,int mb){return properties(mb,fluid==null?Optional.empty():PhysicalWater.solution(fluid,REFERENCE_C));}
 public static Optional<PhysicalWater.Solution> solution(WaterBatch b,double temperatureC){
  if(b.mB()==0||b.quality()==WaterBalance.Quality.UNKNOWN)return Optional.empty();
  return PhysicalWater.solution(b.salinity(),temperatureC);
 }
 public static Properties batch(WaterBatch b){return properties(b.mB(),solution(b,REFERENCE_C));}
 public static OptionalDouble freezing(WaterBatch b){return b.mB()==0||b.quality()==WaterBalance.Quality.UNKNOWN?OptionalDouble.empty():PhysicalWater.freezingPointC(b.salinity());}
 public static OptionalDouble boiling(WaterBatch b){return b.mB()==0||b.quality()==WaterBalance.Quality.UNKNOWN?OptionalDouble.empty():PhysicalWater.boilingPointC(b.salinity());}
 /** Volume remains the existing integer mB gameplay budget. Input masses are calculated by Core density.
  * The output volume is additive, not a thermodynamic contraction model; no exact mass claim for reconstructed output. */
 public static OptionalDouble mixed(WaterBatch a,WaterBatch b,int moved){
  if(moved<=0||b.mB()==0)return OptionalDouble.empty();
  var x=batch(a.withVolume(moved));var y=batch(b);
  if(!x.known()||!y.known())return OptionalDouble.empty();
  return PhysicalWater.mixedSalinityGkg(x.massKg(),a.salinity(),y.massKg(),b.salinity());
 }
 /** Solve remaining salt mass at reference temperature after the existing gameplay evaporation.
  * The Core solution model provides density, not a duplicate local correlation. */
 public static OptionalDouble concentrated(WaterBatch b,int remainingMb){
  if(remainingMb<=0||remainingMb>b.mB())return OptionalDouble.empty();
  var initial=batch(b);if(!initial.known())return OptionalDouble.empty();
  if(b.salinity()==0)return OptionalDouble.of(0);
  double target=initial.massKg()*b.salinity(), volume=remainingMb/1_000_000.0;
  double lo=b.salinity(),hi=PhysicalWater.MAX_SALINITY_GKG;
  if(PhysicalWater.solution(hi,REFERENCE_C).orElseThrow().densityKgM3()*volume*hi<target)return OptionalDouble.empty();
  for(int i=0;i<48;i++){
   double mid=(lo+hi)*.5;
   if(PhysicalWater.solution(mid,REFERENCE_C).orElseThrow().densityKgM3()*volume*mid<target)lo=mid;else hi=mid;
  }
  return OptionalDouble.of((lo+hi)*.5);
 }
}
