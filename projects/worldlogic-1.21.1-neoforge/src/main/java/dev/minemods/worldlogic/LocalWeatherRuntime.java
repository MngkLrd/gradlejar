package dev.minemods.worldlogic;
import java.util.Optional;
import dev.minemods.worldlogic.api.WorldLogicApi.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
public final class LocalWeatherRuntime {
 private LocalWeatherRuntime(){}
 private record Demo(BlockPos center,long expires,WeatherKind kind){}
 private static final java.util.Map<ServerLevel,Demo> DEMOS=new java.util.WeakHashMap<>();
 public static boolean demo(ServerLevel l,BlockPos p,WeatherKind kind){
  if(!active(l)||!l.hasChunkAt(p))return false;
  DEMOS.put(l,new Demo(p.immutable(),l.getGameTime()+2400,kind));return true;
 }
 public static void clearDemo(ServerLevel l){DEMOS.remove(l);}
 static double demoWeight(BlockPos center,BlockPos p,long expires,long now){
  if(now>=expires)return 0;
  double distance=Math.hypot((double)p.getX()-center.getX(),(double)p.getZ()-center.getZ());
  return (1-LocalWeatherModel.smooth(96,128,distance))*LocalWeatherModel.smooth(0,100,expires-now);
 }
 public static boolean active(ServerLevel l){
  if(l!=l.getServer().overworld()||!Rules.on("regional_weather")||!l.getGameRules().getBoolean(GameRules.RULE_WEATHER_CYCLE))return false;
  return l.getChunkSource().getGenerator() instanceof NoiseBasedChunkGenerator gen&&gen.generatorSettings().unwrapKey().map(k->k.location().getNamespace().equals("worldlogic")&&java.util.Set.of("earthlike_climate","earthlike_extended","realistic_unified").contains(k.location().getPath())).orElse(false);
 }
 public static Optional<LocalWeather> at(ServerLevel l,BlockPos p){
  if(!active(l)||l.isOutsideBuildHeight(p)||!l.hasChunkAt(p))return Optional.empty();
  return at(l,p,GeoDensity.regional(l,p.getX(),p.getZ()));
 }
 public static Optional<LocalWeather> at(ServerLevel l,BlockPos p,Optional<RegionalClimate> region){
  if(!active(l)||l.isOutsideBuildHeight(p)||!l.hasChunkAt(p)||region.isEmpty())return Optional.empty();
  var c=region.orElseThrow();
  var biome=l.getBiome(p);
  boolean dry=biome.is(TagKey.create(Registries.BIOME,ResourceLocation.fromNamespaceAndPath("worldlogic","vegetation/desert")))||
    biome.is(TagKey.create(Registries.BIOME,ResourceLocation.fromNamespaceAndPath("worldlogic","vegetation/scrub")))||!biome.value().hasPrecipitation();
  double temperature=14+28*c.thermalIndex()-.065*Math.max(-80,Math.min(160,p.getY()-c.modeledSurfaceY()));
  var season=SeasonRuntime.at(l,p,region);
  var climate=new LocalWeatherModel.Climate(temperature+season.map(Season::temperatureOffsetC).orElse(0.0),
    LocalWeatherModel.unit(c.moistureIndex()+season.map(Season::moistureOffset).orElse(0.0)),c.absoluteLatitude(),c.prevailingWindX(),c.prevailingWindZ(),dry);
  var w=season.isPresent()?LocalWeatherModel.sample(l.getSeed(),p.getX(),p.getZ(),l.getGameTime(),p.getY()-l.getSeaLevel(),climate,l.canSeeSky(p),season.get().wetSeasonStrength()):
    LocalWeatherModel.sample(l.getSeed(),p.getX(),p.getZ(),l.getGameTime(),p.getY()-l.getSeaLevel(),climate,l.canSeeSky(p));
  var d=DEMOS.get(l);
  if(d!=null){
   if(d.expires<=l.getGameTime())DEMOS.remove(l);
   else{
    double strength=demoWeight(d.center,p,d.expires,l.getGameTime());
    if(strength>.01)w=LocalWeatherModel.demo(w,d.kind,strength);
   }
  }
  double plants=HydrologyRuntime.transpirationInfluence(l,p,c.modeledSurfaceY());
  if(plants>0&&w.exposed())w=PlantSoilModel.microclimate(w,plants);
  return Optional.of(w);
 }
 static boolean rainingAt(ServerLevel l,BlockPos p){return at(l,p).map(w->w.precipitation()==Precipitation.RAIN).orElseGet(()->l.isRainingAt(p));}
 static boolean snowingAt(ServerLevel l,BlockPos p){return at(l,p).map(w->w.precipitation()==Precipitation.SNOW).orElseGet(l::isRaining);}
 public static void tick(LevelTickEvent.Post e){
  if(e.getLevel() instanceof ServerLevel l&&l.getGameTime()%100==0&&active(l)){
   // No world-wide rainfall can contradict two simultaneous local weather zones.
   if(l.getLevelData().isRaining()||l.getLevelData().isThundering())l.setWeatherParameters(1200,0,false,false);
  }
 }
 public static void playerTick(PlayerTickEvent.Post e){
  if(!(e.getEntity() instanceof ServerPlayer p)||p instanceof net.neoforged.neoforge.common.util.FakePlayer||p.tickCount%20!=0)return;
  LocalWeatherSync.send(p,at(p.serverLevel(),p.blockPosition()));
 }
}
