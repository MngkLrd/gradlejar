package dev.minemods.worldlogic;
import dev.minemods.worldlogic.api.WorldLogicApi.*;
/** Local soil water and salt budgets; plant inputs are aggregate addon reports. */
final class PlantSoilModel{
 private PlantSoilModel(){}
 record Change(HydrologyModel.Store store,HydrologyState.Eco eco,double accepted,boolean applied){}
 static void validate(VegetationInput v){
  if(v==null||!Double.isFinite(v.waterDemandMm())||v.waterDemandMm()<0||v.waterDemandMm()>4||!unit(v.cover())||!unit(v.rootRetention())||!unit(v.litter()))throw new IllegalArgumentException("Invalid aggregate vegetation report");
 }
 static boolean unit(double v){return Double.isFinite(v)&&v>=0&&v<=1;}
 static void validateIrrigation(double mm,double salt){if(!Double.isFinite(mm)||mm<=0||mm>20||!Double.isFinite(salt)||salt<0||salt>100)throw new IllegalArgumentException("Invalid irrigation input");}
 static boolean due(long previous,long now){return previous<0||Math.floorDiv(now,200)>Math.floorDiv(previous,200);}
 static SoilEcology snapshot(HydrologyModel.Store s,SoilType soil,HydrologyState.Eco e){
  double concentration=e.salt()/Math.max(1,s.soil());
  double fertility=HydrologyModel.unit((HydrologyModel.profile(soil).fertility()+e.fertilityDelta())/(1+concentration*.18));
  return new SoilEcology(e.salt(),concentration,fertility,e.cover(),e.roots(),e.transpiration(),e.totalTranspiration(),e.saltExport(),e.recoveredCover(),e.vegetationTick());
 }
 static Change vegetation(HydrologyModel.Store s,SoilType soil,HydrologyState.Eco e,VegetationInput v,long now){
  validate(v);if(!due(e.vegetationTick(),now))return new Change(s,e,0,false);
  double actual=Math.min(s.soil(),v.waterDemandMm());
  double recovery=e.vegetationTick()<0?0:Math.max(0,v.cover()-e.cover());
  double fertility=Math.max(-.5,Math.min(.5,e.fertilityDelta()+.002*v.litter()-.0005*actual));
  var next=new HydrologyState.Eco(e.salt(),v.cover(),v.rootRetention(),fertility,actual,e.totalTranspiration()+actual,e.saltExport(),e.recoveredCover()+recovery,now,e.irrigationTick());
  return new Change(new HydrologyModel.Store(s.soil()-actual,s.groundwater(),s.snow(),s.pond()),next,actual,true);
 }
 static Change irrigate(HydrologyModel.Store s,SoilType soil,HydrologyState.Eco e,double offered,double salinity,long now){
  validateIrrigation(offered,salinity);if(!due(e.irrigationTick(),now))return new Change(s,e,0,false);
  double accepted=Math.min(offered,Math.max(0,HydrologyModel.profile(soil).capacity()-s.soil()));
  if(accepted<=0)return new Change(s,e,0,false);
  var next=new HydrologyState.Eco(e.salt()+accepted*salinity,e.cover(),e.roots(),e.fertilityDelta(),e.transpiration(),e.totalTranspiration(),e.saltExport(),e.recoveredCover(),e.vegetationTick(),now);
  return new Change(new HydrologyModel.Store(s.soil()+accepted,s.groundwater(),s.snow(),s.pond()),next,accepted,true);
 }
 static double vaporInfluence(HydrologyState.Eco e,long now){
  if(e.vegetationTick()<0||now<e.vegetationTick()||now-e.vegetationTick()>=400)return 0;
  return HydrologyModel.unit(e.transpiration()/4)*(1-(now-e.vegetationTick())/400.0);
 }
 static LocalWeather microclimate(LocalWeather w,double influence){
  double i=HydrologyModel.unit(influence);
  return new LocalWeather(w.kind(),w.intensity(),w.temperatureDeltaC()-1.2*i,Math.min(100,w.relativeHumidityPercent()+8*i),
   w.windXMps(),w.windZMps(),w.gustMps(),w.cloudFraction(),w.fogDensity(),w.solarTransmission(),w.precipitation(),w.precipitationIntensity(),w.pressureHpa(),w.exposed());
 }
 static HydrologyState.Eco leach(HydrologyState.Eco e,HydrologyModel.Store before,HydrologyModel.Step step){
  // Recharge exports dissolved salt from the root-zone budget; runoff is not counted twice.
  double exported=Math.min(e.salt(),e.salt()*Math.min(1,step.recharge()/Math.max(1,before.soil()+step.recharge()))*.85);
  return new HydrologyState.Eco(e.salt()-exported,e.cover(),e.roots(),e.fertilityDelta(),e.transpiration(),e.totalTranspiration(),exported,e.recoveredCover(),e.vegetationTick(),e.irrigationTick());
 }
}
