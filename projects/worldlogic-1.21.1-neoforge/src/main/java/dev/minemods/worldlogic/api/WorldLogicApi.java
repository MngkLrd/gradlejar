package dev.minemods.worldlogic.api;

import dev.minemods.worldlogic.GeoDensity;
import dev.minemods.worldlogic.AtmosphereModel;
import dev.minemods.worldlogic.RegionalWeatherModel;
import dev.minemods.worldlogic.Material;
import dev.minemods.worldlogic.RuleSync;
import dev.minemods.worldlogic.Rules;
import dev.minemods.worldlogic.Survival;
import java.util.Optional;
import java.util.OptionalInt;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;

/** Public addon contract, version 1. Server methods must be called on the server thread. */
public final class WorldLogicApi {
 public static final int API_VERSION=1;
 /** RC22 adds a regional weather preview; existing v1 signatures and live environment semantics unchanged. */
 public static final int API_REVISION=3;
 private WorldLogicApi(){}
 /** Thermal and moisture indices are modeled values, not biome selection temperature or relative humidity.
  * Wind is prevailing horizontal direction (not a measured wind speed). Latitude is absolute 0..90.
  * Surface elevation is the macro terrain model, not an actual block height. */
 public record RegionalClimate(double thermalIndex,double moistureIndex,double absoluteLatitude,
                               double prevailingWindX,double prevailingWindZ,double modeledSurfaceY){}
 /** Instantaneous local world conditions; ambient Celsius is an approximation, without body heat or heat sources.
  * solarExposure is a normalized direct-sun proxy, not watts/m2. covered means no sky visibility. */
 public record Environment(double ambientCelsiusEstimate,int elevationAboveSea,boolean covered,
                           boolean rainingAt,boolean globalThunder,double solarExposure,
                           Optional<RegionalClimate> regionalClimate){}
 public enum Precipitation { NONE, RAIN, SNOW }
 /** Modeled relative humidity and open-air wind; local precipitation reflects vanilla weather.
  * Wind vector components and speed are estimated m/s, not a physical fluid simulation.
  * Local precipitation is normalized 0..1, not mm/hour. */
 public record Atmosphere(Environment environment,double relativeHumidityPercent,
                          double outdoorWindXMps,double outdoorWindZMps,double outdoorWindSpeedMps,
                          Precipitation precipitation,double precipitationIntensity){}
 /** Experimental regional weather model, not live local conditions. Never drive player penalties from this preview. */
 public record RegionalWeatherPreview(double seaLevelPressureHpa,double localPressureHpa,
  double temperatureAnomalyC,double moistureFraction,double cloudFraction,double precipitationPotential,
  double windXMps,double windZMps,double gustMps){}
 public record Fatigue(boolean enabled,double fatiguePercent,double remainingStaminaPercent){}
 private static void serverThread(ServerLevel level){
  if(!level.getServer().isSameThread())throw new IllegalStateException("World Logic API requires server thread");
 }
 private static double clamp(double value,double low,double high){return Math.max(low,Math.min(high,value));}
 /** Loaded locations only; never generates or force-loads chunks. */
 public static Optional<Environment> environment(ServerLevel level,BlockPos pos){
  serverThread(level);
  if(level.isOutsideBuildHeight(pos)||!level.hasChunkAt(pos))return Optional.empty();
  var regional=GeoDensity.regional(level,pos.getX(),pos.getZ());
  double base=regional.map(c->14+28*c.thermalIndex()-Math.max(-80,Math.min(160,pos.getY()-c.modeledSurfaceY()))*.065)
   .orElseGet(()->-12+32*level.getBiome(pos).value().getBaseTemperature()-Math.max(0,pos.getY()-level.getSeaLevel())*.065);
  boolean sky=level.dimensionType().hasSkyLight()&&level.canSeeSky(pos);
  double sun=sky?Math.max(0,Math.cos((Math.floorMod(level.getDayTime(),24000L)-6000)*Math.PI/12000.0)):0;
  sun*=level.getBrightness(LightLayer.SKY,pos)/15.0;
  if(level.isRaining())sun*=.35;
  boolean rain=level.isRainingAt(pos);
  double ambient=clamp(base+(sky?sun*4-2:0)-(rain?2:0),-100,100);
  return Optional.of(new Environment(ambient,pos.getY()-level.getSeaLevel(),!sky,rain,level.isThundering(),sun,regional));
 }
 /** RC21: weather snapshot for loaded locations in skylight dimensions; no chunk loads. */
 public static Optional<Atmosphere> atmosphere(ServerLevel level,BlockPos pos){
  serverThread(level);
  if(!level.dimensionType().hasSkyLight())return Optional.empty();
  var existing=environment(level,pos);
  if(existing.isEmpty())return Optional.empty();
  var env=existing.orElseThrow();
  var biome=level.getBiome(pos).value();
  var regional=env.regionalClimate();
  double moisture=regional.map(RegionalClimate::moistureIndex).orElse(biome.hasPrecipitation()?.55:.15);
  double reference=regional.map(c->14+28*c.thermalIndex()-Math.max(-80,Math.min(160,pos.getY()-c.modeledSurfaceY()))*.065)
   .orElseGet(()->-12+32*biome.getBaseTemperature()-Math.max(0,pos.getY()-level.getSeaLevel())*.065);
  double rain=level.isRaining()?clamp(level.getRainLevel(1.0F),0,1):0;
  double thunder=level.isThundering()?clamp(level.getThunderLevel(1.0F),0,1):0;
  var kind=biome.getPrecipitationAt(pos);
  Precipitation local=rain<=0||env.covered()?Precipitation.NONE:
    kind==Biome.Precipitation.RAIN&&env.rainingAt()?Precipitation.RAIN:
    kind==Biome.Precipitation.SNOW?Precipitation.SNOW:Precipitation.NONE;
  double intensity=local==Precipitation.NONE?0:rain;
  // Regional humidity responds to weather even under a roof; a roof is not sealed insulation.
  double wetWeather=biome.hasPrecipitation()?rain:0;
  double humidity=AtmosphereModel.humidity(env.ambientCelsiusEstimate(),reference,moisture,wetWeather);
  var wind=AtmosphereModel.wind(level.getSeed(),pos.getX(),pos.getZ(),level.getGameTime(),
    regional.map(RegionalClimate::prevailingWindX).orElse(0.0),
    regional.map(RegionalClimate::prevailingWindZ).orElse(0.0),rain,thunder);
  return Optional.of(new Atmosphere(env,humidity,wind.x(),wind.z(),wind.speed(),local,intensity));
 }
 /** RC22 preview: immutable regional fields for a loaded location, never loads terrain or changes vanilla weather. */
 public static Optional<RegionalWeatherPreview> regionalWeatherPreview(ServerLevel level,BlockPos pos){
  serverThread(level);
  if(!level.dimensionType().hasSkyLight())return Optional.empty();
  var env=environment(level,pos);
  if(env.isEmpty())return Optional.empty();
  var c=env.orElseThrow().regionalClimate();
  var s=RegionalWeatherModel.sample(level.getSeed(),pos.getX(),pos.getZ(),level.getGameTime(),
    env.orElseThrow().elevationAboveSea(),
    c.map(RegionalClimate::moistureIndex).orElse(level.getBiome(pos).value().hasPrecipitation()?.55:.15),
    c.map(RegionalClimate::prevailingWindX).orElse(0.0),c.map(RegionalClimate::prevailingWindZ).orElse(0.0));
  return Optional.of(new RegionalWeatherPreview(s.seaLevelPressureHpa(),s.localPressureHpa(),s.temperatureAnomalyC(),
    s.moistureFraction(),s.cloudFraction(),s.precipitationPotential(),s.windX(),s.windZ(),s.gustMps()));
 }
 public static Fatigue fatigue(ServerPlayer player){
  serverThread(player.serverLevel());
  boolean enabled=Rules.on("exertion")&&!player.isCreative()&&!player.isSpectator();
  double fatigue=enabled?Survival.fatigue(player):0;
  return new Fatigue(enabled,fatigue,100-fatigue);
 }
 /** Adds existing exertion units; movement strength is applied by the core. */
 public static void addExertion(ServerPlayer player,double amount){
  serverThread(player.serverLevel());validate(amount);Survival.work(player,amount);
 }
 /** Recovers existing exertion units, subject to the core rule and game mode. */
 public static void recoverExertion(ServerPlayer player,double amount){
  serverThread(player.serverLevel());validate(amount);Survival.rest(player,amount);
 }
 private static void validate(double amount){
  if(!Double.isFinite(amount)||amount<0||amount>100)throw new IllegalArgumentException("exertion amount must be finite and in 0..100");
 }
 /** Local client only: synchronized integer percentage, empty until sync/when disabled. */
 public static OptionalInt clientFatiguePercent(){return RuleSync.clientFatiguePercent();}
 /** Relative gameplay weight and mass, not SI kilograms. */
 public static double itemWeight(ItemStack stack){return Material.weight(stack);}
 public static double entityMass(Entity entity){return Material.mass(entity);}
 public static String materialFamily(BlockState state){return Material.family(state);}
 public static boolean ruleEnabled(String key){return Rules.on(key);}
 public static double ruleStrength(String family){return Rules.strength(family);}
}
