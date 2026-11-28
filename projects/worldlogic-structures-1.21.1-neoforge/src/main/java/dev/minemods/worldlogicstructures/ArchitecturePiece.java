package dev.minemods.worldlogicstructures;

import dev.minemods.worldlogicstructures.api.StructuresApi;
import dev.minemods.worldlogicstructures.api.StructuresApi.Design;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.*;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import java.util.*;

final class ArchitecturePiece extends StructurePiece {
 private final String designId;
 private final int x,y,z;
 private final Rotation rotation;
 private final long seed;
 ArchitecturePiece(Design d,int x,int y,int z,Rotation rotation,long seed){
  super(StructuresMod.ARCHITECTURE.get(),0,new BoundingBox(x-8,y-7,z-8,x+8,y+Architecture.maxHeight(d)+1,z+8));
  this.designId=d.id();this.x=x;this.y=y;this.z=z;this.rotation=rotation;this.seed=seed;
 }
 ArchitecturePiece(CompoundTag tag){
  super(StructuresMod.ARCHITECTURE.get(),tag);
  designId=tag.getString("Design");x=tag.getInt("X");y=tag.getInt("Y");z=tag.getInt("Z");
  rotation=Rotation.values()[Math.floorMod(tag.getInt("Rotation"),4)];seed=tag.getLong("Seed");
 }
 @Override protected void addAdditionalSaveData(StructurePieceSerializationContext c,CompoundTag tag){
  tag.putString("Design",designId);tag.putInt("X",x);tag.putInt("Y",y);tag.putInt("Z",z);tag.putInt("Rotation",rotation.ordinal());tag.putLong("Seed",seed);
 }
 private BlockPos global(BlockPos local){return Architecture.transform(local.getX(),local.getY(),local.getZ(),rotation).offset(x,y,z);}
 @Override public void postProcess(WorldGenLevel level,StructureManager manager,ChunkGenerator generator,RandomSource random,BoundingBox clip,ChunkPos chunk,BlockPos pivot){
  var d=StructuresApi.CATALOGUE.designs().stream().filter(v->v.id().equals(designId)).findFirst().orElse(null);
  if(d==null)return;
  var plan=Architecture.build(d,seed);
  var palette=Architecture.palette(d.family());
  for(var footing:plan.footings){
   BlockPos at=global(footing);
   for(int depth=1;depth<=7;depth++){
    BlockPos below=at.below(depth);
    if(!clip.isInside(below)||!getBoundingBox().isInside(below))break;
    BlockState existing=level.getBlockState(below);
    if(existing.isFaceSturdy(level,below,Direction.UP)&&existing.getFluidState().isEmpty())break;
    level.setBlock(below,(d.raised()>0?palette.frame():palette.foundation()).defaultBlockState(),2);
   }
  }
  // Deterministic plan; supports and load-bearing blocks precede ladders and furnishings.
  var ordered=new ArrayList<>(plan.cells.entrySet());
  ordered.sort(Comparator.comparingInt(e->e.getKey().getY()));
  for(var cell:ordered){
   BlockPos at=global(cell.getKey());
   if(clip.isInside(at)&&getBoundingBox().isInside(at))level.setBlock(at,cell.getValue().rotate(rotation),2);
  }
 }
}
