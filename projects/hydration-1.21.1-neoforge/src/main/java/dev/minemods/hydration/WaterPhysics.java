package dev.minemods.hydration;

import dev.minemods.worldlogic.api.PhysicalMaterials;
import dev.minemods.worldlogic.api.WorldLogicApi;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import java.util.Locale;

/** Data-only bridge. Core supplies material coefficients; no gameplay load or thermal simulation. */
public final class WaterPhysics {
 private WaterPhysics(){}
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
 private static Fluid registered(String path){
  var key=ResourceLocation.fromNamespaceAndPath("worldlogic",path);
  Fluid fluid=BuiltInRegistries.FLUID.get(key);
  return key.equals(BuiltInRegistries.FLUID.getKey(fluid))?fluid:null;
 }
 /** Container units: 1000 mB = 1 L; never pass a terrain voxel as a bucket. */
 public static Properties fluid(Fluid fluid,int volumeMb){
  if(volumeMb<0)throw new IllegalArgumentException("negative volume");
  double volume=volumeMb/1_000_000.0;
  if(volumeMb==0||fluid==null)return unknown(volume);
  var object=PhysicalMaterials.fluidVolume(fluid,volumeMb);
  if(object.evidence()==PhysicalMaterials.Evidence.UNKNOWN||object.components().size()!=1)return unknown(volume);
  var id=object.components().get(0).materialId();
  var material=PhysicalMaterials.material(id);
  if(material.isEmpty()||material.get().phase()!=PhysicalMaterials.Phase.LIQUID)return unknown(volume);
  var result=PhysicalMaterials.evaluate(object,PhysicalMaterials.State.reference());
  if(!Double.isFinite(result.massKg())||!Double.isFinite(result.bulkDensityKgM3()))return unknown(volume);
  return new Properties(result.volumeM3(),result.massKg(),result.bulkDensityKgM3(),
    result.heatCapacityJK(),result.specificHeatJkgK(),material.get().viscosityPaS(),id.toString());
 }
 /** A stored batch can outlive its original fluid. Only use matching nominal salinity,
  * not provenance alone: distilled seawater is fresh and arbitrary blends remain unknown.
  * Core does not yet expose a continuous solution-density/viscosity model. */
 public static Properties batch(WaterBatch batch){
  int mb=batch.mB();double volume=mb/1_000_000.0;
  if(mb==0||batch.quality()==WaterBalance.Quality.UNKNOWN)return unknown(volume);
  for(String name:new String[]{"river_water","brackish_water","sea_water"}){
   Fluid fluid=registered(name);if(fluid==null)continue;
   var profile=WorldLogicApi.waterProfile(fluid);
   if(profile.salinityGkg()<0||profile.quality()==WorldLogicApi.WaterQuality.NOT_WATER)continue;
   if(Math.abs(batch.salinity()-profile.salinityGkg())<1e-8||
      (name.equals("river_water")&&batch.salinity()==0))return fluid(fluid,mb);
  }
  return unknown(volume);
 }
}
