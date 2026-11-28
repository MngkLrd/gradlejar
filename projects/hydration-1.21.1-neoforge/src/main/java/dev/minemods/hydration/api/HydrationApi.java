package dev.minemods.hydration.api;
import java.util.Optional;
import dev.minemods.hydration.Hydration;
import dev.minemods.hydration.WaterBottle;
import dev.minemods.hydration.VesselItem;
import dev.minemods.hydration.TankEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import java.util.Set;
import net.minecraft.server.level.ServerPlayer;

/** Public server-thread API. Hydration owns fluid balance, never foreign body state. */
public final class HydrationApi {
 public static final int API_VERSION=1;
 public static final int API_REVISION=3;
 /** Reference-temperature SI parameters for water contents only, not the vessel shell. */
 public static Optional<dev.minemods.hydration.WaterPhysics.Properties> liquidPhysics(ItemStack stack){
  if(stack.getItem() instanceof VesselItem v)return Optional.of(dev.minemods.hydration.WaterPhysics.batch(v.read(stack)));
  return WaterBottle.read(stack).map(dev.minemods.hydration.WaterPhysics::batch);
 }
 public static Optional<dev.minemods.hydration.WaterPhysics.Properties> tankPhysics(ServerLevel level,BlockPos pos){
  if(!level.getServer().isSameThread())throw new IllegalStateException("Server thread required");
  if(!level.hasChunkAt(pos)||!(level.getBlockEntity(pos) instanceof TankEntity tank))return Optional.empty();
  return Optional.of(dev.minemods.hydration.WaterPhysics.batch(tank.water()));
 }
 public record Physiology(double hydrationPercent,double electrolyteIndex,double exposurePoints,String dehydrationStage){}
 public static Optional<Physiology> physiology(ServerPlayer p){
  if(!p.serverLevel().getServer().isSameThread())throw new IllegalStateException("Server thread required");
  if(!Hydration.active(p))return Optional.empty();
  var s=Hydration.state(p);return Optional.of(new Physiology(s.water(),s.electrolytes(),s.exposure(),dev.minemods.hydration.WaterBalance.stage(s.water()).name()));
 }
 public static final int BUCKET_MB=dev.minemods.hydration.FluidUnits.BUCKET_MB;
 public record Status(double hydrationPercent,double reservePoints,double pendingPoints,double exposurePoints){}
 public record LiquidStatus(int volumeMb,String quality,double salinityGkg,double contaminationRisk,Set<String> origins){
  public LiquidStatus{origins=Set.copyOf(origins);}
  /** Legacy accessor; same numeric quantity, now expressed in mB. */
  @Deprecated public int volumeMl(){return volumeMb;}
 }
 /** Reads an existing bottle snapshot; does not collect water or mutate a stack. */
 public static Optional<LiquidStatus> bottleWater(ItemStack stack){return WaterBottle.read(stack).map(b->new LiquidStatus(b.mB(),b.quality().name(),b.salinity(),b.risk(),b.origins()));}
 /** Reads a bottle or any Hydration vessel without mutating its data. */
 public static Optional<LiquidStatus> liquidWater(ItemStack stack){
  if(stack.getItem() instanceof VesselItem v){var b=v.read(stack);return Optional.of(new LiquidStatus(b.mB(),b.quality().name(),b.salinity(),b.risk(),b.origins()));}
  return bottleWater(stack);
 }
 /** Server-thread-only read of a loaded placed tank; no chunk loading. */
 public static Optional<LiquidStatus> tankWater(ServerLevel level,BlockPos pos){
  if(!level.getServer().isSameThread())throw new IllegalStateException("Server thread required");
  if(!level.hasChunkAt(pos)||!(level.getBlockEntity(pos) instanceof TankEntity tank))return Optional.empty();
  var b=tank.water();return Optional.of(new LiquidStatus(b.mB(),b.quality().name(),b.salinity(),b.risk(),b.origins()));
 }
 private HydrationApi(){}
 public static Optional<Status> status(ServerPlayer p){
  if(!p.serverLevel().getServer().isSameThread())throw new IllegalStateException("Server thread required");
  if(!Hydration.active(p))return Optional.empty();
  var s=Hydration.state(p);return Optional.of(new Status(s.water(),s.reserve(),s.pending(),s.exposure()));
 }
}
