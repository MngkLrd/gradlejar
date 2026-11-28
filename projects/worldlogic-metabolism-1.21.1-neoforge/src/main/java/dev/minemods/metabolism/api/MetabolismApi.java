package dev.minemods.metabolism.api;
import dev.minemods.metabolism.Metabolism;
import net.minecraft.server.level.ServerPlayer;
public final class MetabolismApi {
 public static final int API_VERSION=1,API_REVISION=5;
 public record NutrientStatus(String id,double adequacy,double trend){}
 public record Digestion(double fastKcal,double fatKcal,double queuedKcal,double kcalPerGameHour,int deficient,int critical,double estimatedFastHours,double estimatedTotalHours){}
 public static java.util.List<NutrientStatus> nutrients(ServerPlayer player){if(!player.serverLevel().getServer().isSameThread())throw new IllegalStateException("server thread required");var s=Metabolism.state(player);var out=new java.util.ArrayList<NutrientStatus>();for(int i=0;i<dev.minemods.metabolism.MetabolicModel.N;i++)out.add(new NutrientStatus(dev.minemods.metabolism.MetabolicModel.KEYS[i],s.adequacy(i),s.trend(i)));return java.util.List.copyOf(out);}
 public static Digestion digestion(ServerPlayer player){if(!player.serverLevel().getServer().isSameThread())throw new IllegalStateException("server thread required");var s=Metabolism.state(player);return new Digestion(s.fast,s.reserve,s.stomach,s.burn,s.deficient(),s.critical(),s.fastHours(),s.totalHours());}
 public record Physiology(double dietQuality,double availableEnergy,double recoveryMultiplier,double resistanceMultiplier,double toxinLoad,double absorptionEfficiency){}
 public static Physiology physiology(ServerPlayer player){
  if(!player.serverLevel().getServer().isSameThread())throw new IllegalStateException("server thread required");
  var s=Metabolism.state(player);return new Physiology(s.quality(),s.energy(),s.recovery(),s.resistance(),s.toxin,s.absorptionEfficiency());
 }
 public record Clinical(double tissue,double bone,double blood,double fractureThreshold,double resistance,double toxin){}
 public static Clinical clinical(ServerPlayer p){
  if(!p.serverLevel().getServer().isSameThread())throw new IllegalStateException("server thread required");
  if(!p.isAlive()||p.isCreative()||p.isSpectator())return new Clinical(1,1,1,1,1,0);
  var s=Metabolism.state(p);return new Clinical(s.tissueFactor(),s.boneFactor(),s.bloodFactor(),s.fractureThresholdFactor(),s.resistance(),s.toxin);
 }
 public record Energy(double metabolicReserveFraction,double coreStaminaPoints,double stomachFraction,double overeating,double lastIntakePoints,double lastDigestionPoints,int waterCooldownSeconds){}
 public static Energy energy(ServerPlayer p){
  thread(p);var s=Metabolism.state(p);var fatigue=dev.minemods.worldlogic.api.WorldLogicApi.fatigue(p);
  return new Energy(s.energy(),fatigue.enabled()?fatigue.remainingStaminaPercent():-1,s.fullness(),s.overload,s.lastInstant,s.lastDigest,s.drinkCooldown);
 }
 /** Actual completed non-item food portion only; ordinary FOOD and vanilla cake already handled. Server thread. */
 public static Metabolism.IntakeResult reportFoodIntake(ServerPlayer p,double kcal,java.util.Map<String,Double> nutrients,double toxin,double absorption){
  thread(p);if(!Double.isFinite(kcal)||kcal<=0||kcal>10000||nutrients==null||!Double.isFinite(toxin)||toxin<0||toxin>1||!Double.isFinite(absorption)||absorption<.15||absorption>1)throw new IllegalArgumentException("Invalid meal");
  var n=new double[dev.minemods.metabolism.MetabolicModel.N];
  for(var entry:nutrients.entrySet()){int i=java.util.Arrays.asList(dev.minemods.metabolism.MetabolicModel.KEYS).indexOf(entry.getKey());Double v=entry.getValue();if(i<0||v==null||!Double.isFinite(v)||v<0||v>dev.minemods.metabolism.MetabolicModel.DAILY[i]*4)throw new IllegalArgumentException("Invalid nutrient");n[i]=v;}
  return Metabolism.acceptMeal(p,new dev.minemods.metabolism.MetabolicModel.Meal(kcal,n,toxin,absorption),true);
 }
 /** Owner reports successful water-only intake once, not food moisture. Actual stamina points; no kcal or hydration mutation. */
 public static double reportWaterIntake(ServerPlayer p,int volumeMb,double contaminationRisk,double salinityGkg){
  thread(p);return Metabolism.acceptWater(p,volumeMb,contaminationRisk,salinityGkg);
 }
 private static void thread(ServerPlayer p){if(p==null||!p.serverLevel().getServer().isSameThread())throw new IllegalStateException("server thread required");}
 /** Nutrient composition per actual item serving. Unknown modded macros are absent, not zero. */
 public record FoodProfile(boolean known,double kcal,Double proteinG,Double fatG,Double availableCarbG,Double fiberG,java.util.Map<String,Double> micronutrients,double toxin,double absorption){
  public FoodProfile{micronutrients=java.util.Map.copyOf(micronutrients);}
 }
 public static java.util.Optional<FoodProfile> foodProfile(net.minecraft.world.item.ItemStack stack,net.minecraft.world.entity.LivingEntity eater){
  if(stack==null||stack.isEmpty())return java.util.Optional.empty();
  return dev.minemods.metabolism.FoodProfiles.profile(stack,eater).map(e->{var m=e.meal();var n=m.nutrients();var micro=new java.util.LinkedHashMap<String,Double>();if(e.known())for(int i=4;i<dev.minemods.metabolism.MetabolicModel.N;i++)micro.put(dev.minemods.metabolism.MetabolicModel.KEYS[i],n[i]);return new FoodProfile(e.known(),m.kcal(),e.known()?n[0]:null,e.known()?n[1]:null,e.known()?n[2]:null,e.known()?n[3]:null,micro,m.toxin(),m.absorption());});
 }
 private MetabolismApi(){}
}
