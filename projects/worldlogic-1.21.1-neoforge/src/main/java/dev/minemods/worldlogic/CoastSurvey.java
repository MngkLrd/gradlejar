package dev.minemods.worldlogic;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import dev.minemods.worldlogic.api.WorldLogicApi.WaterKind;
/** Independent survey of full generated chunks, not predicted geography. */
final class CoastSurvey {
 private CoastSurvey(){}
 private static boolean ocean(net.minecraft.core.Holder<net.minecraft.world.level.biome.Biome> b){
  return b.unwrapKey().map(k->k.location().getNamespace().equals("worldlogic")&&k.location().getPath().endsWith("ocean")).orElse(false);
 }
 private static boolean wet(LevelChunk c,int x,int z,int sea){
  for(int y=sea-1;y>=sea-4;y--){
   var p=new BlockPos(x,y,z);var b=c.getBlockState(p);
   if(c.getFluidState(p).is(FluidTags.WATER)||b.is(Blocks.ICE)||b.is(Blocks.PACKED_ICE)||b.is(Blocks.BLUE_ICE)||b.is(Blocks.FROSTED_ICE))return true;
  }
  return false;
 }
 private static int dryInterior(ServerLevel l,int x,int z,int sea){
  boolean unknown=false;
  for(int dx=-5;dx<=5;dx++)for(int dz=-5;dz<=5;dz++){
   var c=l.getChunkSource().getChunkNow((x+dx)>>4,(z+dz)>>4);
   if(c==null){unknown=true;continue;}
   if(wet(c,x+dx,z+dz,sea))return 0;
  }
  return unknown?-1:1;
 }
 static String audit(ServerLevel l,BlockPos origin){
  var c=l.getChunkSource().getChunkNow(origin.getX()>>4,origin.getZ()>>4);
  if(c==null)return "COAST_AUDIT unavailable";
  int sea=l.getSeaLevel(),x0=c.getPos().getMinBlockX(),z0=c.getPos().getMinBlockZ();
  int ocean=0,wetOcean=0,dry=0,inland=0,unknown=0,dryQuart=0,river=0,links=0;
  int deep=0;int[] riverY=new int[256];Arrays.fill(riverY,Integer.MIN_VALUE);
  List<String> examples=new ArrayList<>();
  for(int x=0;x<16;x++)for(int z=0;z<16;z++){
   int wx=x0+x,wz=z0+z,h=c.getHeight(Heightmap.Types.OCEAN_FLOOR,x,z);
   boolean water=wet(c,wx,wz,sea);
   int top=c.getHeight(Heightmap.Types.WORLD_SURFACE,x,z);
   var p=new BlockPos(wx,Math.max(sea-1,h+1),wz);
   var actual=l.getBiome(p);
   var flight=l.getBiome(new BlockPos(wx,Math.min(l.getMaxBuildHeight()-1,Math.max(96,h+12)),wz));
   if(ocean(actual)||ocean(flight)){
    ocean++;if(water)wetOcean++;else{
     dry++;int status=dryInterior(l,wx,wz,sea);
     if(status==1){inland++;if(examples.size()<12)examples.add(wx+","+h+","+wz+":"+actual.unwrapKey().orElseThrow().location()+"/"+flight.unwrapKey().orElseThrow().location());}
     if(status<0)unknown++;
    }
   }
   for(int y=Math.min(l.getMaxBuildHeight()-1,Math.max(top,h+1));y>=Math.max(sea,h-2);y--){
    if(NaturalFluids.kind(c.getFluidState(new BlockPos(wx,y,wz)).getType())==WaterKind.RIVER){river++;riverY[x*16+z]=y;if(NaturalFluids.kind(c.getFluidState(new BlockPos(wx,y-1,wz)).getType())==WaterKind.RIVER&&NaturalFluids.kind(c.getFluidState(new BlockPos(wx,y-2,wz)).getType())==WaterKind.RIVER)deep++;break;}
   }
  }
  for(int x=0;x<4;x++)for(int z=0;z<4;z++){
   boolean any=false;for(int dx=0;dx<4;dx++)for(int dz=0;dz<4;dz++)if(wet(c,x0+x*4+dx,z0+z*4+dz,sea))any=true;
   int h=c.getHeight(Heightmap.Types.OCEAN_FLOOR,x*4+2,z*4+2);
   if(!any&&ocean(c.getNoiseBiome((x0+x*4)>>2,Math.max(sea-1,h+1)>>2,(z0+z*4)>>2)))dryQuart++;
  }
  for(int x=0;x<16;x++)for(int z=0;z<16;z++){
   int y=riverY[x*16+z];if(y==Integer.MIN_VALUE)continue;
   if(x<15&&riverY[(x+1)*16+z]!=Integer.MIN_VALUE&&Math.abs(y-riverY[(x+1)*16+z])<=1)links++;
   if(z<15&&riverY[x*16+z+1]!=Integer.MIN_VALUE&&Math.abs(y-riverY[x*16+z+1])<=1)links++;
  }
  if(inland>0||dryQuart>0)org.slf4j.LoggerFactory.getLogger("WorldLogic").warn("WORLDLOGIC_DRY_OCEAN chunk={} dryQuart={} inland={} examples={}",c.getPos(),dryQuart,inland,examples);
  return "COAST_AUDIT columns=256 ocean="+ocean+" wet="+wetOcean+" dry="+dry+" inland="+inland+" unknown="+unknown+" dryQuart="+dryQuart+" river="+river+" links="+links+" deep="+deep+" examples="+examples;
 }
}
