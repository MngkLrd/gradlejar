package dev.minemods.worldlogic;
import dev.minemods.worldlogic.api.WorldLogicApi;
import dev.minemods.worldlogic.api.WorldLogicApi.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
/** Replace standalone generated water, chunk-confined after decoration. Do not edit density or neighboring chunks. */
public final class NaturalWaterGeneration{
 private NaturalWaterGeneration(){}
 public static boolean enabled(ChunkGenerator g){
  return g instanceof NoiseBasedChunkGenerator n&&n.generatorSettings().unwrapKey().map(k->k.location().equals(ResourceLocation.fromNamespaceAndPath("worldlogic","realistic_unified"))).orElse(false);
 }
 private static boolean tagged(Holder<Biome> b,String ns,String tag){return b.is(TagKey.create(Registries.BIOME,ResourceLocation.fromNamespaceAndPath(ns,tag)));}
 static WaterKind surfaceKind(Holder<Biome> b,double river){
  if(tagged(b,"minecraft","is_ocean"))return WaterKind.SEA;
  if(tagged(b,"worldlogic","water/estuary"))return WaterKind.BRACKISH;
  if(tagged(b,"worldlogic","water/wetland"))return WaterKind.WETLAND;
  if(tagged(b,"minecraft","is_river")||river>.72)return WaterKind.RIVER;
  return WaterKind.LAKE;
 }
 public static void convert(WorldGenLevel level,ChunkAccess chunk,ChunkGenerator generator){
  if(!enabled(generator))return;
  var sampler=WorldLogicApi.worldgenSampler(level,generator);
  convertChunk(chunk,sampler,null);
 }
 static int convertChunk(ChunkAccess chunk,java.util.Optional<WorldgenSampler> sampler,Holder<Biome> fixtureBiome){
  int x0=chunk.getPos().getMinBlockX(),z0=chunk.getPos().getMinBlockZ(),converted=0;
  var p=new BlockPos.MutableBlockPos();
  var kinds=new WaterKind[256];
  int[] floors=new int[256];
  for(int x=0;x<16;x++)for(int z=0;z<16;z++)floors[x*16+z]=chunk.getHeight(Heightmap.Types.OCEAN_FLOOR_WG,x,z);
  for(int i=0;i<chunk.getSections().length;i++){
   var section=chunk.getSections()[i];
   if(!section.maybeHas(s->s.is(Blocks.WATER)))continue;
   int y0=chunk.getMinBuildHeight()+i*16;
   for(int x=0;x<16;x++)for(int z=0;z<16;z++)for(int y=y0;y<y0+16;y++){
    p.set(x0+x,y,z0+z);var s=chunk.getBlockState(p);
    if(!s.is(Blocks.WATER))continue;
    int index=x*16+z,floor=floors[index];WaterKind kind;
    if(y<floor-6)kind=WaterKind.GROUNDWATER;
    else{
     kind=kinds[index];
     if(kind==null){
      int sy=Math.min(chunk.getMaxBuildHeight()-1,Math.max(chunk.getMinBuildHeight(),Math.max(y,chunk.getHeight(Heightmap.Types.WORLD_SURFACE_WG,x,z)-1)));
      var biome=fixtureBiome!=null?fixtureBiome:chunk.getNoiseBiome((x0+x)>>2,sy>>2,(z0+z)>>2);
      var bed=chunk.getBlockState(p.set(x0+x,Math.max(chunk.getMinBuildHeight(),Math.min(y-1,floor)),z0+z));
      var samplePos=new BlockPos(x0+x,sy,z0+z);
      double river=sampler.map(v->v.sample(samplePos,biome,bed).riverbankPotential()).orElse(0.0);
      kind=surfaceKind(biome,river);kinds[index]=kind;
     }
    }
    p.set(x0+x,y,z0+z);
    var next=NaturalFluids.get(kind).block.get().defaultBlockState().setValue(LiquidBlock.LEVEL,s.getValue(LiquidBlock.LEVEL));
    chunk.setBlockState(p,next,false);converted++;
    if(!s.getFluidState().isSource())chunk.markPosForPostprocessing(p);
   }
  }
  return converted;
 }
}
