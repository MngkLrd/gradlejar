package dev.minemods.worldlogic;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;

/** Chunk-confined geological generation, activated only for the new versioned preset. */
public final class GeologyGeneration {
 private GeologyGeneration(){}
 private static final Set<Block> REPLACED=Set.of(Blocks.COAL_ORE,Blocks.DEEPSLATE_COAL_ORE,Blocks.IRON_ORE,Blocks.DEEPSLATE_IRON_ORE,
  Blocks.COPPER_ORE,Blocks.DEEPSLATE_COPPER_ORE,Blocks.GOLD_ORE,Blocks.DEEPSLATE_GOLD_ORE,Blocks.DIAMOND_ORE,Blocks.DEEPSLATE_DIAMOND_ORE,
  Blocks.EMERALD_ORE,Blocks.DEEPSLATE_EMERALD_ORE,Blocks.LAPIS_ORE,Blocks.DEEPSLATE_LAPIS_ORE,Blocks.REDSTONE_ORE,Blocks.DEEPSLATE_REDSTONE_ORE);
 public static boolean enabled(ChunkGenerator g){
  return g instanceof NoiseBasedChunkGenerator n&&n.generatorSettings().unwrapKey().map(k->k.location().getNamespace().equals("worldlogic")&&Set.of("earthlike_extended","realistic_unified").contains(k.location().getPath())).orElse(false);
 }
 public static boolean replaces(OreConfiguration c){return !c.targetStates.isEmpty()&&c.targetStates.stream().allMatch(t->REPLACED.contains(t.state.getBlock()));}
 static boolean rock(BlockState s){return s.is(Blocks.STONE)||s.is(Blocks.DEEPSLATE)||s.is(Blocks.ANDESITE)||s.is(Blocks.DIORITE)||s.is(Blocks.GRANITE)||s.is(Blocks.TUFF)||s.is(Blocks.CALCITE)||s.is(Blocks.SANDSTONE)||GroundMaterials.volcanicRock(s)||RockContent.natural(s);}
 private static boolean shaftRock(BlockState s){return rock(s)||s.is(net.minecraft.tags.BlockTags.DIRT)||s.is(Blocks.GRASS_BLOCK)||s.is(Blocks.DIRT)||s.is(Blocks.COARSE_DIRT)||s.is(Blocks.GRAVEL)||s.is(Blocks.SAND)||s.is(Blocks.SANDSTONE);}
 static BlockState ore(GeologyModel.Ore type,boolean deep){return switch(type){
  case COAL -> (deep?Blocks.DEEPSLATE_COAL_ORE:Blocks.COAL_ORE).defaultBlockState();
  case IRON -> (deep?Blocks.DEEPSLATE_IRON_ORE:Blocks.IRON_ORE).defaultBlockState();
  case COPPER -> (deep?Blocks.DEEPSLATE_COPPER_ORE:Blocks.COPPER_ORE).defaultBlockState();
  case GOLD -> (deep?Blocks.DEEPSLATE_GOLD_ORE:Blocks.GOLD_ORE).defaultBlockState();
  case DIAMOND -> (deep?Blocks.DEEPSLATE_DIAMOND_ORE:Blocks.DIAMOND_ORE).defaultBlockState();
  case EMERALD -> (deep?Blocks.DEEPSLATE_EMERALD_ORE:Blocks.EMERALD_ORE).defaultBlockState();
  case LAPIS -> (deep?Blocks.DEEPSLATE_LAPIS_ORE:Blocks.LAPIS_ORE).defaultBlockState();
  case REDSTONE -> (deep?Blocks.DEEPSLATE_REDSTONE_ORE:Blocks.REDSTONE_ORE).defaultBlockState();
 };}
 public static void carve(long seed,ChunkAccess chunk){
  int x0=chunk.getPos().getMinBlockX(),z0=chunk.getPos().getMinBlockZ();
  var districts=GeologyModel.around(seed,x0+8,z0+8);
  var pos=new BlockPos.MutableBlockPos();
  for(int x=x0;x<x0+16;x++)for(int z=z0;z<z0+16;z++){
   int surface=chunk.getHeight(Heightmap.Types.OCEAN_FLOOR_WG,x&15,z&15);
   var province=GeologyModel.province(seed,x,z);
   int top=surface-6;
   int fold=(int)(6*Math.sin(x*.018)+5*Math.cos(z*.016));
   RockContent.stratify(chunk,x,z,top,province,fold);
   for(var d:districts)for(var c:d.caves()){
    if(Math.abs(x-c.x())>96||Math.abs(z-c.z())>96)continue;
    int low=Math.max(chunk.getMinBuildHeight()+8,c.minY()),high=Math.min(chunk.getMaxBuildHeight()-1,c.shaft()&&Math.hypot(x-c.x(),z-c.z())<10?Math.max(c.maxY(),surface+1):c.maxY());
    for(int y=low;y<=high;y++){
     boolean shaft=c.shaftAt(x,y,z,surface);
     if(!shaft&&(y>surface-5||c.score(x,y,z)>1))continue;
     pos.set(x,y,z);var s=chunk.getBlockState(pos);
     if(!(shaft?shaftRock(s):rock(s)))continue;
     var next=y<=c.waterY()?Blocks.WATER.defaultBlockState():Blocks.CAVE_AIR.defaultBlockState();
     chunk.setBlockState(pos,next,false);
     if(!next.getFluidState().isEmpty())chunk.markPosForPostprocessing(pos);
    }
   }
  }
 }
 public static void mineralize(long seed,ChunkAccess chunk){
  int x0=chunk.getPos().getMinBlockX(),z0=chunk.getPos().getMinBlockZ();
  var districts=GeologyModel.around(seed,x0+8,z0+8);
  var pos=new BlockPos.MutableBlockPos();
  for(var d:districts)for(var o:d.ores())placeBody(chunk,x0,z0,pos,o,false);
  for(var p:GeologyModel.nearbyPockets(seed,x0+8,z0+8))placeBody(chunk,x0,z0,pos,p.body(),p.relative());
 }
 private static void placeBody(ChunkAccess chunk,int x0,int z0,BlockPos.MutableBlockPos pos,GeologyModel.Body o,boolean relative){
  double reach=Math.max(o.along(),o.across())+2;
  int minX=Math.max(x0,(int)Math.floor(o.x()-reach)),maxX=Math.min(x0+15,(int)Math.ceil(o.x()+reach));
  int minZ=Math.max(z0,(int)Math.floor(o.z()-reach)),maxZ=Math.min(z0+15,(int)Math.ceil(o.z()+reach));
  for(int x=minX;x<=maxX;x++)for(int z=minZ;z<=maxZ;z++){
   int surface=chunk.getHeight(Heightmap.Types.OCEAN_FLOOR_WG,x&15,z&15);
   int shift=relative?surface:0;
   int low=Math.max(chunk.getMinBuildHeight()+8,o.minY()+shift),high=Math.min(surface-4,Math.min(chunk.getMaxBuildHeight()-1,o.maxY()+shift));
   for(int y=low;y<=high;y++){
    if(!o.contains(x,y-shift,z))continue;
    pos.set(x,y,z);var s=chunk.getBlockState(pos);
    if(rock(s))chunk.setBlockState(pos,ore(o.ore(),y<0),false);
   }
  }
 }
}
