package dev.minemods.worldlogic;
import dev.minemods.worldlogic.api.WorldLogicApi.SoilType;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Bounded soil/groundwater/snow/pond storage. Transfers are water-conserving. */
public final class HydrologyModel {
 private HydrologyModel(){}
 record Soil(double capacity,double permeability,double fieldCapacity,double fertility){}
 static Soil profile(SoilType t){return switch(t){
  case LOAM->new Soil(110,.68,.58,.82);case HUMUS->new Soil(150,.76,.62,.94);
  case PEAT->new Soil(220,.40,.82,.60);case SILT->new Soil(135,.49,.70,.87);
  case FROZEN->new Soil(70,.035,.80,.17);case SAND->new Soil(48,.95,.29,.26);
  case CLAY->new Soil(145,.18,.84,.57);case ROCK->new Soil(12,.08,.25,.08);
  case UNKNOWN->new Soil(75,.40,.55,.35);
 };}
 public static SoilType soilType(BlockState b){
  if(b.is(Content.SOILS.get("humus").get()))return SoilType.HUMUS;
  if(b.is(Content.SOILS.get("loam").get()))return SoilType.LOAM;
  if(b.is(Content.SOILS.get("peat").get()))return SoilType.PEAT;
  if(b.is(Content.SOILS.get("alluvial_silt").get()))return SoilType.SILT;
  if(b.is(Content.SOILS.get("frozen_soil").get())||b.is(Blocks.SNOW_BLOCK)||b.is(Blocks.ICE)||b.is(Blocks.PACKED_ICE))return SoilType.FROZEN;
  if(b.is(BlockTags.SAND))return SoilType.SAND;
  if(b.is(Blocks.CLAY)||b.is(Blocks.MUD))return SoilType.CLAY;
  if(b.is(BlockTags.DIRT)||b.is(Blocks.FARMLAND))return SoilType.LOAM;
  if(b.is(BlockTags.BASE_STONE_OVERWORLD)||b.is(Blocks.GRAVEL)||b.is(Blocks.SANDSTONE)||b.is(Blocks.CALCITE))return SoilType.ROCK;
  return SoilType.UNKNOWN;
 }
 record Store(double soil,double groundwater,double snow,double pond){
  double mass(){return soil+groundwater+snow+pond;}
 }
 record Step(Store store,double runoff,double evaporation,double recharge,double discharge){}
 static double unit(double v){return Math.max(0,Math.min(1,v));}
 static Store initial(SoilType type,double moisture){
  var s=profile(type);return new Store(s.capacity*unit(moisture)*.80,30+180*unit(moisture),0,0);
 }
 static Step advance(Store old,SoilType type,double rain,double snowfall,double temp,double humidity,double sun,double wind){
  return advance(old,type,rain,snowfall,temp,humidity,sun,wind,0);
 }
 static Step advance(Store old,SoilType type,double rain,double snowfall,double temp,double humidity,double sun,double wind,double roots){
  roots=unit(roots);
  var s=profile(type);
  double snow=old.snow+Math.max(0,snowfall);
  double melt=Math.min(snow,Math.max(0,temp)*.20);snow-=melt;
  double soil=Math.min(old.soil,s.capacity);
  double liquid=old.pond+Math.max(0,rain)+melt+Math.max(0,old.soil-s.capacity);
  double freeze=temp<=-1?.06:temp<2?.35:1;
  double infiltrate=Math.min(liquid,Math.min(s.capacity-soil,8*s.permeability*freeze*(1+.35*roots)));
  soil+=infiltrate;liquid-=infiltrate;
  double recharge=Math.min(soil,Math.max(0,soil-s.capacity*s.fieldCapacity)*.13*s.permeability*freeze*(1-.30*roots));
  soil-=recharge;
  double gw=old.groundwater+recharge;
  double discharge=Math.min(gw,Math.max(0,gw-50)*.012);
  gw-=discharge;liquid+=discharge;
  double evaporationDemand=Math.max(0,temp+4)*.018*(1-unit(humidity))*(.25+.75*unit(sun))*(1+Math.min(25,Math.max(0,wind))*.045);
  double fromPond=Math.min(liquid,evaporationDemand);liquid-=fromPond;
  double fromSoil=Math.min(soil,(evaporationDemand-fromPond)*unit(soil/s.capacity)*(1-.35*roots));soil-=fromSoil;
  double pond=Math.min((type==SoilType.PEAT?18:6)+4*roots,liquid),runoff=liquid-pond;
  return new Step(new Store(soil,gw,snow,pond),runoff,fromPond+fromSoil,recharge,discharge);
 }
}
