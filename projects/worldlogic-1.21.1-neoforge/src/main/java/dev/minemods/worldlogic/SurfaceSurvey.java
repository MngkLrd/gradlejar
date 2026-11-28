package dev.minemods.worldlogic;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
final class SurfaceSurvey{
 private static boolean ground(BlockState s){
  return GeologyGeneration.rock(s)||s.is(BlockTags.DIRT)||s.is(BlockTags.SAND)||s.is(Blocks.GRAVEL)||s.is(Blocks.CLAY)||s.is(Blocks.SNOW_BLOCK)||s.is(Blocks.ICE)||s.is(Blocks.PACKED_ICE)||s.is(Blocks.BLUE_ICE)||s.is(Blocks.TERRACOTTA)||s.is(Blocks.RED_SANDSTONE)||s.is(Blocks.SANDSTONE)||Content.SOILS.values().stream().anyMatch(v->s.is(v.get()));
 }
 static String audit(ServerLevel l,BlockPos origin){
  var c=l.getChunkSource().getChunkNow(origin.getX()>>4,origin.getZ()>>4);
  if(c==null)return "SURFACE_AUDIT unavailable";
  int x0=c.getPos().getMinBlockX(),z0=c.getPos().getMinBlockZ();
  var data=new StringJoiner(";");var p=new BlockPos.MutableBlockPos();
  for(int x=0;x<16;x+=2)for(int z=0;z<16;z+=2){
   int wx=x0+x,wz=z0+z,top=c.getHeight(Heightmap.Types.WORLD_SURFACE,x,z),floor=c.getHeight(Heightmap.Types.OCEAN_FLOOR,x,z);
   boolean water=false;int waterY=Integer.MIN_VALUE;
   for(int y=top;y>=Math.max(c.getMinBuildHeight(),floor-2);y--){
    if(c.getFluidState(p.set(wx,y,wz)).is(FluidTags.WATER)){water=true;waterY=y;break;}
   }
   int y=floor;for(;y>=Math.max(c.getMinBuildHeight(),floor-48);y--)if(ground(c.getBlockState(p.set(wx,y,wz))))break;
   var s=c.getBlockState(p.set(wx,y,wz));String type=!ground(s)?"?":s.is(BlockTags.SAND)||s.is(Blocks.SANDSTONE)?"s":s.is(BlockTags.DIRT)||Content.SOILS.values().stream().anyMatch(v->s.is(v.get()))?"d":s.is(Blocks.SNOW_BLOCK)||s.is(Blocks.ICE)||s.is(Blocks.PACKED_ICE)||s.is(Blocks.BLUE_ICE)?"i":"r";
   var f=water?NaturalFluids.kind(c.getFluidState(p.set(wx,waterY,wz)).getType()):dev.minemods.worldlogic.api.WorldLogicApi.WaterKind.UNKNOWN;
   data.add(y+","+type+","+(water?waterY:-999)+","+f.name());
  }
  return "SURFACE_AUDIT "+data;
 }
}
