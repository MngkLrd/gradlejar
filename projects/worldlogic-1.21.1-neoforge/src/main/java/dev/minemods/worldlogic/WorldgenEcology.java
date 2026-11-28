package dev.minemods.worldlogic;
import dev.minemods.worldlogic.api.WorldLogicApi.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Pure habitat inference over immutable seeded terrain and caller-supplied local ground. */
final class WorldgenEcology {
 private WorldgenEcology(){}
 private static double unit(double v){return Math.max(0,Math.min(1,v));}
 private static boolean tagged(Holder<Biome> b,String name){
  return b.is(TagKey.create(Registries.BIOME,ResourceLocation.fromNamespaceAndPath("worldlogic","vegetation/"+name)));
 }
 private static Zone zone(Holder<Biome> b,double river){
  if(tagged(b,"marine"))return Zone.MARINE;
  if(tagged(b,"subterranean"))return Zone.SUBTERRANEAN;
  if(tagged(b,"coast"))return Zone.COAST;
  if(tagged(b,"wetland"))return Zone.WETLAND;
  if(river>.72)return Zone.RIVERBANK;
  if(tagged(b,"polar")||tagged(b,"tundra"))return Zone.TUNDRA;
  if(tagged(b,"alpine"))return Zone.ALPINE;
  if(tagged(b,"boreal"))return Zone.COLD_FOREST;
  if(tagged(b,"temperate_forest"))return Zone.TEMPERATE_FOREST;
  if(tagged(b,"tropical_forest")||tagged(b,"savanna"))return Zone.TROPICAL;
  if(tagged(b,"desert")||tagged(b,"scrub"))return Zone.DRYLAND;
  if(tagged(b,"grassland"))return Zone.OPEN_PLAINS;
  return Zone.UNKNOWN;
 }
 private static SurfaceFamily surface(BlockState b,double moisture,double wet,double cold){
  var soil=HydrologyModel.soilType(b);
  if(soil==SoilType.FROZEN)return SurfaceFamily.FROZEN;
  if(soil==SoilType.PEAT)return SurfaceFamily.BOGGY;
  if(soil==SoilType.SILT)return wet>.72?SurfaceFamily.BOGGY:SurfaceFamily.MOIST;
  if(soil==SoilType.HUMUS||soil==SoilType.CHERNOZEM)return moisture>.65?SurfaceFamily.MOIST:SurfaceFamily.NORMAL;
  if(soil==SoilType.VOLCANIC_ASH)return SurfaceFamily.DRY;
  if(soil==SoilType.SANDY_LOAM||soil==SoilType.LATERITE)return moisture<.30?SurfaceFamily.DRY:SurfaceFamily.NORMAL;
  if(soil==SoilType.ROCK)return SurfaceFamily.ROCKY;
  if(b.is(Blocks.SNOW)||b.is(Blocks.SNOW_BLOCK)||b.is(Blocks.ICE)||b.is(Blocks.PACKED_ICE)||b.is(Blocks.POWDER_SNOW)||b.is(Blocks.FROSTED_ICE))return SurfaceFamily.FROZEN;
  if(b.is(BlockTags.SAND))return SurfaceFamily.SANDY;
  if(b.is(BlockTags.BASE_STONE_OVERWORLD)||b.is(Blocks.CALCITE)||b.is(Blocks.GRAVEL)||b.is(Blocks.SANDSTONE)||b.is(Blocks.RED_SANDSTONE)||b.is(Blocks.TERRACOTTA))return SurfaceFamily.ROCKY;
  if(b.is(Blocks.MUD)||b.is(Blocks.MUDDY_MANGROVE_ROOTS)||b.is(Blocks.CLAY)||b.is(Blocks.WATER))return SurfaceFamily.BOGGY;
  if(b.is(BlockTags.DIRT)||b.is(Blocks.FARMLAND)){
   if(cold>.82)return SurfaceFamily.FROZEN;
   if(wet>.72)return SurfaceFamily.BOGGY;
   if(moisture<.30||b.is(Blocks.COARSE_DIRT))return SurfaceFamily.DRY;
   if(moisture>.65)return SurfaceFamily.MOIST;
   return SurfaceFamily.NORMAL;
  }
  return SurfaceFamily.UNKNOWN;
 }
 static WorldgenEnvironment sample(GeoModel.WorldgenSite s,BlockPos pos,Holder<Biome> biome,BlockState ground){
  double temp=14+28*s.thermal()-.065*Math.max(-80,Math.min(160,pos.getY()-s.height()));
  double moisture=unit(s.moisture()),cold=unit((6-temp)/20);
  double flow=unit(Math.log1p(s.drainage())/Math.log(100));
  double flat=1-unit(s.slope()/24);
  double river=s.height()>=63?unit((flow-.60)/.40)*flat:0;
  double wet=unit(.64*moisture+.32*river+.12*flat-(1-flat)*.18);
  if(ground.is(Blocks.WATER)||ground.is(Blocks.MUD))wet=Math.max(wet,.85);
  double dry=unit((1-moisture)*.78+.22*unit((temp-14)/26)+.15*(1-flat)-.25*river);
  var family=surface(ground,moisture,wet,cold);var zone=zone(biome,river);
  double nutrient=switch(family){case NORMAL->.78;case MOIST->.84;case DRY->.42;case SANDY->.20;case BOGGY->.52;case ROCKY->.12;case FROZEN->.18;case UNKNOWN->.25;};
  var soilType=HydrologyModel.soilType(ground);
  if(soilType!=SoilType.UNKNOWN&&soilType!=SoilType.LOAM)nutrient=HydrologyModel.profile(soilType).fertility();
  double fertility=unit(nutrient*(.72+.28*flat)*(1-.45*cold));
  double strength=unit(fertility*(1-.55*dry)*(1-.38*cold)*(1-.35*Math.max(0,wet-.60)/.40));
  var id=biome.unwrapKey().map(k->k.location()).orElse(ResourceLocation.fromNamespaceAndPath("worldlogic","unknown"));
  return new WorldgenEnvironment(temp,moisture,fertility,dry,wet,cold,strength,zone,family,s.slope(),s.exposure(),s.windX(),s.windZ(),s.height(),river,id);
 }
}
