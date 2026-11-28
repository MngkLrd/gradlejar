package dev.minemods.worldlogic;
import com.mojang.brigadier.arguments.*;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import java.util.*;
public final class AdminCommands {
 private static int reply(CommandSourceStack s,String message){s.sendSuccess(()->Component.literal(message),false);return 1;}
 private static int change(CommandSourceStack s,Map<String,String> m){if(!Rules.apply(m)){s.sendFailure(Component.literal("World Logic: config write failed"));return 0;}return reply(s,"World Logic settings saved.");}
 public static void register(RegisterCommandsEvent event){
  var root=Commands.literal("worldlogic").requires(s->s.hasPermission(2)).executes(c->reply(c.getSource(),"World Logic: preset, rule, strength, reload, inspect, selftest"));
  var preset=Commands.literal("preset");for(String name:List.of("light","realistic","hardcore"))preset.then(Commands.literal(name).executes(c->change(c.getSource(),Rules.preset(name))));root.then(preset);
  root.then(Commands.literal("rule").then(Commands.argument("key",StringArgumentType.word()).suggests((c,b)->net.minecraft.commands.SharedSuggestionProvider.suggest(Rules.keys(),b)).then(Commands.argument("enabled",BoolArgumentType.bool()).executes(c->{String k=StringArgumentType.getString(c,"key");if(!Rules.keys().contains(k)){c.getSource().sendFailure(Component.literal("Unknown rule"));return 0;}var m=Rules.snapshot();m.put(k,Boolean.toString(BoolArgumentType.getBool(c,"enabled")));m.put("preset","custom");return change(c.getSource(),m);}))));
  root.then(Commands.literal("strength").then(Commands.argument("family",StringArgumentType.word()).suggests((c,b)->net.minecraft.commands.SharedSuggestionProvider.suggest(Rules.tuningKeys(),b)).then(Commands.argument("factor",DoubleArgumentType.doubleArg(.25,2)).executes(c->{String k=StringArgumentType.getString(c,"family");if(!Rules.tuningKeys().contains(k)){c.getSource().sendFailure(Component.literal("Unknown family"));return 0;}var m=Rules.snapshot();m.put("strength."+k,Double.toString(DoubleArgumentType.getDouble(c,"factor")));m.put("preset","custom");return change(c.getSource(),m);}))));
  root.then(Commands.literal("reload").executes(c->{Rules.load();return reply(c.getSource(),"World Logic configuration reloaded.");}));
  root.then(Commands.literal("inspect").executes(c->{var s=c.getSource();var p=s.getPlayer();if(p==null)return reply(s,"Use in game to inspect player state.");var b=Body.state(p);return reply(s,"mass="+String.format(Locale.ROOT,"%.2f",Material.mass(p))+" fatigue="+String.format(Locale.ROOT,"%.1f",Survival.fatigue(p))+" cargo="+String.format(Locale.ROOT,"%.2f",Condition.burden(p))+" impact_load="+String.format(Locale.ROOT,"%.1f",Trauma.load(p))+" arm_injury="+Trauma.arm(p)+" prone="+Balance.down(p)+" wetness="+b.wet+" air="+p.getAirSupply()+" streak="+b.streak+" pocket="+(b.pocket==null?"none":b.pocket.oxygen));}));
  root.then(Commands.literal("weather").executes(c->{
   var s=c.getSource();
   var pos=net.minecraft.core.BlockPos.containing(s.getPosition());
   var live=dev.minemods.worldlogic.api.WorldLogicApi.atmosphere(s.getLevel(),pos);
   var model=dev.minemods.worldlogic.api.WorldLogicApi.regionalWeatherPreview(s.getLevel(),pos);
   if(live.isEmpty()||model.isEmpty())return reply(s,"Weather unavailable at this position.");
   var a=live.orElseThrow();var w=model.orElseThrow();
   return reply(s,String.format(Locale.ROOT,"Live: %.1f C, RH %.0f%%, wind %.1f m/s, %s. Regional preview: %.1f hPa, anomaly %+.1f C, cloud %.0f%%, precipitation potential %.0f%%, gust %.1f m/s.",
    a.environment().ambientCelsiusEstimate(),a.relativeHumidityPercent(),a.outdoorWindSpeedMps(),a.precipitation(),w.localPressureHpa(),w.temperatureAnomalyC(),w.cloudFraction()*100,w.precipitationPotential()*100,w.gustMps()));
  }));
  root.then(Commands.literal("weather_column").executes(c->{
   var s=c.getSource();var pos=net.minecraft.core.BlockPos.containing(s.getPosition());
   var a=dev.minemods.worldlogic.api.WorldLogicApi.weatherColumnPreview(s.getLevel(),pos);
   if(a.isEmpty())return reply(s,"Weather column unavailable at this position.");
   var d=a.orElseThrow();
   return reply(s,String.format(Locale.ROOT,"Column preview: %.1f C, dew %.1f C, wet bulb %.1f C, vapor %.2f g/kg, LCL %.0f m, cloud %.0f-%.0f m, condensate %.2f g/kg, buoyancy %.0f J/kg, fog index %.2f.",
    d.airC(),d.dewPointC(),d.wetBulbC(),d.vaporMixingRatioGkg(),d.condensationLevelM(),d.cloudBaseM(),d.cloudTopM(),d.condensedWaterGkg(),d.buoyancyEnergyJkg(),d.fogPotential()));
  }));
  root.then(Commands.literal("testfall").executes(c->{var p=c.getSource().getPlayer();if(p!=null&&Balance.debug(p))return reply(c.getSource(),"World Logic: test fall triggered.");c.getSource().sendFailure(Component.literal("Enable physical_falls and use in survival."));return 0;}));
  root.then(Commands.literal("selftest").executes(c->{try{ReleaseChecks.run();return reply(c.getSource(),"WORLDLOGIC_CORE_TESTS_PASSED");}catch(Exception e){c.getSource().sendFailure(Component.literal(e.toString()));return 0;}}));
  root.then(Commands.literal("weather_grid").executes(c->{
   var s=c.getSource();var pos=net.minecraft.core.BlockPos.containing(s.getPosition());
   var a=dev.minemods.worldlogic.api.WorldLogicApi.transportGridPreview(s.getLevel(),pos);
   if(a.isEmpty())return reply(s,"Weather grid not active at this position.");
   var v=a.orElseThrow();return reply(s,String.format(Locale.ROOT,"Grid preview: %.1f hPa, %.1f C, %.2f g/kg vapor, %.0f%% RH, tick %d",v.pressureHpa(),v.airC(),v.vaporGkg(),v.relativeHumidityPercent(),v.modeledTick()));
  }));
  var demo=Commands.literal("weather_demo").requires(s->s.hasPermission(4));
  for(var kind:dev.minemods.worldlogic.api.WorldLogicApi.WeatherKind.values())
   demo.then(Commands.literal(kind.name().toLowerCase(java.util.Locale.ROOT)).executes(c->{
    var s=c.getSource();
    return reply(s,LocalWeatherRuntime.demo(s.getLevel(),net.minecraft.core.BlockPos.containing(s.getPosition()),kind)?
      "Local weather demo: "+kind+" (128 block radius, 120 seconds)":"Local weather demo requires active geographic preset and a loaded position.");
   }));
  demo.then(Commands.literal("stop").executes(c->{LocalWeatherRuntime.clearDemo(c.getSource().getLevel());return reply(c.getSource(),"Local weather demo stopped.");}));
  root.then(demo);
  root.then(Commands.literal("weather_local").executes(c->{
   var s=c.getSource();var w=dev.minemods.worldlogic.api.WorldLogicApi.localWeather(s.getLevel(),net.minecraft.core.BlockPos.containing(s.getPosition()));
   return reply(s,w.map(v->String.format(java.util.Locale.ROOT,"Local weather: %s, intensity %.0f%%, air delta %+.1f C, RH %.0f%%, gust %.1f m/s, fog %.0f%%, exposed=%s",v.kind(),100*v.intensity(),v.temperatureDeltaC(),v.relativeHumidityPercent(),v.gustMps(),100*v.fogDensity(),v.exposed())).orElse("Local weather inactive (unsupported world, unloaded point or regional_weather disabled)."));
  }));
  root.then(Commands.literal("hydrology").executes(c->{var s=c.getSource();var pos=net.minecraft.core.BlockPos.containing(s.getPosition());
   var h=dev.minemods.worldlogic.api.WorldLogicApi.hydrology(s.getLevel(),pos);
   return reply(s,h.map(v->String.format(java.util.Locale.ROOT,"Soil %s; saturation %.0f%%; water %.1f/%.1f mm; groundwater %.1f mm; snow %.1f mm; runoff %.2f mm; simulated=%s",v.soil(),v.saturation()*100,v.soilWaterMm(),v.capacityMm(),v.groundwaterMm(),v.snowWaterMm(),v.lastRunoffMm(),v.simulated())).orElse("Hydrology inactive or no soil at loaded cell."));
  }));
  root.then(Commands.literal("water").executes(c->{var s=c.getSource();
   var p=net.minecraft.core.BlockPos.containing(s.getPosition());
   var w=dev.minemods.worldlogic.api.WorldLogicApi.waterProfile(s.getLevel(),p);
   return reply(s,String.format(java.util.Locale.ROOT,"Water: %s; quality=%s; salinity=%.1f g/kg; risk=%.2f; evidence=%s",w.kind(),w.quality(),w.salinityGkg(),w.contaminationRisk(),w.evidence()));
  }));
  root.then(Commands.literal("qa").requires(s->s.hasPermission(4))
   .then(Commands.literal("analytics").executes(c->{try{AnalyticsChecks.verify(c.getSource().getLevel());return reply(c.getSource(),"WORLDLOGIC_ANALYTICS_PASSED");}catch(Exception e){return reply(c.getSource(),"FAIL: "+e);}}))
   .then(Commands.literal("native_water").executes(c->{try{NaturalWaterChecks.verify(c.getSource().getLevel());return reply(c.getSource(),"WORLDLOGIC_NATIVE_WATER_PASSED");}catch(Exception e){return reply(c.getSource(),"FAIL: "+e);}}))
   .then(Commands.literal("water_profile").executes(c->{try{WaterClassificationChecks.verify(c.getSource().getLevel());return reply(c.getSource(),"WORLDLOGIC_WATER_PROFILE_PASSED");}catch(Exception e){return reply(c.getSource(),"FAIL: "+e);}}))
   .then(Commands.literal("hydrology").executes(c->{try{HydrologyChecks.verify(c.getSource().getLevel());return reply(c.getSource(),"WORLDLOGIC_HYDROLOGY_PASSED");}catch(Exception e){return reply(c.getSource(),"FAIL: "+e);}}))
   .then(Commands.literal("local_weather").executes(c->{try{LocalWeatherChecks.verify(c.getSource().getLevel());return reply(c.getSource(),"WORLDLOGIC_LOCAL_WEATHER_PASSED");}catch(Exception e){return reply(c.getSource(),"FAIL: "+e);}}))
   .then(Commands.literal("worldgen_ecology").executes(c->{try{WorldgenApiChecks.verify(c.getSource().getLevel());return reply(c.getSource(),"WORLDLOGIC_WORLDGEN_ECOLOGY_PASSED");}catch(Exception e){return reply(c.getSource(),"FAIL: "+e);}}))
   .then(Commands.literal("ore_balance").executes(c->{try{GeologyChecks.verifyBalance(c.getSource().getLevel());return reply(c.getSource(),"WORLDLOGIC_ORE_BALANCE_PASSED");}catch(Exception e){return reply(c.getSource(),"FAIL: "+e);}}))
   .then(Commands.literal("karst").executes(c->{try{GeologyChecks.verifyKarst(c.getSource().getLevel());return reply(c.getSource(),"WORLDLOGIC_KARST_PASSED");}catch(Exception e){return reply(c.getSource(),"FAIL: "+e);}}))
   .then(Commands.literal("unified_a").executes(c->{try{BiomeChecks.verify(c.getSource().getLevel(),1234L);return reply(c.getSource(),"WORLDLOGIC_UNIFIED_PASSED");}catch(Exception e){return reply(c.getSource(),"FAIL: "+e);}}))
   .then(Commands.literal("unified_b").executes(c->{try{BiomeChecks.verify(c.getSource().getLevel(),-6487130996193510395L);return reply(c.getSource(),"WORLDLOGIC_UNIFIED_PASSED");}catch(Exception e){return reply(c.getSource(),"FAIL: "+e);}}))
   .then(Commands.literal("ore_bodies").executes(c->{try{GeologyChecks.verifyOres(c.getSource().getLevel());return reply(c.getSource(),"WORLDLOGIC_ORE_BODIES_PASSED");}catch(Exception e){return reply(c.getSource(),"FAIL: "+e);}}))
   .then(Commands.literal("geology").executes(c->{try{GeologyChecks.verify(c.getSource().getLevel());return reply(c.getSource(),"WORLDLOGIC_GEOLOGY_PASSED");}catch(Exception e){return reply(c.getSource(),"FAIL: "+e);}}))
   .then(Commands.literal("transport_grid").executes(c->{try{ApiChecks.verifyTransportGrid(c.getSource().getLevel());return reply(c.getSource(),"WORLDLOGIC_TRANSPORT_GRID_PASSED");}catch(Exception e){return reply(c.getSource(),"FAIL: "+e);}}))
   .then(Commands.literal("weather_controller").executes(c->{try{ApiChecks.verifyWeatherController(c.getSource().getLevel());return reply(c.getSource(),"WORLDLOGIC_WEATHER_CONTROLLER_PASSED");}catch(Exception e){return reply(c.getSource(),"FAIL: "+e);}}))
   .then(Commands.literal("recovery").executes(c->{try{ApiChecks.verifyRecovery(c.getSource().getLevel());return reply(c.getSource(),"WORLDLOGIC_RECOVERY_PASSED");}catch(Exception e){return reply(c.getSource(),"FAIL: "+e);}}))
   .then(Commands.literal("api").executes(c->{ApiChecks.verify(c.getSource().getLevel());return reply(c.getSource(),"WORLDLOGIC_API_CHECKS_PASSED");}))
   .then(Commands.literal("atmosphere_dry").executes(c->{try{ApiChecks.verifyAtmosphere(c.getSource().getLevel(),false);return reply(c.getSource(),"WORLDLOGIC_ATMOSPHERE_DRY_PASSED");}catch(Exception e){return reply(c.getSource(),"FAIL: "+e);}}))
   .then(Commands.literal("atmosphere_rain").executes(c->{try{ApiChecks.verifyAtmosphere(c.getSource().getLevel(),true);return reply(c.getSource(),"WORLDLOGIC_ATMOSPHERE_RAIN_PASSED");}catch(Exception e){return reply(c.getSource(),"FAIL: "+e);}}))
   .then(Commands.literal("regional_weather").executes(c->{try{ApiChecks.verifyRegionalWeather(c.getSource().getLevel());return reply(c.getSource(),"WORLDLOGIC_REGIONAL_WEATHER_PASSED");}catch(Exception e){return reply(c.getSource(),"FAIL: "+e);}}))
   .then(Commands.literal("weather_column").executes(c->{try{ApiChecks.verifyWeatherColumn(c.getSource().getLevel());return reply(c.getSource(),"WORLDLOGIC_WEATHER_COLUMN_PASSED");}catch(Exception e){return reply(c.getSource(),"FAIL: "+e);}}))
   .then(Commands.literal("terrain_seed_a").executes(c->{TerrainChecks.verifyPresetSeed(c.getSource().getLevel(),1234L);return reply(c.getSource(),"WORLDLOGIC_TERRAIN_SEED_PASSED");}))
   .then(Commands.literal("terrain_seed_b").executes(c->{TerrainChecks.verifyPresetSeed(c.getSource().getLevel(),-6487130996193510395L);return reply(c.getSource(),"WORLDLOGIC_TERRAIN_SEED_PASSED");}))
   .then(Commands.literal("terrain").executes(c->{TerrainChecks.verifyPreset(c.getSource().getLevel());return reply(c.getSource(),"WORLDLOGIC_TERRAIN_PRESET_PASSED");}))
   .then(Commands.literal("world").executes(c->{ReleaseChecks.verifyWorld(c.getSource().getLevel());return reply(c.getSource(),"WORLDLOGIC_WORLD_FIXTURE_PASSED");})));
  event.getDispatcher().register(root);
 }
}
