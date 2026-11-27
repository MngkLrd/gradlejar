package dev.minemods.metabolism.api;
import dev.minemods.metabolism.Metabolism;
import net.minecraft.server.level.ServerPlayer;
public final class MetabolismApi {
 public static final int API_VERSION=1,API_REVISION=2;
 public record NutrientStatus(String id,double adequacy,double trend){}
 public record Digestion(double fastKcal,double fatKcal,double queuedKcal,double kcalPerGameHour,int deficient,int critical,double estimatedFastHours,double estimatedTotalHours){}
 public static java.util.List<NutrientStatus> nutrients(ServerPlayer player){if(!player.serverLevel().getServer().isSameThread())throw new IllegalStateException("server thread required");var s=Metabolism.state(player);var out=new java.util.ArrayList<NutrientStatus>();for(int i=0;i<dev.minemods.metabolism.MetabolicModel.N;i++)out.add(new NutrientStatus(dev.minemods.metabolism.MetabolicModel.KEYS[i],s.adequacy(i),s.trend(i)));return java.util.List.copyOf(out);}
 public static Digestion digestion(ServerPlayer player){if(!player.serverLevel().getServer().isSameThread())throw new IllegalStateException("server thread required");var s=Metabolism.state(player);return new Digestion(s.fast,s.reserve,s.stomach,s.burn,s.deficient(),s.critical(),s.fastHours(),s.totalHours());}
 public record Physiology(double dietQuality,double availableEnergy,double recoveryMultiplier,double resistanceMultiplier,double toxinLoad,double absorptionEfficiency){}
 public static Physiology physiology(ServerPlayer player){
  if(!player.serverLevel().getServer().isSameThread())throw new IllegalStateException("server thread required");
  var s=Metabolism.state(player);return new Physiology(s.quality(),s.energy(),s.recovery(),s.resistance(),s.toxin,s.absorptionEfficiency());
 }
 private MetabolismApi(){}
}
