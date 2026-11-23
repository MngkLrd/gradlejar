package com.kirill.simplesorter.sorting;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
public record ContainerRef(BlockPos pos,IItemHandler handler,Object identity,ServerLevel level){
 public int slots(){return handler.getSlots();}
}
