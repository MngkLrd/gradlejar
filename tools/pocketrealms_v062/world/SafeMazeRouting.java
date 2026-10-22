package com.kirill.pocketrealms.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

/**
 * Runtime topology guard. Test the route from every active player to the
 * terminal centre BEFORE sealing a shortcut or removing a trail floor.
 * Uses actual collision geometry, not an outdated generation-time mask.
 */
public final class SafeMazeRouting {
    private SafeMazeRouting() {}

    public static boolean canAlter(ServerLevel level, PocketRunEntry run, BlockPos blockedFloor) {
        boolean checked=false;
        for (ServerPlayer player : level.players()) {
            if (!run.contains(player.blockPosition()) || !player.isAlive()) continue;
            checked=true;
            if (!reachable(level,run,player.blockPosition(),run.exitPos,blockedFloor))
                return false;
        }
        return checked;
    }

    private static boolean reachable(ServerLevel level, PocketRunEntry run,
                                     BlockPos current, BlockPos goal, BlockPos forbidden) {
        int y=run.spawnPos.getY();
        BlockPos first=new BlockPos(current.getX(),y,current.getZ());
        BlockPos destination=new BlockPos(goal.getX(),y,goal.getZ());
        if (first.equals(destination))return true;
        if (!walkable(level,run,first,forbidden))return false;
        ArrayDeque<BlockPos> queue=new ArrayDeque<>();
        Set<Long> visited=new HashSet<>();
        queue.add(first);visited.add(first.asLong());
        while(!queue.isEmpty()){
            BlockPos cell=queue.removeFirst();
            for(Direction d:Direction.Plane.HORIZONTAL){
                BlockPos next=cell.relative(d);
                if(next.equals(destination))return walkable(level,run,next,forbidden);
                if(visited.add(next.asLong()) && walkable(level,run,next,forbidden))
                    queue.addLast(next);
            }
        }
        return false;
    }

    private static boolean walkable(ServerLevel level,PocketRunEntry run,BlockPos floor,BlockPos forbidden) {
        if(floor.equals(forbidden))return false;
        if(floor.getX()<=run.minX || floor.getZ()<=run.minZ
                || floor.getX()>=run.maxX || floor.getZ()>=run.maxZ) return false;
        if(!level.getBlockState(floor).isFaceSturdy(level,floor,Direction.UP)) return false;
        BlockPos legs=floor.above();
        BlockPos head=legs.above();
        return level.getBlockState(legs).getCollisionShape(level,legs).isEmpty()
                && level.getBlockState(head).getCollisionShape(level,head).isEmpty();
    }
}
