package dev.minemods.worldlogic;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
final class AnalyticsChecks{
 private static void require(boolean v,String s){if(!v)throw new IllegalStateException("analytics: "+s);}
 static void verify(ServerLevel l){
  var state=new AnalyticsState();var c=state.cell(42L);var clean=state.cell(43L);
  for(int n=1;n<=64;n++){EnvironmentAnalytics.update(c,n*1200L,5,0,.4,2);EnvironmentAnalytics.update(clean,n*1200L,0,0,0,2);}
  require(c.combustion>0&&c.soil>0&&clean.air()==0&&clean.soil==0,"combustion and soil deposition controls");
  require(c.history.size()==48,"bounded history");
  double before=c.combustion;EnvironmentAnalytics.update(c,65*1200L,0,.5,1,20);
  require(c.combustion<before&&c.dust==.5,"natural dust distinct and dispersion");
  require(EnvironmentAnalytics.combustion(Blocks.FURNACE.defaultBlockState().setValue(BlockStateProperties.LIT,true))&&!EnvironmentAnalytics.combustion(Blocks.FURNACE.defaultBlockState()),"active combustion");
  EnvironmentAnalytics.action(c,Blocks.OAK_LOG.defaultBlockState(),false);EnvironmentAnalytics.action(c,Blocks.STONE.defaultBlockState(),false);EnvironmentAnalytics.action(c,Blocks.DIRT.defaultBlockState(),false);EnvironmentAnalytics.action(c,Blocks.DIRT.defaultBlockState(),true);
  require(c.logs==1&&c.mined==1&&c.disturbed==1&&c.placed==1,"activity classifications");
  var loaded=AnalyticsState.load(state.save(new CompoundTag(),l.registryAccess()),l.registryAccess());
  require(EnvironmentAnalytics.snapshot(c).equals(EnvironmentAnalytics.snapshot(loaded.cells.get(42L)))&&java.util.List.copyOf(c.history).equals(java.util.List.copyOf(loaded.cells.get(42L).history)),"saved analytics");
  var pond=WaterClassification.registeredProfile(NaturalFluids.get(dev.minemods.worldlogic.api.WorldLogicApi.WaterKind.POND).source.get());
  require(WaterClassification.withSoil(pond,.25).contaminationRisk()>pond.contaminationRisk(),"pollution affects water quality");
  org.slf4j.LoggerFactory.getLogger("WorldLogic").info("WORLDLOGIC_ANALYTICS history={} air={} soil={} activity=4",c.history.size(),c.air(),c.soil);
 }
}
