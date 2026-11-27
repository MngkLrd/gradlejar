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
