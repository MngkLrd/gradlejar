package dev.minemods.worldlogic;
import java.nio.file.*;
import java.util.*;
import net.neoforged.fml.loading.FMLPaths;
public final class Rules {
 private static final String[] KEYS=("loose_gravel ricochet arrow_recovery falling_objects falling_dripstone fragile_containers fragile_lanterns glass_pressure "
 +"knockback_weight projectile_force shield_momentum creeper_shockwave sharp_blocks landing_recovery soft_landing "
 +"tool_momentum cobweb grip ladder_grip exhausted_sprint armor_noise breath_debt exhausted_breath cold_shock wet_bow wet_gear "
 +"water_drag water_extinguish slippery_rain mud_tracks snow_compression rain_weight wind "
 +"campfire_sparks residual_heat thermal_shock fire_needs_air draft_smoke ashes wet_torches "
 +"crop_crowding flood_stress scent herd_panic hot_meal spoilage heavy_items doors_have_mass weighted_redstone finite_air tree_weight snow_weight exertion injuries smoke_inhalation campfire_fuel soil_fatigue mine_instability interrupted_meals shield_fatigue armored_diving sheltered_sleep mining_noise combat_recovery fragile_cargo dangerous_retreat unstable_climbing armor_fatigue environmental_wear persistent_pursuit blood_trail predator_memory boat_overload heavy_swing leash_resistance wall_collision torch_lifetime physical_falls injured_balance cargo_burden weak_condition dangerous_darkness desperate_climb heavy_landing animal_hunger unstable_carrying tool_grip rough_terrain ground_recovery dynamic_knockdown emergency_grab rolling_descent chain_collision desperate_recovery unstable_footing combat_disarm landing_shockwave broken_guard desperate_dodge injury_aggravation shoulder_impact mob_weight dangerous_trees blast_concussion repeated_impact structural_stress water_pressure debris_impact ice_entrapment animal_stampede weapon_obstruction cramped_mining blast_chain_reaction thermal_expansion burning_structures modeled_weather regional_weather hydrology no_daytime_hostiles tree_descent ground_inertia downhill_falls failed_jump_falls").split(" ");
 private static final Map<String,Double> DEFAULTS=Map.of("impact",1.0,"movement",1.0,"air",1.0,"weather",1.0,"fire",1.0,"ecology",1.0);
 private static volatile Map<String,String> values=Map.of();
 public static List<String> keys(){return List.of(KEYS);}
 public static List<String> tuningKeys(){return DEFAULTS.keySet().stream().sorted().toList();}
 private static Path path(){return FMLPaths.CONFIGDIR.get().resolve("worldlogic.properties");}
 public static Map<String,String> preset(String name){
  if(!name.equals("realistic"))throw new IllegalArgumentException("Unknown preset");
  Map<String,String> m=new LinkedHashMap<>();m.put("preset",name);
  for(String k:KEYS)m.put(k,Boolean.toString(!Set.of("modeled_weather","downhill_falls","failed_jump_falls").contains(k)));
  for(String k:tuningKeys())m.put("strength."+k,Double.toString(1.0));
  return m;
 }
 /** Every restart restores the single realism baseline; old profile overrides must not disable mechanics. */
 public static synchronized void load(){
  Map<String,String> m=preset("realistic");
  values=Map.copyOf(m);
  save(m);
 }
 public static Map<String,String> snapshot(){return new LinkedHashMap<>(values);}
 /** Operator diagnostics only: overrides are never persisted and reload restores Realistic. */
 public static synchronized boolean apply(Map<String,String> proposed){
  Map<String,String> next=new LinkedHashMap<>(preset("realistic"));
  for(String k:KEYS)next.put(k,Boolean.toString(Boolean.parseBoolean(proposed.getOrDefault(k,next.get(k)))));
  for(String k:tuningKeys()){String key="strength."+k;double d=1;try{d=Double.parseDouble(proposed.getOrDefault(key,"1"));}catch(NumberFormatException ignored){}next.put(key,Double.toString(Double.isFinite(d)?Math.max(.25,Math.min(2,d)):1));}
  values=Map.copyOf(next);return true;
 }
 private static boolean save(Map<String,String> m){
  try{Path p=path();Files.createDirectories(p.getParent());Path tmp=Files.createTempFile(p.getParent(),"worldlogic",".tmp");try{Properties out=new Properties();out.putAll(m);try(var os=Files.newOutputStream(tmp)){out.store(os,"World Logic Realistic baseline. Operator diagnostic overrides are temporary.");}try{Files.move(tmp,p,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}catch(AtomicMoveNotSupportedException e){Files.move(tmp,p,StandardCopyOption.REPLACE_EXISTING);}return true;}finally{Files.deleteIfExists(tmp);}}catch(java.io.IOException ex){org.slf4j.LoggerFactory.getLogger("WorldLogic").warn("Cannot save config",ex);return false;}
 }
 public static boolean on(String key){
  if(!Boolean.parseBoolean(values.getOrDefault(key,"false")))return false;
  var mods=net.neoforged.fml.ModList.get();
  if(mods==null)return true;
  if((key.equals("tree_weight")||key.equals("dangerous_trees"))&&mods.isLoaded("wl_treechopper"))return false;
  if(key.equals("cold_shock")&&mods.isLoaded("worldlogic_thermal"))return false;
  if((key.equals("injuries")||key.equals("injury_aggravation")||key.equals("shoulder_impact"))&&mods.isLoaded("worldlogic_trauma"))return false;
  return true;
 }
 static String addonSummary(){
  var mods=net.neoforged.fml.ModList.get();
  var result=new StringJoiner("; ","World Logic ownership: ","");
  for(String id:List.of("worldlogic_thermal","worldlogic_trauma","wl_treechopper","hydration","worldlogicterrain","metabolism","worldlogic_aqua"))result.add(id+"="+(mods!=null&&mods.isLoaded(id)?"loaded":"absent"));
  return result+"; Core owns hydrology, soil, geology, friction, fall risk and stamina; Hydration owns fluid balance; Terrarian owns flora and continuous surface speed; Trauma owns anatomy; Thermal owns body temperature; Metabolism owns nutrition and rest rate; Aqua owns fish behavior; TreeChopper owns accepted chopping work";
 }
 public static double strength(String family){try{return Double.parseDouble(values.getOrDefault("strength."+family,"1"));}catch(NumberFormatException e){return 1;}}
 public static double chance(String family,double base){return Math.max(0,Math.min(1,base*strength(family)));}
}
