package dev.minemods.worldlogic;
import java.lang.reflect.Method;
import java.util.*;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;

/** Optional migration bridge to the actual versioned public peer methods; never writes foreign state. */
public final class PeerPhysiology {
 private PeerPhysiology(){}
 private record Peer(String mod,String api,int revision,String method,boolean nutrition){}
 private static final Peer METABOLIC_REST=new Peer("metabolism","dev.minemods.metabolism.api.MetabolismApi",6,"restingRecoveryPointsPerSecond",false);
 private static final List<Peer> HEAL=List.of(
  new Peer("worldlogic_trauma","dev.minemods.traumacraft.api.TraumaApi",7,"adminRestore",false),
  new Peer("hydration","dev.minemods.hydration.api.HydrationApi",5,"adminRestore",false),
  new Peer("worldlogic_thermal","dev.minemods.thermal.api.ThermalApi",4,"adminRestore",false),
  new Peer("metabolism","dev.minemods.metabolism.api.MetabolismApi",6,"restoreNutrition",true));
 private static final Map<Peer,Optional<Method>> METHODS=new HashMap<>();
 private static final Set<Peer> WARNED=new HashSet<>();
 private static Optional<Method> method(Peer peer){
  if(!ModList.get().isLoaded(peer.mod()))return Optional.empty();
  return METHODS.computeIfAbsent(peer,p->{
   try{
    var c=Class.forName(p.api());
    if(c.getField("API_VERSION").getInt(null)!=1||c.getField("API_REVISION").getInt(null)<p.revision())return Optional.empty();
    var m=p.nutrition()?c.getMethod(p.method(),ServerPlayer.class,boolean.class):c.getMethod(p.method(),ServerPlayer.class);
    if(!java.lang.reflect.Modifier.isStatic(m.getModifiers()))return Optional.empty();
    if(m.getReturnType()!=(p.equals(METABOLIC_REST)?double.class:boolean.class))return Optional.empty();
    return Optional.of(m);
   }catch(ReflectiveOperationException|LinkageError|RuntimeException ex){warn(p,ex);return Optional.empty();}
  });
 }
 private static void warn(Peer p,Throwable ex){
  if(WARNED.add(p))org.slf4j.LoggerFactory.getLogger("WorldLogic").warn("Optional physiology endpoint unavailable: "+p.mod()+"/"+p.method(),ex);
 }
 public static boolean usesMetabolicRestRecovery(ServerPlayer player){return !(player instanceof net.neoforged.neoforge.common.util.FakePlayer)&&method(METABOLIC_REST).isPresent();}
 static OptionalDouble restRate(ServerPlayer player){
  if(!usesMetabolicRestRecovery(player))return OptionalDouble.empty();
  var m=method(METABOLIC_REST);if(m.isEmpty())return OptionalDouble.empty();
  try{double rate=((Number)m.get().invoke(null,player)).doubleValue();return Double.isFinite(rate)&&rate>=0&&rate<=100?OptionalDouble.of(rate):OptionalDouble.empty();}
  catch(ReflectiveOperationException|LinkageError|RuntimeException ex){warn(METABOLIC_REST,ex);return OptionalDouble.empty();}
 }
 /** Read-only inspection of actual installed public reset and rest endpoints, not a gameplay test. */
 static List<String> missingEndpoints(){
  var missing=new ArrayList<String>();
  for(var peer:HEAL)if(ModList.get().isLoaded(peer.mod())&&method(peer).isEmpty())missing.add(peer.mod()+"/"+peer.method());
  if(ModList.get().isLoaded(METABOLIC_REST.mod())&&method(METABOLIC_REST).isEmpty())missing.add(METABOLIC_REST.mod()+"/"+METABOLIC_REST.method());
  return List.copyOf(missing);
 }
 /** Assert optional absent/installed endpoint negotiation and the explicit result of each actual reset attempt. */
 static void verifyNegotiation(ServerPlayer player,HealResult result){
  for(var peer:HEAL){
   boolean installed=ModList.get().isLoaded(peer.mod());
   boolean restored=result.restored().contains(peer.mod());
   boolean unavailable=result.unavailable().stream().anyMatch(x->x.startsWith(peer.mod()+" ("));
   if(installed!=(restored||unavailable)||restored&&unavailable)
    throw new IllegalStateException("heal provider result not attributed: "+peer.mod());
   if(method(peer).isEmpty()&&restored)throw new IllegalStateException("absent or incompatible provider reported restored: "+peer.mod());
  }
  if(usesMetabolicRestRecovery(player))throw new IllegalStateException("FakePlayer must not own native metabolic rest");
  if(!restRate(player).isEmpty())throw new IllegalStateException("FakePlayer must retain standalone rest");
 }
 public record HealResult(List<String> restored,List<String> unavailable){
  public HealResult{restored=List.copyOf(restored);unavailable=List.copyOf(unavailable);}
  public boolean complete(){return unavailable.isEmpty();}
 }
 public static HealResult heal(ServerPlayer player){
  if(!player.serverLevel().getServer().isSameThread())throw new IllegalStateException("server thread required");
  if(!player.isAlive()||player.isCreative()||player.isSpectator())return new HealResult(List.of(),List.of("player is dead or not in survival/adventure"));
  var done=new ArrayList<String>();var missing=new ArrayList<String>();
  player.removeAllEffects();
  player.setHealth(player.getMaxHealth());player.getFoodData().setFoodLevel(20);player.getFoodData().setSaturation(20);player.getFoodData().setExhaustion(0);
  player.setAirSupply(player.getMaxAirSupply());player.clearFire();player.setTicksFrozen(0);player.fallDistance=0;
  Survival.adminRestore(player);Trauma.adminRestore(player);Balance.adminRestore(player);Body.adminRestore(player);CombatConsequences.adminRestore(player);
  GroundHazards.adminRestore(player);ImpactDynamics.adminRestore(player);TravelPhysics.adminRestore(player);
  done.add("vanilla/Core");
  for(var peer:HEAL){
   if(!ModList.get().isLoaded(peer.mod()))continue;
   var m=method(peer);
   if(m.isEmpty()){missing.add(peer.mod()+" (API unavailable)");continue;}
   try{
    Object result=peer.nutrition()?m.get().invoke(null,player,true):m.get().invoke(null,player);
    if(Boolean.TRUE.equals(result))done.add(peer.mod());else missing.add(peer.mod()+" (reset declined)");
   }catch(ReflectiveOperationException|LinkageError|RuntimeException ex){warn(peer,ex);missing.add(peer.mod()+" (reset failed)");}
  }
  player.setHealth(player.getMaxHealth());RuleSync.send(player);
  return new HealResult(done,missing);
 }
}
