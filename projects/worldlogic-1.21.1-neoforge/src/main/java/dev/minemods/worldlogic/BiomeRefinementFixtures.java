package dev.minemods.worldlogic;
import java.util.List;
final class BiomeRefinementFixtures {
 record Fixture(String id,double temperature,double humidity,double continentalness,double erosion,double depth,double weirdness,String group){}
 static final List<Fixture> VALUES=List.of(
  new Fixture("worldlogic:polar_gravel_desert",-0.875,-0.5700000000000001,0.5775,0.54,0,0,"polar"),
  new Fixture("worldlogic:dry_tundra",-0.875,-0.4,0.5775,0.54,0,0,"tundra"),
  new Fixture("worldlogic:light_conifer_taiga",-0.385,-0.14,0.5775,0.54,0,0,"boreal"),
  new Fixture("worldlogic:continental_taiga",-0.385,0.21,0.5775,0.54,0,0,"boreal"),
  new Fixture("worldlogic:dark_conifer_taiga",-0.385,0.39,0.5775,0.54,0,0,"boreal"),
  new Fixture("worldlogic:shortgrass_steppe",-0.03,-0.47500000000000003,0.5775,0.54,0,0,"grassland"),
  new Fixture("worldlogic:prairie",-0.03,0.06,0.5775,0.54,0,0,"grassland"),
  new Fixture("worldlogic:temperate_deciduous_forest",-0.03,0.39,0.72,0.54,0,0,"temperate_forest"),
  new Fixture("worldlogic:temperate_marsh",-0.03,0.42,0.54,0.59,0,0,"wetland"),
  new Fixture("worldlogic:temperate_marsh",0.26,0.60,0.54,0.59,0,0,"wetland"),
  new Fixture("worldlogic:temperate_deciduous_forest",-0.03,0.32,0.54,0.59,0,0,"temperate_forest"),
  new Fixture("worldlogic:sclerophyll_woodland",0.27,-0.31,0.5775,0.54,0,0,"scrub"),
  new Fixture("worldlogic:rock_desert",0.79,-0.56,0.5775,0.54,0,0,"desert"),
  new Fixture("worldlogic:seasonal_tropical_forest",0.79,0.25,0.5775,0.54,0,0,"tropical_forest")
 );
}
