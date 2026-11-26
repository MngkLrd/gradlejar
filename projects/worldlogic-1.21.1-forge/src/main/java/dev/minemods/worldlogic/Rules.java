package dev.minemods.worldlogic;
import java.nio.file.*;
import java.util.*;
import net.minecraftforge.fml.loading.FMLPaths;
public final class Rules {
 private static final Properties P=new Properties();
 private static final String[] KEYS=("loose_gravel ricochet arrow_recovery falling_objects falling_dripstone fragile_containers fragile_lanterns glass_pressure "
 +"knockback_weight projectile_force shield_momentum creeper_shockwave sharp_blocks landing_recovery soft_landing "
 +"tool_momentum cobweb grip exhausted_sprint armor_noise breath_debt exhausted_breath cold_shock wet_bow "
 +"water_drag water_extinguish slippery_rain mud_tracks snow_compression rain_weight wind "
 +"campfire_sparks residual_heat thermal_shock fire_needs_air draft_smoke ashes wet_torches "
 +"crop_crowding flood_stress scent herd_panic hot_meal spoilage heavy_items doors_have_mass weighted_redstone finite_air tree_weight snow_weight").split(" ");
 public static void load(){
  for(String k:KEYS) P.setProperty(k, String.valueOf(!Set.of("campfire_sparks","wet_torches","thermal_shock","spoilage","glass_pressure","loose_gravel","arrow_recovery","falling_objects","snow_compression","weighted_redstone","tree_weight","snow_weight").contains(k)));
  Path file=FMLPaths.CONFIGDIR.get().resolve("worldlogic.properties");
  try{Files.createDirectories(file.getParent()); if(Files.exists(file)){try(var in=Files.newInputStream(file)){P.load(in);}} else {try(var out=Files.newOutputStream(file)){P.store(out,"World Logic; restart to reload. Destructive/automation-altering rules default off.");}}}catch(java.io.IOException ex){org.slf4j.LoggerFactory.getLogger("WorldLogic").warn("Cannot load config; using defaults",ex);}
 }
 public static boolean on(String key){return Boolean.parseBoolean(P.getProperty(key,"false"));}
}
