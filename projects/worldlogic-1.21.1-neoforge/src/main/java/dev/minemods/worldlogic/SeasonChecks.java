package dev.minemods.worldlogic;
import dev.minemods.worldlogic.api.WorldLogicApi.*;
import dev.minemods.worldlogic.api.WorldLogicApi;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.*;
final class SeasonChecks{
 private static void require(boolean v,String s){if(!v)throw new IllegalStateException("seasons: "+s);}
 private static boolean close(double a,double b){return Math.abs(a-b)<1e-7;}
 static void verify(ServerLevel l){
  var state=new SeasonState();require(state.days()==24&&close(state.phase(24L*24000),.25),"default calendar");
  long t=1234567;double before=state.phase(t);state.length(t,3);require(close(before,state.phase(t))&&close(SeasonModel.wrap(before+.25),state.phase(t+3*24000L)),"length preserves phase");
  state.phase(t,.875);var saved=SeasonState.load(state.save(new CompoundTag(),l.registryAccess()),l.registryAccess());require(saved.days()==3&&close(saved.phase(t+10000),state.phase(t+10000)),"saved calendar roundtrip");
  var summer=SeasonModel.sample(.375,24,55,1,false);var winter=SeasonModel.sample(.875,24,55,1,false);
  require(summer.kind()==SeasonKind.SUMMER&&winter.kind()==SeasonKind.WINTER&&summer.temperatureOffsetC()-winter.temperatureOffsetC()>20,"temperate annual range");
  require(SeasonModel.sample(.375,24,55,-1,false).kind()==SeasonKind.WINTER,"opposite climate band");
  require(Math.abs(SeasonModel.sample(.375,24,55,1,true).temperatureOffsetC())<summer.temperatureOffsetC(),"marine moderation");
  require(Math.abs(SeasonModel.sample(.375,24,0,1,false).temperatureOffsetC())<2,"weak equatorial temperature cycle");
  require(Math.abs(SeasonModel.sample(.999999,24,55,1,false).temperatureOffsetC()-SeasonModel.sample(0,24,55,1,false).temperatureOffsetC())<.01,"continuous wrap");
  require(summer.solarFactor()>winter.solarFactor()&&summer.wetSeasonStrength()>winter.wetSeasonStrength(),"solar and wet cycle");
  require(!SeasonRuntime.active(l)&&WorldLogicApi.season(l,BlockPos.ZERO).isEmpty(),"flat-world isolation");
  int snow=0,rain=0;for(long tick=0;tick<24000L*4;tick+=400){
   var warm=LocalWeatherModel.sample(42,0,300,tick,20,new LocalWeatherModel.Climate(10+summer.temperatureOffsetC(),.85,55,1,0,false),true,summer.wetSeasonStrength());
   var cold=LocalWeatherModel.sample(42,0,300,tick,20,new LocalWeatherModel.Climate(10+winter.temperatureOffsetC(),.85,55,1,0,false),true,winter.wetSeasonStrength());
   if(warm.precipitation()==Precipitation.RAIN)rain++;
   if(cold.precipitation()==Precipitation.SNOW)snow++;
   require(warm.precipitation()!=Precipitation.SNOW,"warm seasonal precipitation");
  }
  require(snow>0&&rain>0,"seasonal precipitation phase");
  require(SeasonModel.snowAction(0,Precipitation.SNOW,1,0)==1&&SeasonModel.snowAction(1,Precipitation.NONE,0,0)==0&&SeasonModel.snowAction(8,Precipitation.NONE,0,0)==-1&&SeasonModel.snowAction(-8,Precipitation.NONE,0,0)==0,"snow accumulation hysteresis");
  var p=new BlockPos(5,110,5);var old=l.getBlockState(p);var support=l.getBlockState(p.below());try{
   l.setBlock(p.below(),Blocks.STONE.defaultBlockState(),3);l.setBlock(p,Blocks.AIR.defaultBlockState(),3);
   require(SeasonRuntime.applySnow(l,p,1),"actual snowfall layer");for(int i=0;i<3;i++)require(SeasonRuntime.applySnow(l,p,1),"accumulation");
   require(l.getBlockState(p).getValue(SnowLayerBlock.LAYERS)==4&&!SeasonRuntime.applySnow(l,p,1),"depth cap");
   for(int i=0;i<4;i++)require(SeasonRuntime.applySnow(l,p,-1),"actual melt");
   require(l.getBlockState(p).isAir(),"melt clears layer");
   l.setBlock(p,Blocks.SNOW_BLOCK.defaultBlockState(),3);require(!SeasonRuntime.applySnow(l,p,-1),"full snow block protected");
  }finally{l.setBlock(p,old,3);l.setBlock(p.below(),support,3);}
  org.slf4j.LoggerFactory.getLogger("WorldLogic").info("WORLDLOGIC_SEASONS rainSamples={} snowSamples={} clock=PASS layer=PASS",rain,snow);
 }
}
