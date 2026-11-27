package dev.minemods.hydration.api;
import java.util.Optional;
import dev.minemods.hydration.Hydration;
import dev.minemods.hydration.WaterBottle;
import net.minecraft.world.item.ItemStack;
import java.util.Set;
import net.minecraft.server.level.ServerPlayer;

/** Public server-thread API. Hydration owns fluid balance, never foreign body state. */
public final class HydrationApi {
 public static final int API_VERSION=1;
 public record Status(double hydrationPercent,double reservePoints,double pendingPoints,double exposurePoints){}
 public record LiquidStatus(int volumeMl,String quality,double salinityGkg,double contaminationRisk,Set<String> origins){public LiquidStatus{origins=Set.copyOf(origins);}}
 /** Reads an existing bottle snapshot; does not collect water or mutate a stack. */
 public static Optional<LiquidStatus> bottleWater(ItemStack stack){return WaterBottle.read(stack).map(b->new LiquidStatus(b.ml(),b.quality().name(),b.salinity(),b.risk(),b.origins()));}
 private HydrationApi(){}
 public static Optional<Status> status(ServerPlayer p){
  if(!p.serverLevel().getServer().isSameThread())throw new IllegalStateException("Server thread required");
  if(!Hydration.active(p))return Optional.empty();
  var s=Hydration.state(p);return Optional.of(new Status(s.water(),s.reserve(),s.pending(),s.exposure()));
 }
}
