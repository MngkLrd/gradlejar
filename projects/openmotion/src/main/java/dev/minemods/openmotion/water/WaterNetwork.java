package dev.minemods.openmotion.water;

import dev.minemods.openmotion.block.SprinklerBlock;
import dev.minemods.openmotion.block.WaterPipeBlock;
import dev.minemods.openmotion.block.WaterPumpBlock;
import dev.minemods.openmotion.block.WaterValveBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Bounded topology-aware pipe simulation. No infinite water, world fluid placement,
 * block entities on pipes, chunk tickets or client-side logic.
 */
public final class WaterNetwork {
    private static final int MAX_COMPONENT_BLOCKS = 384;
    private WaterNetwork() { }

    public static boolean isNetworkNode(BlockState state) {
        Block block = state.getBlock();
        return block instanceof WaterPipeBlock || block instanceof WaterPumpBlock
                || block instanceof SprinklerBlock;
    }

    private static boolean conducting(BlockState state) {
        if (state.getBlock() instanceof WaterValveBlock) return WaterValveBlock.canFlow(state);
        return state.getBlock() instanceof WaterPipeBlock || state.getBlock() instanceof WaterPumpBlock;
    }

    public static boolean hasSourceWater(ServerLevel level, BlockPos pumpPos) {
        // Pump touching or standing next to a lake; do not generate or consume fluid.
        for (int y = -2; y <= 0; y++) {
            for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++) {
                if (x * x + z * z > 4) continue;
                BlockPos test = pumpPos.offset(x, y, z);
                if (!level.hasChunkAt(test)) continue;
                var fluid = level.getFluidState(test);
                if (fluid.isSource() && fluid.is(FluidTags.WATER)) return true;
            }
        }
        return false;
    }

    /** Finds physical component even through shut valves, then fills only pump-reachable subgraph. */
    public static void refresh(ServerLevel level, BlockPos start) {
        if (!level.hasChunkAt(start)) return;
        BlockState initial = level.getBlockState(start);
        if (!isNetworkNode(initial)) return;

        Map<BlockPos, BlockState> component = new HashMap<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(start);
        while (!queue.isEmpty() && component.size() < MAX_COMPONENT_BLOCKS) {
            BlockPos pos = queue.removeFirst();
            if (component.containsKey(pos) || !level.hasChunkAt(pos)) continue;
            BlockState state = level.getBlockState(pos);
            if (!isNetworkNode(state)) continue;
            component.put(pos, state);
            for (Direction d : Direction.values()) {
                BlockPos next = pos.relative(d);
                if (!component.containsKey(next) && level.hasChunkAt(next)
                        && isNetworkNode(level.getBlockState(next))) queue.add(next);
            }
        }
        if (component.isEmpty()) return;

        Set<BlockPos> fed = new HashSet<>();
        ArrayDeque<BlockPos> wave = new ArrayDeque<>();
        for (Map.Entry<BlockPos, BlockState> entry : component.entrySet()) {
            if (entry.getValue().getBlock() instanceof WaterPumpBlock
                    && !level.hasNeighborSignal(entry.getKey())
                    && hasSourceWater(level, entry.getKey())) {
                fed.add(entry.getKey());
                wave.add(entry.getKey());
            }
        }
        while (!wave.isEmpty()) {
            BlockPos pos = wave.removeFirst();
            BlockState state = component.get(pos);
            if (state == null || !conducting(state)) continue; // sprinkler is a terminal
            for (Direction direction : Direction.values()) {
                BlockPos next = pos.relative(direction);
                BlockState ns = component.get(next);
                if (ns == null || fed.contains(next)) continue;
                // A closed valve cannot receive or transmit water.
                if (ns.getBlock() instanceof WaterValveBlock && !conducting(ns)) continue;
                if (ns.getBlock() instanceof SprinklerBlock && level.hasNeighborSignal(next)) continue;
                fed.add(next);
                wave.add(next);
            }
        }

        for (Map.Entry<BlockPos, BlockState> entry : component.entrySet()) {
            BlockPos pos = entry.getKey();
            BlockState state = entry.getValue();
            boolean wet = fed.contains(pos);
            if (state.getBlock() instanceof WaterValveBlock) {
                boolean locked = level.hasNeighborSignal(pos);
                BlockState newState = state.setValue(WaterValveBlock.LOCKED, locked)
                        .setValue(WaterPipeBlock.FLOWING, wet && !locked && !state.getValue(WaterValveBlock.CLOSED));
                if (newState != state) level.setBlock(pos, newState, Block.UPDATE_CLIENTS);
            } else if (state.getBlock() instanceof WaterPipeBlock) {
                if (state.getValue(WaterPipeBlock.FLOWING) != wet)
                    level.setBlock(pos, state.setValue(WaterPipeBlock.FLOWING, wet), Block.UPDATE_CLIENTS);
            } else if (state.getBlock() instanceof WaterPumpBlock) {
                if (state.getValue(WaterPumpBlock.RUNNING) != wet)
                    level.setBlock(pos, state.setValue(WaterPumpBlock.RUNNING, wet), Block.UPDATE_CLIENTS);
            } else if (state.getBlock() instanceof SprinklerBlock) {
                if (state.getValue(SprinklerBlock.ACTIVE) != wet)
                    level.setBlock(pos, state.setValue(SprinklerBlock.ACTIVE, wet), Block.UPDATE_CLIENTS);
            }
        }
    }
}
