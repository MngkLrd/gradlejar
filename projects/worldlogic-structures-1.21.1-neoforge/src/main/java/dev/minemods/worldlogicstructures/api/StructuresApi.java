package dev.minemods.worldlogicstructures.api;

import dev.minemods.worldlogic.api.WorldLogicApi.WorldgenEnvironment;
import dev.minemods.worldlogic.api.WorldLogicCapabilities;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import java.util.*;

public final class StructuresApi {
 private StructuresApi(){}
 public static final String MOD_ID="worldlogic_structures";
 public static final WorldLogicCapabilities.Key<Catalogue> KEY=new WorldLogicCapabilities.Key<>(ResourceLocation.fromNamespaceAndPath(MOD_ID,"catalogue"),1,Catalogue.class);
 public enum Family implements StringRepresentable {
  COLD,BOREAL,TEMPERATE,ARID,TROPICAL,WETLAND,ALPINE,COAST;
  public String getSerializedName(){return name().toLowerCase(Locale.ROOT);}
  public ResourceLocation locateId(){return ResourceLocation.fromNamespaceAndPath(MOD_ID,getSerializedName());}
 }
 public enum Kind {HOUSE,TOWER,RUIN,PLATFORM}
 public enum Roof {GABLE,FLAT,SHED,NONE}
 public record Design(String id,Family family,Kind kind,int halfWidth,int halfDepth,int wallHeight,Roof roof,int raised){}
 public interface Catalogue {
  List<Design> designs();
  Optional<Family> family(WorldgenEnvironment habitat);
  default List<Design> designs(Family family){return designs().stream().filter(d->d.family()==family).toList();}
 }
 private static final List<Design> DESIGNS=List.of(
  new Design("winter_cabin",Family.COLD,Kind.HOUSE,3,4,3,Roof.GABLE,0),
  new Design("frozen_watchtower",Family.COLD,Kind.TOWER,2,2,7,Roof.GABLE,0),
  new Design("snow_shelter_ruin",Family.COLD,Kind.RUIN,3,2,2,Roof.NONE,0),
  new Design("taiga_loghouse",Family.BOREAL,Kind.HOUSE,4,4,3,Roof.GABLE,0),
  new Design("foresters_hut",Family.BOREAL,Kind.HOUSE,3,3,3,Roof.GABLE,0),
  new Design("timber_watchtower",Family.BOREAL,Kind.TOWER,2,2,7,Roof.GABLE,0),
  new Design("barn_remains",Family.BOREAL,Kind.RUIN,4,3,3,Roof.NONE,0),
  new Design("farmhouse",Family.TEMPERATE,Kind.HOUSE,4,4,4,Roof.GABLE,0),
  new Design("stone_watchtower",Family.TEMPERATE,Kind.TOWER,3,2,8,Roof.FLAT,0),
  new Design("abandoned_shed",Family.TEMPERATE,Kind.HOUSE,2,3,3,Roof.SHED,0),
  new Design("farmstead_ruin",Family.TEMPERATE,Kind.RUIN,4,4,3,Roof.NONE,0),
  new Design("adobe_house",Family.ARID,Kind.HOUSE,4,3,4,Roof.FLAT,0),
  new Design("sandstone_watchtower",Family.ARID,Kind.TOWER,2,2,7,Roof.FLAT,0),
  new Design("courtyard_remains",Family.ARID,Kind.RUIN,4,4,3,Roof.NONE,0),
  new Design("desert_lean_to",Family.ARID,Kind.HOUSE,3,2,3,Roof.SHED,0),
  new Design("jungle_stilt_house",Family.TROPICAL,Kind.HOUSE,3,4,3,Roof.GABLE,2),
  new Design("jungle_lookout",Family.TROPICAL,Kind.TOWER,2,2,7,Roof.GABLE,0),
  new Design("overgrown_ruin",Family.TROPICAL,Kind.RUIN,3,4,3,Roof.NONE,0),
  new Design("marsh_stilt_house",Family.WETLAND,Kind.HOUSE,3,3,3,Roof.GABLE,2),
  new Design("fishing_hut",Family.WETLAND,Kind.HOUSE,2,3,3,Roof.SHED,1),
  new Design("broken_boardwalk",Family.WETLAND,Kind.PLATFORM,2,4,1,Roof.NONE,1),
  new Design("mountain_refuge",Family.ALPINE,Kind.HOUSE,3,3,3,Roof.GABLE,0),
  new Design("alpine_watchtower",Family.ALPINE,Kind.TOWER,2,2,7,Roof.FLAT,0),
  new Design("outpost_remains",Family.ALPINE,Kind.RUIN,3,3,3,Roof.NONE,0),
  new Design("coastal_fishing_house",Family.COAST,Kind.HOUSE,3,4,3,Roof.GABLE,0),
  new Design("signal_tower",Family.COAST,Kind.TOWER,2,2,8,Roof.FLAT,0),
  new Design("broken_pier",Family.COAST,Kind.PLATFORM,2,4,1,Roof.NONE,1)
 );
 public static final Catalogue CATALOGUE=new Catalogue(){
  public List<Design> designs(){return DESIGNS;}
  public Optional<Family> family(WorldgenEnvironment h){
   if(h==null||h.zone()==null||!Double.isFinite(h.temperatureCelsius())||!Double.isFinite(h.moisture())||!Double.isFinite(h.dryness()))return Optional.empty();
   double t=h.temperatureCelsius();
   return switch(h.zone()){
    case MARINE,SUBTERRANEAN,UNKNOWN->Optional.empty();
    case ALPINE->Optional.of(Family.ALPINE);
    case TUNDRA->Optional.of(Family.COLD);
    case COAST->Optional.of(t<0?Family.COLD:Family.COAST);
    case WETLAND,RIVERBANK->Optional.of(t<0?Family.COLD:Family.WETLAND);
    case COLD_FOREST->Optional.of(Family.BOREAL);
    default->Optional.of(t<3?Family.COLD:t<8&&h.zone()==dev.minemods.worldlogic.api.WorldLogicApi.Zone.TEMPERATE_FOREST?Family.BOREAL:
     (t>=12&&h.dryness()>=0.58&&h.moisture()<=0.45)?Family.ARID:t>=20&&h.moisture()>=0.52?Family.TROPICAL:Family.TEMPERATE);
   };
  }
 };
}
