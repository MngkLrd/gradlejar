package dev.minemods.metabolism.api;
import dev.minemods.metabolism.Metabolism;
import net.minecraft.server.level.ServerPlayer;
public final class MetabolismApi {
 public static final int API_VERSION=1;
 public record Physiology(double dietQuality,double availableEnergy,double recoveryMultiplier,double resistanceMultiplier,double toxinLoad,double absorptionEfficiency){}
 public static Physiology physiology(ServerPlayer player){
  if(!player.serverLevel().getServer().isSameThread())throw new IllegalStateException("server thread required");
  var s=Metabolism.state(player);return new Physiology(s.quality(),s.energy(),s.recovery(),s.resistance(),s.toxin,s.absorptionEfficiency());
 }
 private MetabolismApi(){}
}
