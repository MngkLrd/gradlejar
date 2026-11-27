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
 public static void verifyAtmosphere(ServerLevel l,boolean rain){
  BlockPos pos=l.getSharedSpawnPos().above(5);
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
  BlockPos pos=l.getSharedSpawnPos().above(5);
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
