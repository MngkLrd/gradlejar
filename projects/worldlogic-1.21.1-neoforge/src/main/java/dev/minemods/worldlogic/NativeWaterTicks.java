package dev.minemods.worldlogic;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
/** Direct chunk writes bypass LiquidBlock.onPlace. Re-arm exposed native fluids
 * once per chunk load, also repairing old generated rivers without retrogen.
 * Only loaded chunks and bounded section batches are touched. */
final class NativeWaterTicks{
 private NativeWaterTicks(){}
 private static final Map<ServerLevel,LinkedHashMap<ChunkPos,Integer>> PENDING=new WeakHashMap<>();
 private static final Direction[] DOWN_OR_SIDE={Direction.DOWN,Direction.NORTH,Direction.SOUTH,Direction.WEST,Direction.EAST};
 static boolean nativeBlock(BlockState s){return s.getBlock() instanceof net.minecraft.world.level.block.LiquidBlock&&NaturalFluids.kind(s.getFluidState().getType())!=dev.minemods.worldlogic.api.WorldLogicApi.WaterKind.UNKNOWN;}
 static boolean needsTick(ServerLevel l,BlockPos p){
  var s=l.getBlockState(p);if(!nativeBlock(s))return false;
  if(!s.getFluidState().isSource())return true;
  for(var d:DOWN_OR_SIDE){var q=p.relative(d);if(l.isOutsideBuildHeight(q))continue;
   if(!l.hasChunkAt(q))return true;
   if(l.getBlockState(q).isAir())return true;
  }return false;
 }
 static void arm(ServerLevel l,BlockPos p){if(needsTick(l,p)){var f=l.getFluidState(p);l.scheduleTick(p,f.getType(),f.getType().getTickDelay(l));}}
 static void load(ChunkEvent.Load e){
  if(e.getLevel() instanceof ServerLevel l&&e.getChunk() instanceof LevelChunk c&&NaturalWaterGeneration.enabled(l.getChunkSource().getGenerator()))
   PENDING.computeIfAbsent(l,v->new LinkedHashMap<>()).putIfAbsent(c.getPos(),0);
 }
 static void tick(ServerTickEvent.Post e){
  for(var l:e.getServer().getAllLevels()){
   var pending=PENDING.get(l);if(pending==null)continue;
   for(int budget=0;budget<8&&!pending.isEmpty();budget++){
    var entry=pending.entrySet().iterator().next();var pos=entry.getKey();int i=entry.getValue();
    var c=l.getChunkSource().getChunkNow(pos.x,pos.z);if(c==null||i>=c.getSections().length){pending.remove(pos);continue;}
    var section=c.getSections()[i];int y0=c.getMinBuildHeight()+i*16;
    if(section.maybeHas(NativeWaterTicks::nativeBlock)){
     var p=new BlockPos.MutableBlockPos();
     for(int x=0;x<16;x++)for(int z=0;z<16;z++)for(int y=0;y<16;y++){
      if(nativeBlock(section.getBlockState(x,y,z)))arm(l,p.set(pos.getMinBlockX()+x,y0+y,pos.getMinBlockZ()+z));
     }
    }
    pending.remove(pos);if(i+1<c.getSections().length)pending.put(pos,i+1);
   }
  }
 }
}
