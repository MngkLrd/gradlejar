package dev.minemods.worldlogic;
import dev.minemods.worldlogic.api.WorldLogicApi;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
public final class ApiChecks {
 private static void require(boolean value,String detail){if(!value)throw new IllegalStateException("World Logic API: "+detail);}
 private static BlockPos weatherFixture(ServerLevel l){
  return new BlockPos(0,Math.min(l.getMaxBuildHeight()-1,Math.max(70,l.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING,0,0)+5)),0);
 }
 public static void verifyRecovery(ServerLevel l){
  var p=FakePlayerFactory.getMinecraft(l);double old=Survival.fatigue(p);int food=p.getFoodData().getFoodLevel();
  var pos=p.position();boolean grounded=p.onGround(),sprint=p.isSprinting(),sneak=p.isShiftKeyDown();
  try{
   require(WorldLogicApi.fatigue(p).enabled(),"exertion enabled");
   Survival.clearExertionTracking(p);
   p.getFoodData().setFoodLevel(20);p.setSprinting(false);p.setShiftKeyDown(false);p.setOnGround(true);p.setPos(.5,70,.5);
   Survival.rest(p,100);Survival.work(p,60);long now=l.getGameTime();double initial=Survival.fatigue(p);
   Survival.updateExertion(p,l,now);
   require(Survival.fatigue(p)==initial,"no immediate post-work recovery");
   p.setPos(4.5,70,.5);p.setOnGround(true);Survival.updateExertion(p,l,now+20);
   double walking=initial-Survival.fatigue(p);
   require(Math.abs(walking-2.4)<1e-8,"ordinary walking restores stamina");
   p.setPos(5.5,70,.5);p.setOnGround(true);p.setShiftKeyDown(true);
   double before=Survival.fatigue(p);Survival.updateExertion(p,l,now+40);
   require(before-Survival.fatigue(p)>walking,"slow sneak recovery");
   p.setShiftKeyDown(false);before=Survival.fatigue(p);Survival.updateExertion(p,l,now+60);
   require(before-Survival.fatigue(p)>walking,"standing recovery faster");
   p.getFoodData().setFoodLevel(5);p.setPos(9.5,70,.5);p.setOnGround(true);before=Survival.fatigue(p);
   Survival.updateExertion(p,l,now+80);
   require(before-Survival.fatigue(p)>0&&before-Survival.fatigue(p)<walking,"hunger reduces recovery");
   p.setSprinting(true);before=Survival.fatigue(p);Survival.updateExertion(p,l,now+100);
   require(Survival.fatigue(p)>before,"sprint consumes stamina");
   p.setSprinting(false);p.setOnGround(false);before=Survival.fatigue(p);Survival.updateExertion(p,l,now+120);
   require(Survival.fatigue(p)==before,"no airborne recovery");
   Survival.rest(p,100);p.setOnGround(true);Survival.updateExertion(p,l,now+140);
   require(Survival.fatigue(p)==0,"fatigue never negative");
  }finally{
   Survival.clearExertionTracking(p);p.getPersistentData().putDouble("worldlogic_exertion",old);
   p.getFoodData().setFoodLevel(food);p.setPos(pos.x,pos.y,pos.z);p.setOnGround(grounded);p.setSprinting(sprint);p.setShiftKeyDown(sneak);
  }
 }
 public static void verifyAtmosphere(ServerLevel l,boolean rain){
  BlockPos pos=weatherFixture(l);
  require(l.hasChunkAt(pos)&&l.canSeeSky(pos),"weather fixture must be loaded and exposed");
  require(l.isRaining()==rain,"weather fixture must match requested weather");
  var a=WorldLogicApi.atmosphere(l,pos).orElseThrow();
  require(a.environment().equals(WorldLogicApi.environment(l,pos).orElseThrow()),"RC20 environment preserved");
  require(Double.isFinite(a.relativeHumidityPercent())&&a.relativeHumidityPercent()>=0&&a.relativeHumidityPercent()<=100,"relative humidity bounds");
  require(a.outdoorWindSpeedMps()>0&&a.outdoorWindSpeedMps()<=15,"wind bounds");
  require(Math.abs(Math.hypot(a.outdoorWindXMps(),a.outdoorWindZMps())-a.outdoorWindSpeedMps())<1e-8,"wind vector");
  require(a.equals(WorldLogicApi.atmosphere(l,pos).orElseThrow()),"same-tick deterministic snapshot");
  require(WorldLogicApi.atmosphere(l,new BlockPos(1000000,64,1000000)).isEmpty(),"unloaded weather query");
  require(WorldLogicApi.atmosphere(l,new BlockPos(pos.getX(),l.getMaxBuildHeight(),pos.getZ())).isEmpty(),"out-of-height weather query");
  require(a.precipitation()==(rain?WorldLogicApi.Precipitation.RAIN:WorldLogicApi.Precipitation.NONE),"flat plains local precipitation");
  require(rain?a.precipitationIntensity()>0&&a.precipitationIntensity()<=1:a.precipitationIntensity()==0,"precipitation intensity");
  double dry=AtmosphereModel.humidity(25,25,.1,0),humid=AtmosphereModel.humidity(25,25,.9,0);
  require(dry<humid&&AtmosphereModel.humidity(35,25,.1,0)<dry,"moisture and temperature dependence");
  require(AtmosphereModel.humidity(25,25,.1,1)>dry,"rain moistens air");
  for(double t:new double[]{-100,-40,0,40,100}){
   double rh=AtmosphereModel.humidity(t,t,.5,0);
   require(Double.isFinite(rh)&&rh>=0&&rh<=100,"extreme temperature humidity");
  }
  var calm=AtmosphereModel.wind(42,0,0,0,1,0,0,0);
  var storm=AtmosphereModel.wind(42,0,0,0,1,0,1,1);
  require(storm.speed()>calm.speed()&&calm.equals(AtmosphereModel.wind(42,0,0,0,1,0,0,0)),"storm wind and deterministic model");
  require(AtmosphereModel.wind(42,0,0,1,1,0,0,0).speed()!=calm.speed(),"wind evolves with time");
 }
 public static void verifyRegionalWeather(ServerLevel l){
  BlockPos pos=weatherFixture(l);
  var live=WorldLogicApi.atmosphere(l,pos).orElseThrow();
  var a=WorldLogicApi.regionalWeatherPreview(l,pos).orElseThrow();
  require(a.equals(WorldLogicApi.regionalWeatherPreview(l,pos).orElseThrow()),"same-tick regional sample");
  require(live.equals(WorldLogicApi.atmosphere(l,pos).orElseThrow()),"preview does not mutate live weather");
  require(WorldLogicApi.regionalWeatherPreview(l,new BlockPos(1000000,64,1000000)).isEmpty(),"unloaded preview");
  require(WorldLogicApi.regionalWeatherPreview(l,new BlockPos(pos.getX(),l.getMaxBuildHeight(),pos.getZ())).isEmpty(),"out-of-height preview");
  var origin=RegionalWeatherModel.sample(42,0,0,0,0,.5,1,0);
  var high=RegionalWeatherModel.sample(42,0,0,0,200,.5,1,0);
  require(origin.seaLevelPressureHpa()==high.seaLevelPressureHpa()&&high.localPressureHpa()<origin.localPressureHpa(),"pressure falls with elevation");
  require(!origin.equals(RegionalWeatherModel.sample(43,0,0,0,0,.5,1,0)),"seed affects regional fields");
  require(!origin.equals(RegionalWeatherModel.sample(42,0,0,24000,0,.5,1,0)),"weather evolves over a day");
  boolean wet=false,dry=false;
  for(long seed:new long[]{42,-5,987654321234L})for(int x=-8192;x<=8192;x+=1024)for(int z=-8192;z<=8192;z+=1024){
   var s=RegionalWeatherModel.sample(seed,x,z,24000,0,.75,1,0);
   var neighbor=RegionalWeatherModel.sample(seed,x+1,z,24000,0,.75,1,0);
   var next=RegionalWeatherModel.sample(seed,x,z,24001,0,.75,1,0);
   var arid=RegionalWeatherModel.sample(seed,x,z,24000,0,.05,1,0);
   require(s.seaLevelPressureHpa()>=989&&s.seaLevelPressureHpa()<=1038&&Double.isFinite(s.localPressureHpa()),"pressure range");
   require(Math.abs(s.temperatureAnomalyC())<=8&&s.moistureFraction()>=0&&s.moistureFraction()<=1,"thermal and moisture bounds");
   require(s.cloudFraction()>=0&&s.cloudFraction()<=1&&s.precipitationPotential()>=0&&s.precipitationPotential()<=1,"cloud and precipitation bounds");
   require(s.gustMps()>=Math.hypot(s.windX(),s.windZ())-1e-8&&s.gustMps()<=24,"wind/gust bounds");
   require(Math.abs(s.seaLevelPressureHpa()-neighbor.seaLevelPressureHpa())<.02&&Math.abs(s.seaLevelPressureHpa()-next.seaLevelPressureHpa())<.002,"spatial and temporal continuity");
   require(arid.cloudFraction()<=s.cloudFraction()&&arid.precipitationPotential()<=s.precipitationPotential(),"dry climate does not increase cloud/precipitation");
   wet|=s.precipitationPotential()>.1;dry|=s.precipitationPotential()==0;
  }
  require(wet&&dry,"regional survey includes wet and dry potential");
 }
 public static void verifyWeatherColumn(ServerLevel l){
  BlockPos pos=weatherFixture(l);
  var live=WorldLogicApi.atmosphere(l,pos).orElseThrow();
  var a=WorldLogicApi.weatherColumnPreview(l,pos).orElseThrow();
  require(a.equals(WorldLogicApi.weatherColumnPreview(l,pos).orElseThrow()),"same-tick column");
  require(live.equals(WorldLogicApi.atmosphere(l,pos).orElseThrow()),"column must not change live atmosphere");
  require(WorldLogicApi.weatherColumnPreview(l,new BlockPos(1000000,64,1000000)).isEmpty(),"unloaded column query");
  require(WorldLogicApi.weatherColumnPreview(l,new BlockPos(pos.getX(),l.getMaxBuildHeight(),pos.getZ())).isEmpty(),"out-of-height column query");
  require(Math.abs(AtmosphericColumn.saturationHpa(0)-6.112)<1e-6,"saturation reference");
  require(Math.abs(AtmosphericColumn.dewPoint(AtmosphericColumn.saturationHpa(20))-20)<1e-8,"dew point inverse");
  var dry=AtmosphericColumn.diagnose(25,1013,5,9,1800,0,3);
  var wet=AtmosphericColumn.diagnose(25,1013,90,9,1800,0,3);
  var stable=AtmosphericColumn.diagnose(25,1013,90,2,1800,0,3);
  var noLift=AtmosphericColumn.diagnose(25,1013,90,9,0,0,3);
  require(dry.condensedWaterGkg()==0&&dry.precipitationPotential()==0&&!dry.convective(),"dry parcel does not rain");
  require(wet.condensationLevelM()>=0&&wet.cloudTopM()>wet.cloudBaseM()&&wet.condensedWaterGkg()>0,"humid lifted parcel condenses");
  require(wet.buoyancyEnergyJkg()>stable.buoyancyEnergyJkg()&&wet.cloudTopM()>stable.cloudTopM(),"stability controls vertical growth");
  require(noLift.condensedWaterGkg()==0&&!noLift.convective(),"no trigger means no active convection");
  var fog=AtmosphericColumn.diagnose(10,1013,95,2,0,3,1);
  var windy=AtmosphericColumn.diagnose(10,1013,95,2,0,3,10);
  require(fog.fogPotential()>0&&windy.fogPotential()==0,"radiation fog cooling and mixing");
  for(int t=-50;t<=50;t+=10)for(int rh=0;rh<=100;rh+=10)for(int p:new int[]{800,1013}){
   var d=AtmosphericColumn.diagnose(t,p,rh,7,1500,2,3);
   require(Double.isFinite(d.dewPointC())&&Double.isFinite(d.wetBulbC())&&d.dewPointC()<=d.wetBulbC()+1e-6&&d.wetBulbC()<=d.airC()+1e-6,"dew <= wet bulb <= air");
   require(d.condensedWaterGkg()>=0&&d.condensedWaterGkg()<=d.vaporMixingRatioGkg()+1e-8,"column moisture budget");
   require(d.cloudFraction()>=0&&d.cloudFraction()<=1&&d.precipitationPotential()>=0&&d.precipitationPotential()<=1&&Double.isFinite(d.buoyancyEnergyJkg()),"column index bounds");
  }
 }
 public static void verify(ServerLevel l){
  BlockPos pos=l.getSharedSpawnPos();
  var env=WorldLogicApi.environment(l,pos).orElseThrow();
  if(env.regionalClimate().isPresent()){
  var climate=env.regionalClimate().orElseThrow();
  var state=l.getChunkSource().randomState();
  var point=new DensityFunction.SinglePointContext(pos.getX(),128,pos.getZ());
  require(Math.abs(climate.moistureIndex()-(1+state.router().vegetation().compute(point))*.5)<1e-8,"regional moisture matches generator");
  require(Math.abs(GeoModel.biomeTemperature(climate.thermalIndex(),climate.moistureIndex()*2-1,climate.modeledSurfaceY())-state.router().temperature().compute(point))<1e-8,"physical climate maps to existing biome parameter");
  }
  require(Double.isFinite(env.ambientCelsiusEstimate())&&env.solarExposure()>=0&&env.solarExposure()<=1,"finite local climate");
  require(env.elevationAboveSea()==pos.getY()-l.getSeaLevel(),"elevation");
  require(WorldLogicApi.environment(l,new BlockPos(1000000,64,1000000)).isEmpty(),"unloaded location must be absent");
  require(WorldLogicApi.itemWeight(new ItemStack(Items.IRON_BLOCK))>WorldLogicApi.itemWeight(new ItemStack(Items.FEATHER)),"relative item weight");
  var p=FakePlayerFactory.getMinecraft(l);
  require(WorldLogicApi.fatigue(p).enabled(),"survival exertion enabled");
  WorldLogicApi.recoverExertion(p,100);
  require(WorldLogicApi.fatigue(p).fatiguePercent()==0,"rest reset");
  WorldLogicApi.addExertion(p,20);
  double exertion=WorldLogicApi.fatigue(p).fatiguePercent();
  require(exertion>0&&exertion<=100,"work affects existing fatigue");
  require(Math.abs(exertion+WorldLogicApi.fatigue(p).remainingStaminaPercent()-100)<1e-8,"complement");
  WorldLogicApi.addExertion(p,100);
  require(WorldLogicApi.fatigue(p).fatiguePercent()==100,"fatigue upper bound");
  try{WorldLogicApi.addExertion(p,Double.NaN);throw new IllegalStateException("accepted NaN");}catch(IllegalArgumentException expected){}
  WorldLogicApi.recoverExertion(p,100);
  require(WorldLogicApi.fatigue(p).fatiguePercent()==0,"recovery");
  require(StaminaScale.halfUnits(0)==20&&StaminaScale.halfUnits(50)==10&&StaminaScale.halfUnits(5)==19&&StaminaScale.halfUnits(100)==0,"HUD stamina scale");
  for(int f=1;f<=100;f++)require(StaminaScale.halfUnits(f)<=StaminaScale.halfUnits(f-1),"monotonic depletion");
  var jump=new net.neoforged.neoforge.event.entity.living.LivingEvent.LivingJumpEvent(p);
  Survival.jump(jump);require(WorldLogicApi.fatigue(p).fatiguePercent()>0,"jump uses shared fatigue");
  WorldLogicApi.recoverExertion(p,100);
  org.slf4j.LoggerFactory.getLogger("WorldLogic").info("WORLDLOGIC_API_CHECKS_PASSED");
 }
}
