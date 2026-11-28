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
  case CHERNOZEM->new Soil(165,.66,.69,.98);
  case SANDY_LOAM->new Soil(85,.82,.45,.67);
  case LATERITE->new Soil(110,.42,.64,.39);
  case WHITE_SAND->new Soil(42,.97,.25,.12);
  case VOLCANIC_SAND->new Soil(53,.91,.32,.24);
  case VOLCANIC_ASH->new Soil(100,.53,.57,.30);
 };}
 public static SoilType soilType(BlockState b){
  b=SoilFarmland.original(b);
  if(GroundMaterials.is(b,"chernozem"))return SoilType.CHERNOZEM;
  if(GroundMaterials.is(b,"sandy_loam"))return SoilType.SANDY_LOAM;
  if(GroundMaterials.is(b,"laterite"))return SoilType.LATERITE;
  if(GroundMaterials.is(b,"white_sand"))return SoilType.WHITE_SAND;
  if(GroundMaterials.is(b,"volcanic_sand"))return SoilType.VOLCANIC_SAND;
  if(GroundMaterials.is(b,"volcanic_ash"))return SoilType.VOLCANIC_ASH;
  if(GroundMaterials.volcanicRock(b)||RockContent.natural(b))return SoilType.ROCK;
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
 public static dev.minemods.worldlogic.api.WorldLogicApi.GroundProfile groundProfile(BlockState b){
  var t=soilType(b);var hyd=profile(t);
  double[] f=switch(t){
   case CHERNOZEM->new double[]{.30,.45,.25,.065,7.0};
   case SANDY_LOAM->new double[]{.65,.23,.12,.025,6.3};
   case LATERITE->new double[]{.34,.20,.46,.015,5.1};
   case WHITE_SAND->new double[]{.94,.04,.02,.002,8.1};
   case VOLCANIC_SAND->new double[]{.90,.07,.03,.004,6.5};
   case VOLCANIC_ASH->new double[]{.32,.56,.12,.003,6.0};
   case LOAM->new double[]{.40,.40,.20,.035,6.5};
   case HUMUS->new double[]{.25,.45,.30,.09,6.4};
   case PEAT->new double[]{.25,.40,.35,.55,4.7};
   case SILT->new double[]{.18,.68,.14,.035,6.8};
   case SAND->new double[]{.92,.06,.02,.008,6.8};
   case CLAY->new double[]{.20,.26,.54,.025,6.3};
   case FROZEN->new double[]{.40,.40,.20,.02,6.0};
   case ROCK,UNKNOWN->new double[]{0,0,0,0,-1};
  };
  boolean rootable=t!=SoilType.ROCK&&t!=SoilType.UNKNOWN&&t!=SoilType.FROZEN;
  return new dev.minemods.worldlogic.api.WorldLogicApi.GroundProfile(t,f[0],f[1],f[2],f[3],f[4],hyd.fertility,hyd.capacity,hyd.permeability,hyd.fieldCapacity,rootable);
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
