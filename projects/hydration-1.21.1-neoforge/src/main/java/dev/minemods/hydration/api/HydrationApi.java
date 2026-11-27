package dev.minemods.hydration.api;
import java.util.Optional;
import dev.minemods.hydration.Hydration;
import dev.minemods.hydration.WaterBalance;
import net.minecraft.server.level.ServerPlayer;

/** Public server-thread API. Hydration owns fluid balance, never foreign body state. */
public final class HydrationApi {
 public static final int API_VERSION=1;
 public record Status(double hydrationPercent,double reservePoints,double pendingPoints,double exposurePoints){}
 private HydrationApi(){}
 public static Optional<Status> status(ServerPlayer p){
  if(!p.serverLevel().getServer().isSameThread())throw new IllegalStateException("Server thread required");
  if(!Hydration.active(p))return Optional.empty();
  var s=Hydration.state(p);return Optional.of(new Status(s.water(),s.reserve(),s.pending(),s.exposure()));
 }
}
