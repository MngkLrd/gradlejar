package dev.minemods.worldlogic.api;

import dev.minemods.worldlogic.GeoDensity;
import dev.minemods.worldlogic.AtmosphereModel;
import dev.minemods.worldlogic.RegionalWeatherModel;
import dev.minemods.worldlogic.AtmosphericColumn;
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
 public static final int API_REVISION=15;
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
 public enum SeasonKind { SPRING, SUMMER, AUTUMN, WINTER }
 /** Local climatological season. Progress values are fractions, offsets are applied by Core.
  * Days are 24000 elapsed simulation ticks, independent of /time and sleeping. */
 public record Season(SeasonKind kind,int dayOfSeason,int daysPerSeason,double yearProgress,double seasonProgress,
  double temperatureOffsetC,double solarFactor,double wetSeasonStrength,double moistureOffset){}
 public static Optional<Season> season(ServerLevel level,BlockPos pos){
  serverThread(level);
  return dev.minemods.worldlogic.SeasonRuntime.at(level,pos,GeoDensity.regional(level,pos.getX(),pos.getZ()));
 }
 public enum Precipitation { NONE, RAIN, SNOW }
 public enum WeatherKind { CLEAR, RAIN, DOWNPOUR, THUNDERSTORM, SNOW, BLIZZARD, DUST_STORM, MONSOON, FOG }
 /** Authoritative local gameplay conditions in geographic presets. Intensity, cloud, fog and solar transmission: 0..1. */
 public record LocalWeather(WeatherKind kind,double intensity,double temperatureDeltaC,double relativeHumidityPercent,
  double windXMps,double windZMps,double gustMps,double cloudFraction,double fogDensity,double solarTransmission,
  Precipitation precipitation,double precipitationIntensity,double pressureHpa,boolean exposed){}
 /** Empty for disabled rules, unsupported dimensions/generators and unloaded positions. Server thread only. */
 public static Optional<LocalWeather> localWeather(ServerLevel level,BlockPos pos){
  serverThread(level);
  return dev.minemods.worldlogic.LocalWeatherRuntime.at(level,pos);
 }
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
 /** RC22 experimental vertical diagnostics. Heights are modeled atmospheric metres above ground.
  * -1 means no condensation/cloud within the modeled column. Indices are not live precipitation or visibility. */
 public record WeatherColumnPreview(double airC,double pressureHpa,double vaporMixingRatioGkg,
  double relativeHumidityPercent,double dewPointC,double wetBulbC,double environmentalLapseCPerKm,
  double condensationLevelM,double cloudBaseM,double cloudTopM,double condensedWaterGkg,
  double buoyancyEnergyJkg,double cloudFraction,double precipitationPotential,double fogPotential,boolean convective){}
 /** World generation only: long-term modeled habitat, never instantaneous weather. */
 public enum Zone { OPEN_PLAINS,TEMPERATE_FOREST,COLD_FOREST,WETLAND,DRYLAND,TROPICAL,ALPINE,RIVERBANK,TUNDRA,COAST,MARINE,SUBTERRANEAN,UNKNOWN }
 public enum SurfaceFamily { NORMAL,DRY,MOIST,SANDY,BOGGY,ROCKY,FROZEN,UNKNOWN }
 public record WorldgenEnvironment(double temperatureCelsius,double moisture,double fertility,double dryness,double waterlogging,
  double coldness,double environmentStrength,Zone zone,SurfaceFamily surfaceFamily,double slopeDegrees,double windExposure,
  double prevailingWindX,double prevailingWindZ,double modeledSurfaceY,double riverbankPotential,net.minecraft.resources.ResourceLocation biomeId){}
 @FunctionalInterface public interface WorldgenSampler {
  /** No world/block/chunk queries. Pass the already available biome and ground block; do not retain across worlds/reloads. */
  WorldgenEnvironment sample(BlockPos pos,net.minecraft.core.Holder<Biome> biome,BlockState ground);
  /** Predict the identity of an actual or hypothetical water block before decoration conversion.
   * Caller supplies column ocean-floor height (same heightmap convention as generator),
   * biome at surface and bed state, and surface sample position. No reads or writes.
   * Does not predict the existence of water. */
  default WaterProfile predictWater(BlockPos waterPos,BlockPos surfacePos,int oceanFloorY,
    net.minecraft.core.Holder<Biome> surfaceBiome,BlockState bed){
   return dev.minemods.worldlogic.NaturalWaterGeneration.predict(waterPos,oceanFloorY,surfaceBiome,
    sample(surfacePos,surfaceBiome,bed).riverbankPotential());
  }
 }
 /** Build from the caller's existing generator state. No live server-thread assertion, world access or chunk loading. */
 public static Optional<WorldgenSampler> worldgenSampler(net.minecraft.world.level.levelgen.RandomState state,
   net.minecraft.core.RegistryAccess registry,net.minecraft.world.level.chunk.ChunkGenerator generator){
  return GeoDensity.worldgenSampler(state,registry,generator);
 }
 /** Convenience for a feature in the existing WorldGenLevel. Reads only generator state and registry references. */
 public static Optional<WorldgenSampler> worldgenSampler(net.minecraft.world.level.WorldGenLevel level,net.minecraft.world.level.chunk.ChunkGenerator generator){
  return worldgenSampler(level.getLevel().getChunkSource().randomState(),level.registryAccess(),generator);
 }
 /** Natural-water classification, not a medical assessment of drinking safety. */
 public enum WaterSource { NONE, FRESH_SURFACE, SALINE, GROUNDWATER, UNKNOWN }
 /** Detailed in-game source class. Existing WaterSource is retained as a compatibility projection. */
 public enum WaterKind { NONE, SEA, RIVER, LAKE, SPRING, GROUNDWATER, WETLAND, POND, BRACKISH, UNKNOWN }
 /** Gameplay treatment recommendation; never a real-world potability assessment. */
 public enum WaterQuality { DRINKABLE, TREAT_RECOMMENDED, DESALINATE, UNKNOWN, NOT_WATER }
 public enum WaterEvidence { FLUID_ABSENT, BIOME, TERRAIN_PROXY, LOCAL_GEOMETRY, UNCLASSIFIED, REGISTERED_FLUID }
 /** Estimated salinity in g/kg (-1 unknown); contamination risk 0..1 (-1 unknown).
  * Game balance estimates, not measured water chemistry. Flowing describes vanilla fluid state, not river current. */
 public record WaterProfile(WaterKind kind,WaterQuality quality,double salinityGkg,double contaminationRisk,
  boolean sourceBlock,boolean flowing,WaterEvidence evidence){}
 /** Loaded block only, no forced chunk loads. Detailed natural-source inference for Hydration. */
 /** Registry identity and intrinsic quality, for tanks, buckets and modded fluid handlers.
  * Does not include surrounding soil; the positional overload adds that local effect. */
 public static WaterProfile waterProfile(net.minecraft.world.level.material.Fluid fluid){
  return dev.minemods.worldlogic.WaterClassification.registeredProfile(fluid);
 }
 public static WaterProfile waterProfile(ServerLevel level,BlockPos pos){
  serverThread(level);return dev.minemods.worldlogic.WaterClassification.sample(level,pos);
 }
 public enum SoilType { LOAM,HUMUS,PEAT,SILT,FROZEN,SAND,CLAY,ROCK,UNKNOWN,CHERNOZEM,SANDY_LOAM,LATERITE,WHITE_SAND,VOLCANIC_SAND,VOLCANIC_ASH }
 /** Model stores expressed in mm water equivalent per horizontal area; rates per 200-tick update. */
 public record Hydrology(SoilType soil,double soilWaterMm,double capacityMm,double groundwaterMm,double snowWaterMm,
  double pondedWaterMm,double lastRunoffMm,double lastEvaporationMm,double lastRechargeMm,double lastDischargeMm,
  double saturation,double fertility,double permeability,double estimatedWaterTableDepthBlocks,boolean simulated,long updatedTick){}
 /** Pure classification of an existing block, safe to use during world generation. */
 public static SoilType soilType(BlockState state){return dev.minemods.worldlogic.HydrologyModel.soilType(state);}
 /** Representative game parameters. Mineral fractions sum to 1 for soils.
  * Organic fraction is a separate whole-soil estimate; pH -1 is not applicable.
  * Not a measured chemical sample; use soilEcology/hydrology for live state. */
 public record GroundProfile(SoilType soil,double sandFraction,double siltFraction,double clayFraction,
  double organicMatterFraction,double nominalPh,double fertility,double waterCapacityMm,double permeability,double fieldCapacityFraction,boolean rootable){}
 public static boolean cultivatedSoil(BlockState state){return state.getBlock() instanceof net.minecraft.world.level.block.FarmBlock;}
 public static BlockState parentSoil(BlockState state){return dev.minemods.worldlogic.SoilFarmland.original(state);}
 public static GroundProfile soilProfile(BlockState state){return dev.minemods.worldlogic.HydrologyModel.groundProfile(state);}
 /** Ground position, not plant position. Read actual root horizon under grass/farmland without loading chunks. */
 public static Optional<GroundProfile> soilProfile(ServerLevel level,BlockPos ground){
  serverThread(level);
  if(level.isOutsideBuildHeight(ground)||!level.hasChunkAt(ground))return Optional.empty();
  var b=level.getBlockState(ground);
  if((b.is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK)||b.is(net.minecraft.world.level.block.Blocks.FARMLAND)||b.is(net.minecraft.world.level.block.Blocks.PODZOL))&&!level.isOutsideBuildHeight(ground.below())){
   var sub=soilProfile(level.getBlockState(ground.below()));if(sub.rootable())return Optional.of(sub);
  }
  return Optional.of(soilProfile(b));
 }
 public static Optional<Hydrology> hydrology(ServerLevel level,BlockPos pos){
  serverThread(level);return dev.minemods.worldlogic.HydrologyRuntime.sample(level,pos);
 }
 /** Classifies actual local water; no chunk loads; no thirst or consumption mechanics. */
 public static WaterSource waterSource(ServerLevel level,BlockPos pos){
  serverThread(level);return dev.minemods.worldlogic.HydrologyRuntime.waterSource(level,pos);
 }
 /** Cell-scale game indices, not measured concentrations. -1 denotes no sample. */
 public record EnvironmentalMetrics(double airLoad,double naturalDust,double combustionLoad,double soilLoad,
  long observedTick,int observedCombustionSources,int removedLogs,int minedBlocks,int disturbedSoil,int placedBlocks){}
 public static Optional<EnvironmentalMetrics> environmentalMetrics(ServerLevel level,BlockPos pos){
  serverThread(level);return dev.minemods.worldlogic.EnvironmentAnalytics.metrics(level,pos);
 }
 /** Aggregate input for one 16x16 horizontal soil cell per 200 game ticks.
  * Water mm is equivalent to liters per square meter (256 L/mm per cell).
  * Cover, root retention, and litter are normalized 0..1. Demand is total transpiration,
  * not per-tree; maximum accepted demand is 4 mm per interval. */
 public record VegetationInput(double waterDemandMm,double cover,double rootRetention,double litter){}
 /** Actual water withdrawal and updated state. Repeated reports in one interval are rejected. */
 public record VegetationResult(boolean applied,double consumedMm,double requestedMm,SoilEcology ecology){}
 public record SoilEcology(double saltGm2,double salinityGPerLiter,double effectiveFertility,
  double vegetationCover,double rootRetention,double lastTranspirationMm,double totalTranspirationMm,
  double lastSaltExportGm2,double recoveredCover,long lastVegetationTick){}
 public static Optional<VegetationResult> applyVegetation(ServerLevel level,BlockPos pos,VegetationInput input){
  serverThread(level);return dev.minemods.worldlogic.HydrologyRuntime.vegetation(level,pos,input);
 }
 /** Report externally supplied irrigation already removed from a container/source.
  * Returns accepted mm; caller must account for unaccepted volume. Salt input is g/L.
  * One report per cell per 200 ticks. No fluid blocks are created. */
 public static double irrigateSoil(ServerLevel level,BlockPos pos,double offeredMm,double salinityGPerLiter){
  serverThread(level);return dev.minemods.worldlogic.HydrologyRuntime.irrigate(level,pos,offeredMm,salinityGPerLiter);
 }
 public static Optional<SoilEcology> soilEcology(ServerLevel level,BlockPos pos){
  serverThread(level);return dev.minemods.worldlogic.HydrologyRuntime.ecology(level,pos);
 }
 /** Observed top-leaf fraction of 256 columns, not tree count or global forest inventory. */
 public record ForestObservation(double canopyCover,double baselineCover,double cumulativeRecovery,long observedTick){}
 public static Optional<ForestObservation> forestObservation(ServerLevel level,BlockPos pos){
  serverThread(level);return dev.minemods.worldlogic.EnvironmentAnalytics.forest(level,pos);
 }
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
  var season=dev.minemods.worldlogic.SeasonRuntime.at(level,pos,regional);
  base+=season.map(Season::temperatureOffsetC).orElse(0.0);
  sun=clamp(sun*season.map(Season::solarFactor).orElse(1.0),0,1);
  var weather=dev.minemods.worldlogic.LocalWeatherRuntime.at(level,pos,regional);
  if(weather.isPresent()){
   var w=weather.orElseThrow();sun*=w.solarTransmission();
   // This is the actual air temperature reference; do not apply the anomaly again in Temperature.
   double ambient=clamp(base+(sky?sun*4-2:0)+w.temperatureDeltaC(),-100,100);
   return Optional.of(new Environment(ambient,pos.getY()-level.getSeaLevel(),!sky,w.precipitation()==Precipitation.RAIN,w.kind()==WeatherKind.THUNDERSTORM,sun,regional));
  }
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
  var regionalWeather=dev.minemods.worldlogic.LocalWeatherRuntime.at(level,pos,env.regionalClimate());
  if(regionalWeather.isPresent()){
   var w=regionalWeather.orElseThrow();
   return Optional.of(new Atmosphere(env,w.relativeHumidityPercent(),w.windXMps(),w.windZMps(),Math.hypot(w.windXMps(),w.windZMps()),w.precipitation(),w.precipitationIntensity()));
  }
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
 /** RC22 research model: uses the same climate reference and moving regional fields; not authoritative for addons. */
 public static Optional<WeatherColumnPreview> weatherColumnPreview(ServerLevel level,BlockPos pos){
  serverThread(level);
  var a=atmosphere(level,pos);
  if(a.isEmpty())return Optional.empty();
  var env=a.orElseThrow().environment();
  var climate=env.regionalClimate();
  double moisture=climate.map(RegionalClimate::moistureIndex).orElse(level.getBiome(pos).value().hasPrecipitation()?.55:.15);
  var w=RegionalWeatherModel.sample(level.getSeed(),pos.getX(),pos.getZ(),level.getGameTime(),env.elevationAboveSea(),
    moisture,climate.map(RegionalClimate::prevailingWindX).orElse(0.0),climate.map(RegionalClimate::prevailingWindZ).orElse(0.0));
  double air=env.ambientCelsiusEstimate()+w.temperatureAnomalyC();
  // Keep the dew-point prior tied to climate and the moving moisture field, not to the temperature anomaly.
  double rh=AtmosphereModel.humidity(air,env.ambientCelsiusEstimate(),w.moistureFraction(),0);
  double low=clamp((1017-w.seaLevelPressureHpa())/28,0,1);
  double sun=env.solarExposure();
  double lapse=clamp(4.5+3*low+2*sun,-2,10);
  double lift=200+1800*low+600*sun;
  double speed=Math.hypot(w.windX(),w.windZ());
  double cooling=(1-sun)*(1-w.cloudFraction())*4;
  var d=AtmosphericColumn.diagnose(air,w.localPressureHpa(),rh,lapse,lift,cooling,speed);
  return Optional.of(new WeatherColumnPreview(d.airC(),d.pressureHpa(),d.vaporMixingRatioGkg(),d.relativeHumidityPercent(),
   d.dewPointC(),d.wetBulbC(),d.environmentalLapseCPerKm(),d.condensationLevelM(),d.cloudBaseM(),d.cloudTopM(),
   d.condensedWaterGkg(),d.buoyancyEnergyJkg(),d.cloudFraction(),d.precipitationPotential(),d.fogPotential(),d.convective()));
 }
 /** Experimental persisted transport domain; not the authoritative player environment. */
 public static Optional<dev.minemods.worldlogic.WeatherGridState.Conditions> transportGridPreview(ServerLevel level,BlockPos pos){
  serverThread(level);
  if(dev.minemods.worldlogic.LocalWeatherRuntime.active(level)||!Rules.on("modeled_weather")||level!=level.getServer().overworld()||level.isOutsideBuildHeight(pos)||!level.hasChunkAt(pos))return Optional.empty();
  return dev.minemods.worldlogic.WeatherGridState.sample(level,pos);
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
 /** RC53 physical profiles in SI. Does not alter legacy relative gameplay weight or mass.
  * For registration, component properties, item state and explicit liquid volumes use PhysicalMaterials. */
 public static PhysicalMaterials.ObjectProfile physicalBlock(BlockState state){return PhysicalMaterials.block(state);}
 public static PhysicalMaterials.Result physicalItem(ItemStack stack){return PhysicalMaterials.itemResult(stack);}
 public static PhysicalMaterials.Result physicalStack(ItemStack stack){return PhysicalMaterials.stackResult(stack);}
 public static PhysicalMaterials.ObjectProfile physicalFluid(net.minecraft.world.level.material.Fluid fluid){return PhysicalMaterials.fluid(fluid);}
 public static Optional<PhysicalMaterials.Result> physicalPlaced(ServerLevel level,BlockPos pos){return PhysicalMaterials.placed(level,pos);}
 /** Relative gameplay weight and mass, not SI kilograms. */
 public static double itemWeight(ItemStack stack){return Material.weight(stack);}
 public static double entityMass(Entity entity){return Material.mass(entity);}
 public static String materialFamily(BlockState state){return Material.family(state);}
 public static boolean ruleEnabled(String key){return Rules.on(key);}
 public static double ruleStrength(String family){return Rules.strength(family);}
}
