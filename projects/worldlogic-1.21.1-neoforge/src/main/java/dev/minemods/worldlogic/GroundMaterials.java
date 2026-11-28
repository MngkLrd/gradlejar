package dev.minemods.worldlogic;
import java.util.*;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.ColorRGBA;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.*;
/** Physical substrates; no ores, no crop or plant generation. */
public final class GroundMaterials{
 private GroundMaterials(){}
 private static final Map<String,Supplier<Block>> MATERIALS=new LinkedHashMap<>();
 static{
  register("white_sand",()->new ColoredFallingBlock(new ColorRGBA(0xffdfd8c6),BlockBehaviour.Properties.ofFullCopy(Blocks.SAND).mapColor(net.minecraft.world.level.material.MapColor.SAND)));
  register("volcanic_sand",()->new ColoredFallingBlock(new ColorRGBA(0xff494844),BlockBehaviour.Properties.ofFullCopy(Blocks.SAND).mapColor(net.minecraft.world.level.material.MapColor.COLOR_GRAY)));
  register("volcanic_ash",()->new ColoredFallingBlock(new ColorRGBA(0xff827d78),BlockBehaviour.Properties.ofFullCopy(Blocks.SAND).mapColor(net.minecraft.world.level.material.MapColor.COLOR_LIGHT_GRAY).strength(.4f)));
  register("volcanic_tuff",()->new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.TUFF).mapColor(net.minecraft.world.level.material.MapColor.TERRACOTTA_GRAY).strength(1.1f,3.5f).requiresCorrectToolForDrops()));
 }
 private static void register(String id,Supplier<Block> factory){
  var block=Content.BLOCKS.register(id,factory);MATERIALS.put(id,block);
  Content.ITEMS.register(id,()->new BlockItem(block.get(),new Item.Properties()));
 }
 static void register(){}
 public static Block block(String id){
  var soil=Content.SOILS.get(id);if(soil!=null)return soil.get();
  var material=MATERIALS.get(id);if(material==null)throw new IllegalArgumentException("Unknown ground "+id);return material.get();
 }
 public static BlockState state(String id){return block(id).defaultBlockState();}
 static boolean is(BlockState b,String id){return b.is(block(id));}
 static boolean volcanicRock(BlockState b){return b.is(Blocks.BASALT)||b.is(Blocks.SMOOTH_BASALT)||is(b,"volcanic_tuff");}
 private static final Set<String> STEPPE=Set.of("temperate_steppe","prairie","shortgrass_steppe","cold_steppe","forest_tundra");
 private static final Set<String> TROPICAL=Set.of("equatorial_rainforest","tropical_monsoon_forest","seasonal_tropical_forest","humid_savanna","subtropical_rainforest","warm_broadleaf_forest");
 private static final Set<String> DRY=Set.of("temperate_steppe","shortgrass_steppe","subtropical_grassland","mediterranean_scrub","sclerophyll_woodland","thorn_scrub","dry_savanna");
 static String soil(String biome,double thermal,double humidity,double slope,double patch){
  if(STEPPE.contains(biome)&&thermal>-.48&&thermal<.38&&humidity>=.33&&humidity<=.73&&slope<.20&&patch>.06)return "chernozem";
  if(TROPICAL.contains(biome)&&thermal>.22&&humidity>.55&&slope<.35&&patch>.14)return "laterite";
  if(DRY.contains(biome)&&humidity<.42&&slope<.32&&patch>.10)return "sandy_loam";
  return "";
 }
 static boolean whiteSand(String biome,double thermal,double shore,boolean carbonate,double patch){
  // Carbonate beaches dominate only suitable existing sand/gravel substrates.
  return thermal>.22&&shore<100&&(carbonate||biome.equals("tropical_sandy_coast"))&&patch>.08
   &&(biome.equals("tropical_sandy_coast")||ReefTerrain.warm(biome));
 }
 static boolean volcanicSand(double distance,double radius,double patch){return distance<radius+200&&patch>.10;}
 static boolean topSoil(BlockState b){
  return b.is(Blocks.GRASS_BLOCK)||b.is(Blocks.DIRT)||b.is(Blocks.COARSE_DIRT)||b.is(Blocks.PODZOL)||b.is(Blocks.ROOTED_DIRT)||b.is(Content.SOILS.get("loam").get())||b.is(Content.SOILS.get("humus").get());
 }
 static int horizon(ChunkAccess c,int x,int z,int h,String id,int thickness){
  var p=new BlockPos.MutableBlockPos();var top=c.getBlockState(p.set(x,h,z));if(!topSoil(top))return 0;
  int from=top.is(Blocks.GRASS_BLOCK)||top.is(Blocks.PODZOL)?h-1:h,changed=0;
  for(int y=from;y>from-thickness&&y>c.getMinBuildHeight()+5;y--){
   var b=c.getBlockState(p.set(x,y,z));if(!topSoil(b)&&!GeologyGeneration.rock(b))break;
   var below=c.getBlockState(p.set(x,y-1,z));if(below.isAir()||!below.getFluidState().isEmpty())break;
   c.setBlockState(p.set(x,y,z),state(id),false);changed++;
  }
  return changed;
 }
 static int sand(ChunkAccess c,int x,int z,int h,String id,int thickness){
  var p=new BlockPos.MutableBlockPos();int count=0;
  for(int y=h;y>h-thickness&&y>c.getMinBuildHeight()+5;y--){
   var b=c.getBlockState(p.set(x,y,z));
   if(!b.is(BlockTags.SAND)&&!b.is(Blocks.SANDSTONE)&&!b.is(Blocks.GRAVEL)&&!b.is(Blocks.CALCITE))break;
   var below=c.getBlockState(p.set(x,y-1,z));if(below.isAir()||!below.getFluidState().isEmpty())break;
   c.setBlockState(p.set(x,y,z),state(id),false);count++;
  }
  return count;
 }
 public static void decorate(NoiseBasedChunkGenerator gen,RandomState random,RegistryAccess access,ChunkAccess c,long seed){
  if(!NaturalWaterGeneration.enabled(gen))return;
  int sea=gen.getSeaLevel(),x0=c.getPos().getMinBlockX(),z0=c.getPos().getMinBlockZ();
  var model=GeoDensity.riverModel(random,access);var coast=CoastalTerrain.field(model,seed);var erosion=SurfaceErosion.field(model);
  var volcano=VolcanicTerrain.at(gen,random,access,seed,x0,z0).orElse(null);var p=new BlockPos.MutableBlockPos();
  for(int x=0;x<16;x++)for(int z=0;z<16;z++){
   int wx=x0+x,wz=z0+z,h=c.getHeight(Heightmap.Types.OCEAN_FLOOR_WG,x,z);
   if(h<=c.getMinBuildHeight()+6||h>=c.getMaxBuildHeight()-2)continue;
   var top=c.getBlockState(p.set(wx,h,wz));
   var key=c.getNoiseBiome(wx>>2,Math.max(h,sea-1)>>2,wz>>2).unwrapKey().orElseThrow().location();if(!key.getNamespace().equals("worldlogic"))continue;
   String biome=key.getPath();double patch=CoastalTerrain.noise(seed^0x73a121L,wx,wz,56);
   boolean beach=biome.endsWith("_coast"),marine=biome.endsWith("ocean");
   if(beach||marine){
    if(h>sea+3||h<sea-20)continue;
    if(marine&&!c.getFluidState(p.set(wx,sea-1,wz)).is(FluidTags.WATER))continue;
    if(volcano!=null&&volcanicSand(volcano.distance(wx,wz),volcano.radius(),patch)){sand(c,wx,wz,h,"volcanic_sand",2+(patch>.7?1:0));continue;}
    var cf=coast.apply(wx,wz);boolean carbonate=GeologyModel.province(seed,wx,wz)==GeologyModel.Province.CARBONATE||cf.variation()>.47;
    if(whiteSand(biome,cf.thermal(),cf.shore(),carbonate,patch))sand(c,wx,wz,h,"white_sand",2+(patch>.76?1:0));
    continue;
   }
   if(h<sea+4||!c.getBlockState(p.set(wx,h+1,wz)).isAir())continue;
   if(volcano!=null){
    double r=volcano.distance(wx,wz)/volcano.radius();
    if(r<.70&&RiverTerrain.natural(top)){c.setBlockState(p.set(wx,h,wz),(r<.31||patch>.47?Blocks.BASALT.defaultBlockState():state("volcanic_tuff")),false);continue;}
    if(r<1.38&&patch>.32&&RiverTerrain.natural(top)){
     var below=c.getBlockState(p.set(wx,h-1,wz));
     if(!below.isAir()&&below.getFluidState().isEmpty()){c.setBlockState(p.set(wx,h,wz),state("volcanic_ash"),false);continue;}
    }
   }
   if(!topSoil(top))continue;
   var regional=model.regional(wx,wz);var e=erosion.apply(wx,wz);
   String id=soil(biome,regional.thermalIndex(),regional.moistureIndex(),e.slope(),patch);
   if(!id.isEmpty())horizon(c,wx,wz,h,id,id.equals("chernozem")?2+(patch>.62?1:0):2);
  }
 }
 static String probe(ServerLevel l,BlockPos origin,String id){
  if(!(l.getChunkSource().getGenerator() instanceof NoiseBasedChunkGenerator g)||!NaturalWaterGeneration.enabled(g))return "GROUND_PROBE unavailable";
  if(!Set.of("chernozem","sandy_loam","laterite","white_sand","volcanic_sand").contains(id))return "GROUND_PROBE unknown";
  var s=l.getChunkSource().randomState();var model=GeoDensity.riverModel(s,l.registryAccess());
  var bounds=net.minecraft.world.level.LevelHeightAccessor.create(g.getMinY(),g.getGenDepth());
  var coast=CoastalTerrain.field(model,l.getSeed());var erosion=SurfaceErosion.field(model);int sea=g.getSeaLevel(),measured=0;
  boolean marine=id.endsWith("_sand");
  for(int ring=0;ring<=8;ring++)for(int dz=-ring;dz<=ring;dz++)for(int dx=-ring;dx<=ring;dx++){
   if(Math.max(Math.abs(dx),Math.abs(dz))!=ring)continue;
   int x=origin.getX()+dx*64,z=origin.getZ()+dz*64;
   double macro=model.raw(x,z);if(marine?(macro<sea-20||macro>sea+6):macro<sea+5)continue;
   double patch=CoastalTerrain.noise(l.getSeed()^0x73a121L,x,z,56);
   int guess=Math.max(sea,(int)Math.round(macro));
   String biome=BiomeSurvey.at(g,s,x,guess,z).getPath();
   if(marine){
    if(!biome.endsWith("_coast")&&!biome.endsWith("ocean"))continue;
    if(id.equals("white_sand")){
     var cf=coast.apply(x,z);boolean carbonate=GeologyModel.province(l.getSeed(),x,z)==GeologyModel.Province.CARBONATE||cf.variation()>.47;
     if(!whiteSand(biome,cf.thermal(),cf.shore(),carbonate,patch))continue;
    }else{
     var v=VolcanicTerrain.at(g,s,l.registryAccess(),l.getSeed(),x,z).orElse(null);
     if(v==null||!volcanicSand(v.distance(x,z),v.radius(),patch))continue;
    }
   }else{
    var climate=model.regional(x,z);
    if(!soil(biome,climate.thermalIndex(),climate.moistureIndex(),erosion.apply(x,z).slope(),patch).equals(id))continue;
   }
   if(measured++>=18)return "GROUND_PROBE no_candidate";
   int h=g.getBaseHeight(x,z,Heightmap.Types.OCEAN_FLOOR_WG,bounds,s)-1;
   if(marine?h>sea+3||h<sea-20:h<sea+4)continue;
   return "GROUND_PROBE x="+x+" z="+z+" material="+id;
  }
  return "GROUND_PROBE no_candidate";
 }
 static String audit(ServerLevel l,BlockPos origin){
  var c=l.getChunkSource().getChunkNow(origin.getX()>>4,origin.getZ()>>4);if(c==null)return "GROUND_AUDIT unavailable";
  var counts=new LinkedHashMap<String,Integer>();for(String id:List.of("white_sand","volcanic_sand","chernozem","sandy_loam","laterite","volcanic_ash","volcanic_tuff","basalt"))counts.put(id,0);
  int x0=c.getPos().getMinBlockX(),z0=c.getPos().getMinBlockZ();var p=new BlockPos.MutableBlockPos();
  for(int x=0;x<16;x++)for(int z=0;z<16;z++){
   int h=c.getHeight(Heightmap.Types.OCEAN_FLOOR,x,z);
   for(int y=h;y>=Math.max(c.getMinBuildHeight(),h-5);y--){
    var b=c.getBlockState(p.set(x0+x,y,z0+z));
    for(String id:counts.keySet())if(id.equals("basalt")?b.is(Blocks.BASALT):is(b,id))counts.put(id,counts.get(id)+1);
   }
  }
  var out=new StringJoiner(" ","GROUND_AUDIT ","");counts.forEach((k,v)->out.add(k+"="+v));return out.toString();
 }
}
