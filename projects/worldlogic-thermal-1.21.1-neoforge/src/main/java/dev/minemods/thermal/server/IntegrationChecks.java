package dev.minemods.thermal.server;
import dev.minemods.thermal.*;
import dev.minemods.thermal.api.ThermalApi;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;
final class IntegrationChecks {
 private static void require(boolean ok,String name){if(!ok)throw new IllegalStateException("Integration QA: "+name);}
 private static ThermalModel.State model(double air,PhysiologyInputs p){
  return ThermalModel.tick(ThermalModel.initial(),new ThermalModel.Exposure(air,0,0,0,0,0,false,air,0,0,0,0,0,p.nutrition(1),p.coolingCapacity(),0,p.circulation()),1);
 }
 static void verify(ServerLevel l,ServerPlayer p){
  var absent=PhysiologyInputs.absent();
  require(absent.nutrition(.2)==.2&&absent.coolingCapacity()==1&&absent.circulation()==1,"optional-provider fallback");
  var energy=new PhysiologyInputs(true,false,false,.1,.4,100,50,1);
  require(Math.abs(energy.nutrition(1)-.16)<1e-8&&energy.nutrition(0)==energy.nutrition(1),"Metabolism supersedes vanilla nutrition");
  require(model(-10,energy).targetC()<model(-10,absent).targetC(),"energy deficiency worsens cold");
  var dehydrated=new PhysiologyInputs(false,true,false,1,1,40,50,1);
  var salts=new PhysiologyInputs(false,true,false,1,1,40,95,1);
  require(model(40,salts).targetC()>model(40,dehydrated).targetC()&&model(40,dehydrated).targetC()>model(40,absent).targetC(),"water and electrolytes affect cooling separately");
  var bleeding=new PhysiologyInputs(false,false,true,1,1,100,50,.4);
  require(model(-10,bleeding).targetC()<model(-10,absent).targetC()&&model(40,bleeding).targetC()>model(40,absent).targetC(),"blood fraction affects thermal stress");
  require(model(22,bleeding).targetC()==37,"blood loss alone does not invent thermal illness");
  var invalid=new PhysiologyInputs(true,true,true,Double.NaN,2,-1,Double.NaN,Double.POSITIVE_INFINITY);
  require(invalid.energy()==1&&invalid.dietQuality()==1&&invalid.waterPercent()==0&&invalid.electrolyteIndex()==50&&invalid.bloodFraction()==1,"finite bounded values");
  var sample=EnvironmentSampler.sample(p,0).orElseThrow();
  if(!ModList.get().isLoaded("metabolism"))require(!sample.physiology().metabolism(),"absent metabolism not marked present");
  else require(sample.physiology().metabolism()&&sample.exposure().mealWarmth()==0,"installed metabolism read and no second meal bonus");
  if(!ModList.get().isLoaded("hydration"))require(!sample.physiology().hydration(),"absent hydration not marked present");
  else require(sample.physiology().hydration(),"installed hydration read");
  if(!ModList.get().isLoaded("worldlogic_trauma"))require(!sample.physiology().trauma(),"absent trauma not marked present");
  else if(dev.minemods.worldlogic.api.WorldLogicApi.ruleEnabled("injuries"))require(sample.physiology().trauma(),"installed enabled trauma read");
  ThermalEvents.update(p);
  var ph=ThermalApi.physiology(p).orElseThrow();
  require(ph.inputs().equals(sample.physiology()),"public snapshot matches sampled physiological inputs");
  require(Math.abs(ph.nutritionFactor()-sample.exposure().nutrition())<1e-8&&Math.abs(ph.coolingCapacity()-sample.exposure().hydration())<1e-8,"public factors match actual model");
  require(ThermalApi.status(p).isPresent()&&ThermalApi.API_VERSION==1,"legacy temperature contract retained");
 }
}
