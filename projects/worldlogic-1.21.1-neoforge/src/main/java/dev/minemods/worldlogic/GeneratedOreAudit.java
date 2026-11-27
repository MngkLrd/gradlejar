package dev.minemods.worldlogic;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
/** Read only existing full chunks. Match absolute deep deposits to actual blocks; no ore locations exposed in the reply. */
final class GeneratedOreAudit {
 private static GeologyModel.Ore type(BlockState b){
  if(b.is(BlockTags.COAL_ORES))return GeologyModel.Ore.COAL;
  if(b.is(BlockTags.IRON_ORES))return GeologyModel.Ore.IRON;
  if(b.is(BlockTags.COPPER_ORES))return GeologyModel.Ore.COPPER;
  if(b.is(BlockTags.GOLD_ORES))return GeologyModel.Ore.GOLD;
  if(b.is(BlockTags.DIAMOND_ORES))return GeologyModel.Ore.DIAMOND;
  if(b.is(BlockTags.EMERALD_ORES))return GeologyModel.Ore.EMERALD;
  if(b.is(BlockTags.LAPIS_ORES))return GeologyModel.Ore.LAPIS;
  if(b.is(BlockTags.REDSTONE_ORES))return GeologyModel.Ore.REDSTONE;
  return null;
 }
 static String scan(ServerLevel l,BlockPos origin){
  var gen=l.getChunkSource().getGenerator();
  if(!GeologyGeneration.enabled(gen))return "ORE_AUDIT inactive: legacy or vanilla generator; new Realistic world required.";
  int cx=origin.getX()>>4,cz=origin.getZ()>>4;
  LevelChunk chunk=l.getChunkSource().getChunkNow(cx,cz);
  if(chunk==null)return "ORE_AUDIT unavailable: chunk not full/loaded.";
  int x0=cx*16,z0=cz*16,deep=0,matched=0,missed=0,eligible=0,total=0;
  var counts=new EnumMap<GeologyModel.Ore,Integer>(GeologyModel.Ore.class);
  var bodies=new ArrayList<GeologyModel.Body>();
  for(var d:GeologyModel.around(l.getSeed(),x0+8,z0+8))bodies.addAll(d.ores());
  for(var p:GeologyModel.nearbyPockets(l.getSeed(),x0+8,z0+8))if(!p.relative())bodies.add(p.body());
  // Only test actual rock/ore sites: caves, structures, soil and water are valid non-ore.
  var pos=new BlockPos.MutableBlockPos();
  for(int x=x0;x<x0+16;x++)for(int z=z0;z<z0+16;z++)for(int y=l.getMinBuildHeight()+8;y<l.getMaxBuildHeight();y++){
   var b=chunk.getBlockState(pos.set(x,y,z));var ore=type(b);
   if(ore!=null){counts.merge(ore,1,Integer::sum);total++;if(y<-80)deep++;}
   if(y>=-80||!(ore!=null||GeologyGeneration.rock(b)))continue;
   boolean expected=false,correct=false;
   for(var body:bodies){
    if(y<body.minY()||y>body.maxY()||Math.abs(x-body.x())>Math.max(body.along(),body.across())+2||Math.abs(z-body.z())>Math.max(body.along(),body.across())+2)continue;
    if(body.contains(x,y,z)){expected=true;if(ore==body.ore())correct=true;}
   }
   if(expected){eligible++;if(correct)matched++;else if(ore==null)missed++;}
  }
  return "ORE_AUDIT total="+total+" deep="+deep+" expected="+eligible+" matched="+matched+" missedRock="+missed+" types="+counts;
 }
}
