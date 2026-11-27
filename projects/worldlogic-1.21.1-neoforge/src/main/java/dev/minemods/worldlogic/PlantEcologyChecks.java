package dev.minemods.worldlogic;
import java.util.*;
import java.util.concurrent.*;
import dev.minemods.worldlogic.api.WorldLogicApi;
import dev.minemods.worldlogic.api.WorldLogicApi.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
final class PlantEcologyChecks{
 static void require(boolean v,String s){if(!v)throw new IllegalStateException("plant ecology: "+s);}
 static void close(double a,double b,String s){require(Math.abs(a-b)<1e-8,s+" "+a+" != "+b);}
 static void verify(ServerLevel l)throws Exception{
  var key=ResourceKey.create(Registries.WORLD_PRESET,ResourceLocation.fromNamespaceAndPath("worldlogic","realistic_unified"));
  var gen=(NoiseBasedChunkGenerator)l.registryAccess().lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(key).value().createWorldDimensions().overworld();
  var state=RandomState.create(gen.generatorSettings().value(),l.registryAccess().lookupOrThrow(Registries.NOISE),42);
  var sampler=WorldLogicApi.worldgenSampler(state,l.registryAccess(),gen).orElseThrow();
  var reg=l.registryAccess().lookupOrThrow(Registries.BIOME);
  var marine=reg.getOrThrow(ResourceKey.create(Registries.BIOME,ResourceLocation.fromNamespaceAndPath("worldlogic","temperate_ocean")));
  var land=reg.getOrThrow(ResourceKey.create(Registries.BIOME,ResourceLocation.fromNamespaceAndPath("worldlogic","mixed_forest")));
  var p=new BlockPos(127,63,-91);var bed=Blocks.DIRT.defaultBlockState();
  var ocean=sampler.predictWater(p,p,45,marine,bed);
  require(ocean.kind()==WaterKind.SEA&&ocean.quality()==WaterQuality.DESALINATE,"marine prediction");
  require(sampler.predictWater(p.below(30),p,60,land,bed).kind()==WaterKind.GROUNDWATER,"groundwater prediction");
  require(sampler.predictWater(p,p,60,land,bed).kind()==NaturalWaterGeneration.surfaceKind(land,sampler.sample(p,land,bed).riverbankPotential()),"converter classification parity");
  var pool=Executors.newFixedThreadPool(2);
  try{var jobs=new ArrayList<Future<WaterProfile>>();for(int i=0;i<8;i++)jobs.add(pool.submit(()->sampler.predictWater(p,p,45,marine,bed)));for(var x:jobs)require(ocean.equals(x.get(10,TimeUnit.SECONDS)),"worker deterministic prediction");}finally{pool.shutdownNow();}
  require(WorldLogicApi.soilEcology(l,p).isEmpty()&&WorldLogicApi.applyVegetation(l,p,new VegetationInput(1,.4,.4,.4)).isEmpty(),"unsupported world no mutation");
  var e=HydrologyState.emptyEco();var store=new HydrologyModel.Store(2,70,0,0);
  var first=PlantSoilModel.vegetation(store,SoilType.LOAM,e,new VegetationInput(4,.25,.5,.5),200);
  require(first.applied()&&first.accepted()==2&&first.store().soil()==0,"limited uptake");
  close(store.mass(),first.store().mass()+first.accepted(),"transpiration mass");
  var duplicate=PlantSoilModel.vegetation(first.store(),SoilType.LOAM,first.eco(),new VegetationInput(4,.8,1,1),201);
  require(!duplicate.applied()&&duplicate.store().equals(first.store()),"duplicate interval rejected");
  var ir=PlantSoilModel.irrigate(first.store(),SoilType.LOAM,first.eco(),20,35,200);
  close(ir.store().mass(),first.store().mass()+ir.accepted(),"irrigation water");
  close(ir.eco().salt(),ir.accepted()*35,"salt input");
  require(!PlantSoilModel.irrigate(ir.store(),SoilType.LOAM,ir.eco(),20,35,201).applied(),"duplicate irrigation");
  var next=PlantSoilModel.vegetation(ir.store(),SoilType.LOAM,ir.eco(),new VegetationInput(2,.75,1,1),400);
  close(next.eco().salt(),ir.eco().salt(),"transpiration retains salt");
  require(next.eco().recoveredCover()==.5&&next.eco().totalTranspiration()==4,"cover recovery and cumulative uptake");
  require(PlantSoilModel.snapshot(next.store(),SoilType.LOAM,next.eco()).effectiveFertility()<HydrologyModel.profile(SoilType.LOAM).fertility(),"salt stress");
  var bare=HydrologyModel.advance(new HydrologyModel.Store(109,120,0,6),SoilType.LOAM,20,0,20,.2,1,10,0);
  var roots=HydrologyModel.advance(new HydrologyModel.Store(109,120,0,6),SoilType.LOAM,20,0,20,.2,1,10,1);
  require(roots.runoff()<bare.runoff()&&roots.evaporation()<=bare.evaporation(),"root water retention");
  close(255,roots.store().mass()+roots.runoff()+roots.evaporation(),"root water budget");
  var leach=PlantSoilModel.leach(next.eco(),next.store(),roots);
  require(leach.saltExport()>0,"leaching");close(next.eco().salt(),leach.salt()+leach.saltExport(),"salt budget");
  require(PlantSoilModel.vaporInfluence(next.eco(),400)>0&&PlantSoilModel.vaporInfluence(next.eco(),800)==0,"transpiration microclimate expires");
  var natural=LocalWeatherModel.sample(42,0,0,0,0,new LocalWeatherModel.Climate(25,.5,20,1,0,false),true);
  var plantAir=PlantSoilModel.microclimate(natural,1);
  require(plantAir.temperatureDeltaC()<natural.temperatureDeltaC()&&plantAir.relativeHumidityPercent()>=natural.relativeHumidityPercent()&&plantAir.kind()==natural.kind(),"evaporative cooling and humidity");
  var data=new HydrologyState();data.cells.put(7L,new HydrologyState.Cell(next.store(),0,0,0,0,400));data.ecology.put(7L,next.eco());
  var restored=HydrologyState.load(data.save(new CompoundTag(),l.registryAccess()),l.registryAccess());
  require(data.cells.equals(restored.cells)&&data.ecology.equals(restored.ecology),"persistent plant and salt state");
  var analytics=new AnalyticsState();var cell=analytics.cell(7L);EnvironmentAnalytics.observeCanopy(cell,.5,200);require(cell.recoveredCanopy==0,"baseline is not recovery");
  EnvironmentAnalytics.observeCanopy(cell,.25,400);EnvironmentAnalytics.observeCanopy(cell,.375,600);
  close(cell.recoveredCanopy,.125,"observed recovery");
  var a=AnalyticsState.load(analytics.save(new CompoundTag(),l.registryAccess()),l.registryAccess()).cells.get(7L);
  close(a.canopy,.375,"canopy persisted");close(a.baselineCanopy,.5,"baseline persisted");close(a.recoveredCanopy,.125,"recovery persisted");
  // Actual top-leaf measurement in a loaded flat-world chunk, restored afterward.
  var q=new BlockPos(4,110,4);require(l.hasChunkAt(q),"loaded canopy fixture");
  var old=l.getBlockState(q);var chunk=new net.minecraft.world.level.ChunkPos(q);
  try{
   l.setBlock(q,Blocks.AIR.defaultBlockState(),2);double before=EnvironmentAnalytics.canopy(l,chunk);
   l.setBlock(q,Blocks.OAK_LEAVES.defaultBlockState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.PERSISTENT,true),2);
   close(EnvironmentAnalytics.canopy(l,chunk),before+1.0/256,"actual canopy observation");
  }finally{l.setBlock(q,old,2);}
  org.slf4j.LoggerFactory.getLogger("WorldLogic").info("WORLDLOGIC_PLANT_ECOLOGY workers=8 waterBudget=PASS saltBudget=PASS persistence=PASS canopyRecovery=PASS");
 }
}
