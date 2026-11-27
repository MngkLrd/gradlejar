package dev.minemods.worldlogic;
import java.util.Map;
import java.util.WeakHashMap;
import dev.minemods.worldlogic.api.WorldLogicApi;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/** Opt-in single-anchor overworld weather driver. Vanilla flags, rendering, and gameplay remain authoritative. */
public final class WeatherController {
 private WeatherController(){}
 enum Mode { CLEAR, RAIN, THUNDER }
 record Applied(long tick,Mode mode,BlockPos anchor){}
 private static final Map<ServerLevel,Applied> LAST=new WeakHashMap<>();
 static Mode choose(double precipitation,boolean convective,double buoyancy,boolean raining,boolean thunder){
  if(precipitation<(raining?.20:.40))return Mode.CLEAR;
  if(convective&&buoyancy>=(thunder?90:180)&&precipitation>=(thunder?.35:.60))return Mode.THUNDER;
  return Mode.RAIN;
 }
 static void apply(ServerLevel l,Mode mode){
  l.setWeatherParameters(mode==Mode.CLEAR?600:0,mode==Mode.CLEAR?0:600,mode!=Mode.CLEAR,mode==Mode.THUNDER);
 }
 static boolean allowed(ServerLevel l){
  return l==l.getServer().overworld()&&Rules.on("modeled_weather")&&l.getGameRules().getBoolean(GameRules.RULE_WEATHER_CYCLE);
 }
 static void update(ServerLevel l){
  if(LocalWeatherRuntime.active(l)){LAST.remove(l);return;}
  if(!allowed(l)){LAST.remove(l);return;}
  BlockPos spawn=l.getSharedSpawnPos();
  if(!l.hasChunkAt(spawn))return;
  int y=Math.min(l.getMaxBuildHeight()-1,Math.max(l.getMinBuildHeight(),l.getHeight(Heightmap.Types.MOTION_BLOCKING,spawn.getX(),spawn.getZ())+1));
  BlockPos anchor=new BlockPos(spawn.getX(),y,spawn.getZ());
  var env=WorldLogicApi.environment(l,anchor);
  if(env.isEmpty())return;
  var climate=env.orElseThrow().regionalClimate();
  var biome=l.getBiome(anchor).value();
  double reference=climate.map(v->14+28*v.thermalIndex()).orElse(-12.0+32.0*biome.getBaseTemperature());
  double moisture=climate.map(WorldLogicApi.RegionalClimate::moistureIndex).orElse(biome.hasPrecipitation()?.55:.15);
  double wx=climate.map(WorldLogicApi.RegionalClimate::prevailingWindX).orElse(0.0),wz=climate.map(WorldLogicApi.RegionalClimate::prevailingWindZ).orElse(0.0);
  var cell=WeatherGridState.get(l).update(l,anchor,reference,moisture,wx,wz);
  var steering=RegionalWeatherModel.sample(l.getSeed(),anchor.getX(),anchor.getZ(),l.getGameTime(),0,moisture,wx,wz);
  double low=Math.max(0,Math.min(1,(1017-cell.pressureHpa())/28));
  double sun=env.orElseThrow().solarExposure();
  var c=AtmosphericColumn.diagnose(cell.airC(),cell.pressureHpa(),cell.relativeHumidity(),4.5+3*low+2*sun,200+1800*low+600*sun,(1-sun)*(1-steering.cloudFraction())*4,Math.hypot(steering.windX(),steering.windZ()));
  Mode mode=choose(c.precipitationPotential(),c.convective(),c.buoyancyEnergyJkg(),l.getLevelData().isRaining(),l.getLevelData().isThundering());
  apply(l,mode);LAST.put(l,new Applied(l.getGameTime(),mode,anchor));
 }
 static Applied last(ServerLevel l){return LAST.get(l);}
 public static void tick(LevelTickEvent.Post e){
  if(e.getLevel() instanceof ServerLevel l&&l.getGameTime()%200==0)update(l);
 }
}
