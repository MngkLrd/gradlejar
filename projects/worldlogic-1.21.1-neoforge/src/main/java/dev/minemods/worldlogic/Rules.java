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
 +"crop_crowding flood_stress scent herd_panic hot_meal spoilage heavy_items doors_have_mass weighted_redstone finite_air tree_weight snow_weight exertion injuries smoke_inhalation campfire_fuel soil_fatigue mine_instability interrupted_meals shield_fatigue armored_diving sheltered_sleep mining_noise combat_recovery fragile_cargo dangerous_retreat unstable_climbing armor_fatigue environmental_wear persistent_pursuit blood_trail predator_memory boat_overload heavy_swing leash_resistance wall_collision torch_lifetime physical_falls injured_balance cargo_burden weak_condition dangerous_darkness desperate_climb heavy_landing animal_hunger unstable_carrying tool_grip rough_terrain ground_recovery dynamic_knockdown emergency_grab rolling_descent chain_collision desperate_recovery unstable_footing combat_disarm landing_shockwave broken_guard desperate_dodge injury_aggravation shoulder_impact mob_weight dangerous_trees blast_concussion repeated_impact structural_stress water_pressure debris_impact ice_entrapment animal_stampede weapon_obstruction cramped_mining blast_chain_reaction thermal_expansion burning_structures modeled_weather regional_weather hydrology").split(" ");
 private static final Set<String> RISK=Set.of("campfire_sparks","wet_torches","thermal_shock","spoilage","glass_pressure","loose_gravel","arrow_recovery","falling_objects","snow_compression","weighted_redstone","tree_weight","snow_weight","mine_instability","fragile_cargo","torch_lifetime","unstable_carrying","animal_hunger","combat_disarm","dangerous_trees","structural_stress","debris_impact","blast_chain_reaction","thermal_expansion","burning_structures");
 private static final Set<String> LIGHT_OFF=Set.of("finite_air","cold_shock","wet_gear","exhausted_sprint","ladder_grip","fire_needs_air","flood_stress","crop_crowding","sharp_blocks","exertion","injuries","smoke_inhalation","campfire_fuel","soil_fatigue","mine_instability","interrupted_meals","shield_fatigue","armored_diving","sheltered_sleep","mining_noise","combat_recovery","fragile_cargo","dangerous_retreat","unstable_climbing","armor_fatigue","environmental_wear","persistent_pursuit","blood_trail","predator_memory","boat_overload","heavy_swing","leash_resistance","wall_collision","torch_lifetime","physical_falls","injured_balance","cargo_burden","weak_condition","dangerous_darkness","desperate_climb","heavy_landing","animal_hunger","unstable_carrying","tool_grip","rough_terrain","ground_recovery","dynamic_knockdown","emergency_grab","rolling_descent","chain_collision","desperate_recovery","unstable_footing","combat_disarm","landing_shockwave","broken_guard","desperate_dodge","injury_aggravation","shoulder_impact","mob_weight","dangerous_trees","blast_concussion","repeated_impact","structural_stress","water_pressure","debris_impact","ice_entrapment","animal_stampede","weapon_obstruction","cramped_mining","blast_chain_reaction","thermal_expansion","burning_structures");
 private static final Map<String,Double> DEFAULTS=Map.of("impact",1.0,"movement",1.0,"air",1.0,"weather",1.0,"fire",1.0,"ecology",1.0);
 private static volatile Map<String,String> values=Map.of();
 public static List<String> keys(){return List.of(KEYS);}
 public static List<String> tuningKeys(){return DEFAULTS.keySet().stream().sorted().toList();}
 private static Path path(){return FMLPaths.CONFIGDIR.get().resolve("worldlogic.properties");}
 public static Map<String,String> preset(String name){
  if(!Set.of("light","realistic","hardcore").contains(name))throw new IllegalArgumentException("Unknown preset");
  Map<String,String> m=new LinkedHashMap<>();m.put("preset",name);
  for(String k:KEYS)m.put(k,Boolean.toString(!k.equals("modeled_weather")&&(name.equals("hardcore")||(!RISK.contains(k)&&(!name.equals("light")||!LIGHT_OFF.contains(k))))));
  for(String k:tuningKeys())m.put("strength."+k,Double.toString(name.equals("light")?.65:name.equals("hardcore")?1.35:1));
  return m;
 }
 public static synchronized void load(){
  Properties p=new Properties();Path path=path();
  try{if(Files.exists(path))try(var in=Files.newInputStream(path)){p.load(in);}}catch(java.io.IOException ex){org.slf4j.LoggerFactory.getLogger("WorldLogic").warn("Cannot load config",ex);}
  String mode=p.getProperty("preset","realistic");if(!Set.of("light","realistic","hardcore","custom").contains(mode))mode="realistic";
  Map<String,String> m=new LinkedHashMap<>(preset(mode.equals("custom")?"realistic":mode));m.put("preset",mode);
  for(String k:KEYS){String v=p.getProperty(k);if("true".equalsIgnoreCase(v)||"false".equalsIgnoreCase(v))m.put(k,v.toLowerCase(Locale.ROOT));}
  for(String k:tuningKeys()){String v=p.getProperty("strength."+k);if(v!=null)try{double d=Double.parseDouble(v);if(Double.isFinite(d))m.put("strength."+k,Double.toString(Math.max(.25,Math.min(2,d))));}catch(NumberFormatException ignored){}}
  values=Map.copyOf(m);if(!Files.exists(path))save(m);
 }
 public static Map<String,String> snapshot(){return new LinkedHashMap<>(values);}
 public static synchronized boolean apply(Map<String,String> proposed){
  Map<String,String> next=new LinkedHashMap<>(preset("realistic"));
  String name=proposed.getOrDefault("preset","custom");next.put("preset",Set.of("light","realistic","hardcore","custom").contains(name)?name:"custom");
  for(String k:KEYS)next.put(k,Boolean.toString(Boolean.parseBoolean(proposed.getOrDefault(k,next.get(k)))));
  for(String k:tuningKeys()){String key="strength."+k;double d=1;try{d=Double.parseDouble(proposed.getOrDefault(key,"1"));}catch(NumberFormatException ignored){}next.put(key,Double.toString(Double.isFinite(d)?Math.max(.25,Math.min(2,d)):1));}
  if(!save(next))return false;values=Map.copyOf(next);return true;
 }
 private static boolean save(Map<String,String> m){
  try{Path p=path();Files.createDirectories(p.getParent());Path tmp=Files.createTempFile(p.getParent(),"worldlogic",".tmp");try{Properties out=new Properties();out.putAll(m);try(var os=Files.newOutputStream(tmp)){out.store(os,"World Logic settings. Server commands or Mods > World Logic > Config.");}try{Files.move(tmp,p,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}catch(AtomicMoveNotSupportedException e){Files.move(tmp,p,StandardCopyOption.REPLACE_EXISTING);}return true;}finally{Files.deleteIfExists(tmp);}}catch(java.io.IOException ex){org.slf4j.LoggerFactory.getLogger("WorldLogic").warn("Cannot save config",ex);return false;}
 }
 public static boolean on(String key){return Boolean.parseBoolean(values.getOrDefault(key,"false"));}
 public static double strength(String family){try{return Double.parseDouble(values.getOrDefault("strength."+family,"1"));}catch(NumberFormatException e){return 1;}}
 public static double chance(String family,double base){return Math.max(0,Math.min(1,base*strength(family)));}
}
