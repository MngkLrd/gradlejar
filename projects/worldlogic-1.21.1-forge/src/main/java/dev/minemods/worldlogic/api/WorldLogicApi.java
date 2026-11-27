package dev.minemods.worldlogic.api;

import dev.minemods.worldlogic.GeoDensity;
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
import net.minecraft.world.level.block.state.BlockState;

/** Public addon contract, version 1. Server methods must be called on the server thread. */
public final class WorldLogicApi {
 public static final int API_VERSION=1;
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
