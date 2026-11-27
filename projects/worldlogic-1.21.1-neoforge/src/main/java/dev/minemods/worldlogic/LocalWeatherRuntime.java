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
  return Optional.of(LocalWeatherModel.sample(l.getSeed(),p.getX(),p.getZ(),l.getGameTime(),p.getY()-l.getSeaLevel(),
   new LocalWeatherModel.Climate(temperature,c.moistureIndex(),c.absoluteLatitude(),c.prevailingWindX(),c.prevailingWindZ(),dry),l.canSeeSky(p)));
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
