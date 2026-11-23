package com.kirill.simplesorter.sorting;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import java.util.*;
public final class ContainerScanner {
 private ContainerScanner(){}
 public static List<ContainerRef> scanRadius(ServerLevel level,Player player,BlockPos center,int radius){
  int safe=Math.max(1,Math.min(radius,32));
  return scanBoxInRadius(level,player,center.offset(-safe,-safe,-safe),center.offset(safe,safe,safe),center,safe);
 }
 public static List<ContainerRef> scanBoxInRadius(ServerLevel level,Player player,BlockPos min,BlockPos max,BlockPos center,int radius){
  List<BlockPos> candidates=new ArrayList<>();
  int r2=radius*radius;
  for(int cx=Math.floorDiv(min.getX(),16);cx<=Math.floorDiv(max.getX(),16);cx++){
   for(int cz=Math.floorDiv(min.getZ(),16);cz<=Math.floorDiv(max.getZ(),16);cz++){
    if(!level.hasChunk(cx,cz))continue;
    for(BlockPos pos:level.getChunk(cx,cz).getBlockEntitiesPos()){
     if(pos.getX()<min.getX()||pos.getX()>max.getX()||pos.getY()<min.getY()||pos.getY()>max.getY()||pos.getZ()<min.getZ()||pos.getZ()>max.getZ())continue;
     int dx=pos.getX()-center.getX(),dy=pos.getY()-center.getY(),dz=pos.getZ()-center.getZ();
     if(dx*dx+dy*dy+dz*dz<=r2)candidates.add(pos.immutable());
    }
   }
  }
  candidates.sort(Comparator.comparingDouble(p->p.distSqr(center)));
  List<ContainerRef> result=new ArrayList<>();
  for(BlockPos pos:candidates.subList(0,Math.min(160,candidates.size()))){
   BlockEntity be=level.getBlockEntity(pos);
   if(be==null)continue;
   var capability=be.getCapability(ForgeCapabilities.ITEM_HANDLER,null).orElse(null);
   if(capability!=null && capability.getSlots()>0)
    result.add(new ContainerRef(pos,new ForgeItemHandlerAdapter(capability),be,level));
   else if(be instanceof Container container&&container.getContainerSize()>0)
    result.add(new ContainerRef(pos,new ContainerItemHandler(container),be,level));
  }
  return result;
 }
}
