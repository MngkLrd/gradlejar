package dev.minemods.thermal.server;
import dev.minemods.thermal.content.ThermalContent;
import dev.minemods.worldlogic.api.WorldLogicApi;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
/** New generated chunks only; no replacement of existing blocks, no force-load. */
public final class SettlementClimate {
 private record Pending(ServerLevel level,ChunkPos pos,int attempts){}
 private static final Queue<Pending> QUEUE=new ConcurrentLinkedQueue<>();
 private SettlementClimate(){}
 public static void chunk(ChunkEvent.Load event){
  if(event.isNewChunk()&&event.getLevel() instanceof ServerLevel l&&l.dimension().equals(Level.OVERWORLD)&&event.getChunk() instanceof LevelChunk c&&QUEUE.size()<1024&&!c.getAllReferences().isEmpty())
   QUEUE.add(new Pending(l,c.getPos(),0));
 }
 public static void tick(ServerTickEvent.Post event){
  for(int i=0;i<Math.min(4,QUEUE.size());i++){
   Pending p=QUEUE.poll();if(p==null)return;
   if(p.level().getServer()!=event.getServer())continue;
   LevelChunk c=p.level().getChunkSource().getChunkNow(p.pos().x,p.pos().z);
   if(c==null){if(p.attempts()<40)QUEUE.add(new Pending(p.level(),p.pos(),p.attempts()+1));continue;}
   if(!equip(p.level(),c)&&p.attempts()<40)QUEUE.add(new Pending(p.level(),p.pos(),p.attempts()+1));
  }
 }
 static int choose(double climate){return climate<=7?1:climate>=29?-1:0;}
 static boolean equip(ServerLevel l,LevelChunk c){
  var registry=l.registryAccess().registryOrThrow(Registries.STRUCTURE);
  for(var entry:c.getAllReferences().entrySet()){
   var id=registry.getKey(entry.getKey());
   if(id==null||!(id.getPath().startsWith("village_")||id.getPath().equals("igloo")))continue;
   for(long packed:entry.getValue().toLongArray()){
    ChunkPos cp=new ChunkPos(packed);
    if(l.getChunkSource().getChunkNow(cp.x,cp.z)==null)return false;
   }
   for(BlockPos bed:List.copyOf(c.getBlockEntitiesPos())){
    BlockState s=c.getBlockState(bed);
    if(!(s.getBlock() instanceof BedBlock)||s.getValue(BedBlock.PART)!=BedPart.HEAD)continue;
    var start=l.structureManager().getStructureWithPieceAt(bed,entry.getKey());
    if(!start.isValid())continue;
    var piece=start.getPieces().stream().filter(x->x.getBoundingBox().isInside(bed)).findFirst().orElse(null);
    if(piece==null)continue;
    var env=WorldLogicApi.environment(l,bed).orElse(null);if(env==null)continue;
    double climate=env.regionalClimate().map(r->14+28*r.thermalIndex()).orElse(env.ambientCelsiusEstimate());
    int kind=id.getPath().equals("igloo")?1:choose(climate);if(kind==0)continue;
    install(l,c,bed,piece.getBoundingBox(),kind);
   }
  }
  return true;
 }
 static boolean install(ServerLevel l,LevelChunk c,BlockPos bed,net.minecraft.world.level.levelgen.structure.BoundingBox bounds,int kind){
  // One per nearby bedroom, never overwrite a door, bed, loot or a furniture block.
  for(BlockPos p:BlockPos.betweenClosed(bed.offset(-3,-1,-3),bed.offset(3,2,3))){
   if(!l.hasChunkAt(p)||!bounds.isInside(p))continue;
   var b=l.getBlockState(p);if(b.is(ThermalContent.HEARTH.get())||b.is(ThermalContent.COOLER.get()))return false;
  }
  for(int radius=2;radius<=3;radius++)for(Direction dir:Direction.Plane.HORIZONTAL){
   BlockPos p=bed.relative(dir,radius);
   if(!new ChunkPos(p).equals(c.getPos())||!bounds.isInside(p)||!l.isEmptyBlock(p)||!l.isEmptyBlock(p.above())||l.canSeeSky(p))continue;
   if(!l.getBlockState(p.below()).isFaceSturdy(l,p.below(),Direction.UP))continue;
   boolean wall=false;
   for(Direction face:Direction.Plane.HORIZONTAL){
    BlockPos n=p.relative(face);
    if(new ChunkPos(n).equals(c.getPos())&&l.getBlockState(n).isFaceSturdy(l,n,face.getOpposite())){wall=true;break;}
   }
   if(!wall)continue;
   var fixture=kind>0?ThermalContent.HEARTH.get():ThermalContent.COOLER.get();
   if(l.setBlockAndUpdate(p,fixture.defaultBlockState())){fixture.activate(l,p,l.getBlockState(p));return true;}
  }
  return false;
 }
}
