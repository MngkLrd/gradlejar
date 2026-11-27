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
  if(!allowed(l)){LAST.remove(l);return;}
  BlockPos spawn=l.getSharedSpawnPos();
  if(!l.hasChunkAt(spawn))return;
  int y=Math.min(l.getMaxBuildHeight()-1,Math.max(l.getMinBuildHeight(),l.getHeight(Heightmap.Types.MOTION_BLOCKING,spawn.getX(),spawn.getZ())+1));
  BlockPos anchor=new BlockPos(spawn.getX(),y,spawn.getZ());
  var preview=WorldLogicApi.weatherColumnPreview(l,anchor);
  if(preview.isEmpty())return;
  var c=preview.orElseThrow();
  Mode mode=choose(c.precipitationPotential(),c.convective(),c.buoyancyEnergyJkg(),l.getLevelData().isRaining(),l.getLevelData().isThundering());
  apply(l,mode);LAST.put(l,new Applied(l.getGameTime(),mode,anchor));
 }
 static Applied last(ServerLevel l){return LAST.get(l);}
 public static void tick(LevelTickEvent.Post e){
  if(e.getLevel() instanceof ServerLevel l&&l.getGameTime()%200==0)update(l);
 }
}
