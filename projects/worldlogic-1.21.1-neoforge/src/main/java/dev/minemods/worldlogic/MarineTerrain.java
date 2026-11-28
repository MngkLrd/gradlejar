package dev.minemods.worldlogic;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;
/** Sediments on the actual exposed bed after carving and before plant/structure decoration.
 * Only replace naturally generated substrate; never change elevation or add water. */
public final class MarineTerrain {
 private MarineTerrain(){}
 private static boolean natural(BlockState b){
  return GeologyGeneration.rock(b)||b.is(BlockTags.DIRT)||b.is(BlockTags.SAND)||b.is(Blocks.GRAVEL)||b.is(Blocks.CLAY);
 }
 private static double hash(long seed,int x,int z){
  long n=seed+341873128712L*x+132897987541L*z;
  n=(n^(n>>>30))*0xbf58476d1ce4e5b9L;
  n=(n^(n>>>27))*0x94d049bb133111ebL;
  return ((n^(n>>>31))>>>11)*0x1.0p-53;
 }
 private static double mix(double a,double b,double t){return a+(b-a)*t;}
 private static double patch(long seed,int x,int z){
  int gx=Math.floorDiv(x,12),gz=Math.floorDiv(z,12);
  double u=Math.floorMod(x,12)/12.0,v=Math.floorMod(z,12)/12.0;
  u=u*u*(3-2*u);v=v*v*(3-2*v);
  return mix(mix(hash(seed,gx,gz),hash(seed,gx+1,gz),u),mix(hash(seed,gx,gz+1),hash(seed,gx+1,gz+1),u),v);
 }
 static BlockState material(String biome,double patch,double detail){
  if(biome.endsWith("sandy_coast"))return Blocks.SAND.defaultBlockState();
  boolean warm=biome.equals("tropical_ocean")||biome.equals("subtropical_ocean")||biome.equals("warm_deep_ocean");
  if(warm)return (patch>.82?Blocks.CLAY:patch<.12?Blocks.STONE:Blocks.SAND).defaultBlockState();
  return (patch<.23?Blocks.STONE:patch>.83?Blocks.CLAY:patch>.52?Blocks.SAND:Blocks.GRAVEL).defaultBlockState();
 }
 public static void prepare(long seed,ChunkAccess chunk,int sea){
  var p=new BlockPos.MutableBlockPos();
  int x0=chunk.getPos().getMinBlockX(),z0=chunk.getPos().getMinBlockZ();
  for(int x=0;x<16;x++)for(int z=0;z<16;z++){
   int wx=x0+x,wz=z0+z;
   int h=chunk.getHeight(Heightmap.Types.OCEAN_FLOOR_WG,x,z);
   if(h<=chunk.getMinBuildHeight()+5||h>sea+3)continue;
   var biome=chunk.getNoiseBiome(wx>>2,Math.max(sea-1,h)>>2,wz>>2).unwrapKey().orElseThrow().location();
   if(!biome.getNamespace().equals("worldlogic"))continue;
   String id=biome.getPath();boolean beach=id.endsWith("sandy_coast");
   if(!beach&&!id.endsWith("ocean"))continue;
   if(!beach&&!chunk.getFluidState(p.set(wx,sea-1,wz)).is(FluidTags.WATER))continue;
   if(!natural(chunk.getBlockState(p.set(wx,h,wz)))||!natural(chunk.getBlockState(p.set(wx,h-1,wz))))continue;
   var top=material(id,patch(seed,wx,wz),patch(seed^0x6295L,wx,wz));
   chunk.setBlockState(p.set(wx,h,wz),top,false);
   if(top.is(Blocks.SAND)||top.is(Blocks.GRAVEL)||top.is(Blocks.CLAY)){
    var below=chunk.getBlockState(p.set(wx,h-1,wz));
    if(natural(below))chunk.setBlockState(p,top.is(Blocks.SAND)&&beach?Blocks.SANDSTONE.defaultBlockState():top,false);
   }
  }
 }
}
