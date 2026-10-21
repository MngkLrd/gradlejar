package dev.minemods.openmotion.water;

import dev.minemods.openmotion.block.SprinklerBlock;
import dev.minemods.openmotion.block.WaterPipeBlock;
import dev.minemods.openmotion.block.WaterPumpBlock;
import dev.minemods.openmotion.block.WaterValveBlock;
import dev.minemods.openmotion.config.OpenMotionConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Bounded loaded-chunk graph; a valve disconnects its own branch but not a parallel branch. */
public final class WaterNetwork {
    private WaterNetwork() { }

    public static boolean isNetworkNode(BlockState state) {
        Block block = state.getBlock();
        return block instanceof WaterPipeBlock || block instanceof WaterPumpBlock
                || block instanceof SprinklerBlock;
    }

    public static boolean hasSourceWater(ServerLevel level, BlockPos pumpPos) {
        for (int y=-2; y<=0; y++) for (int x=-2; x<=2; x++) for (int z=-2; z<=2; z++) {
            if (x*x+z*z>4) continue;
            BlockPos sample=pumpPos.offset(x,y,z);
            if (!level.hasChunkAt(sample)) continue;
            var fluid=level.getFluidState(sample);
            if (fluid.isSource() && fluid.is(FluidTags.WATER)) return true;
        }
        return false;
    }

    /**
     * Recomputed on topology/redstone changes and periodic pump/sprinkler ticks, not every
     * single pipe tick. For safety, oversized components are not partially flooded.
     * There are no chunk tickets or chunk loads.
     */
    public static void refresh(ServerLevel level, BlockPos start) {
        if (!level.hasChunkAt(start) || !isNetworkNode(level.getBlockState(start))) return;
        int limit=OpenMotionConfig.WATER_MAX_NODES.get();
        Map<Long,BlockState> nodes=new HashMap<>();
        Set<Long> enqueued=new HashSet<>();
        ArrayDeque<BlockPos> queue=new ArrayDeque<>();
        queue.add(start);
        enqueued.add(start.asLong());
        boolean truncated=false;
        while (!queue.isEmpty()) {
            if (nodes.size()>=limit) { truncated=true; break; }
            BlockPos pos=queue.removeFirst();
            if (!level.hasChunkAt(pos)) continue;
            BlockState state=level.getBlockState(pos);
            if (!isNetworkNode(state)) continue;
            nodes.put(pos.asLong(),state);
            for(Direction d:Direction.values()){
                BlockPos adjacent=pos.relative(d);
                long id=adjacent.asLong();
                if (!enqueued.contains(id) && level.hasChunkAt(adjacent)
                        && isNetworkNode(level.getBlockState(adjacent))) {
                    enqueued.add(id);
                    queue.addLast(adjacent);
                }
            }
        }
        if (nodes.isEmpty()) return;
        if (truncated) {
            // Prevent a distant pump being missed and giving a FALSE positive water source.
            // A big network needs the configured capacity raised. Existing wet nodes may
            // remain lit until fixed; never invent water in newly-added nodes.
            for(var e:nodes.entrySet()) setWet(level,BlockPos.of(e.getKey()),e.getValue(),false);
            return;
        }

        Map<Long,long[]> adjacency=new HashMap<>();
        Set<Long> sources=new HashSet<>();
        Set<Long> blocked=new HashSet<>();
        Set<Long> terminals=new HashSet<>();
        for(var e:nodes.entrySet()){
            long id=e.getKey();
            BlockPos pos=BlockPos.of(id);
            BlockState state=e.getValue();
            List<Long> neighbors=new ArrayList<>(6);
            for(Direction d:Direction.values()){
                long other=pos.relative(d).asLong();
                if(nodes.containsKey(other)) neighbors.add(other);
            }
            long[] adjacent=new long[neighbors.size()];
            for(int i=0;i<adjacent.length;i++)adjacent[i]=neighbors.get(i);
            adjacency.put(id,adjacent);
            if(state.getBlock() instanceof WaterPumpBlock) {
                if (!level.hasNeighborSignal(pos) && hasSourceWater(level,pos)) sources.add(id);
                else blocked.add(id);
            } else if(state.getBlock() instanceof WaterValveBlock) {
                if(state.getValue(WaterValveBlock.CLOSED)||level.hasNeighborSignal(pos))
                    blocked.add(id);
            } else if(state.getBlock() instanceof SprinklerBlock) {
                terminals.add(id);
                if(level.hasNeighborSignal(pos))blocked.add(id);
            }
        }

        Set<Long> wet=HydraulicGraph.reachable(adjacency,sources,blocked,terminals);
        for(var entry:nodes.entrySet())
            setWet(level,BlockPos.of(entry.getKey()),entry.getValue(),wet.contains(entry.getKey()));
    }

    private static void setWet(ServerLevel level,BlockPos pos,BlockState state,boolean water) {
        BlockState updated=state;
        if(state.getBlock() instanceof WaterValveBlock) {
            boolean locked=level.hasNeighborSignal(pos);
            updated=state.setValue(WaterValveBlock.LOCKED,locked)
                    .setValue(WaterPipeBlock.FLOWING,water && !locked && !state.getValue(WaterValveBlock.CLOSED));
        } else if(state.getBlock() instanceof WaterPipeBlock) {
            updated=state.setValue(WaterPipeBlock.FLOWING,water);
        } else if(state.getBlock() instanceof WaterPumpBlock) {
            updated=state.setValue(WaterPumpBlock.RUNNING,water);
        } else if(state.getBlock() instanceof SprinklerBlock) {
            updated=state.setValue(SprinklerBlock.ACTIVE,water);
        }
        if(updated!=state) level.setBlock(pos,updated,Block.UPDATE_CLIENTS);
    }
}
