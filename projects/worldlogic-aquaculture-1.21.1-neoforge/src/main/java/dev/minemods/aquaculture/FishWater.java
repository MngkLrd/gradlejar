package dev.minemods.aquaculture;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.Level;

/** Geometric fallback for water-tagged native fluids, independent of NeoForge's vanilla FluidType cache. */
public final class FishWater {
 private FishWater(){}
 public static boolean contact(Entity fish){
  Level level=fish.level();
  AABB box=fish.getBoundingBox().deflate(.001);
  int minX=(int)Math.floor(box.minX),maxX=(int)Math.floor(box.maxX);
  int minY=(int)Math.floor(box.minY),maxY=(int)Math.floor(box.maxY);
  int minZ=(int)Math.floor(box.minZ),maxZ=(int)Math.floor(box.maxZ);
  var pos=new BlockPos.MutableBlockPos();
  for(int x=minX;x<=maxX;x++)for(int y=minY;y<=maxY;y++)for(int z=minZ;z<=maxZ;z++){
   pos.set(x,y,z);
   if(level.isOutsideBuildHeight(pos)||!level.hasChunkAt(pos))continue;
   var state=level.getFluidState(pos);
   if(state.is(FluidTags.WATER)&&y+state.getHeight(level,pos)>box.minY+.01&&box.maxY>y)return true;
  }
  return false;
 }
}
