package com.kirill.towerveil.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.state.BlockState;

public final class TowerBuilder {
    private static final int CX = 24;
    private static final int CZ = 0;
    private static final int BASE = 64;
    private static final int R = 5;
    private static final BlockPos MARKER = new BlockPos(CX, BASE, CZ);

    private TowerBuilder() { }

    public static void ensureBuilt(ServerLevel world) {
        if (world.getBlockState(MARKER).is(Blocks.LODESTONE)) return;
        build(world);
    }

    private static void build(ServerLevel world) {
        for (int y = BASE + 1; y <= BASE + 37; y++) {
            for (int dx = -R; dx <= R; dx++) {
                for (int dz = -R; dz <= R; dz++) {
                    if (Math.abs(dx) != R && Math.abs(dz) != R) continue;
                    Block material = ((Math.abs(dx) == R && Math.abs(dz) == R) ||
                            (y % 9 == 0 && (Math.abs(dx) == R || Math.abs(dz) == R)))
                            ? Blocks.POLISHED_DEEPSLATE : Blocks.STONE_BRICKS;
                    if ((y - BASE) % 9 >= 4 && (y - BASE) % 9 <= 5 &&
                            ((Math.abs(dx) == R && dz == -1) || (Math.abs(dz) == R && dx == 0))) {
                        material = Blocks.GLASS_PANE;
                    }
                    if (dx == -R && dz == 0 && y <= BASE + 3) material = Blocks.AIR;
                    set(world, CX + dx, y, CZ + dz, material);
                }
            }
        }
        for (int floor = 0; floor <= 4; floor++) {
            int y = BASE + floor * 9;
            for (int dx = -R + 1; dx < R; dx++) {
                for (int dz = -R + 1; dz < R; dz++) {
                    if (floor != 0 && dx == -R + 1 && dz == 2) continue;
                    set(world, CX + dx, y, CZ + dz, Blocks.STONE_BRICKS);
                }
            }
            if (floor < 4) {
                set(world, CX + 2, y + 7, CZ + 2, Blocks.GLOWSTONE);
                set(world, CX - 2, y + 7, CZ - 2, Blocks.GLOWSTONE);
            }
        }
        BlockState ladder = Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.EAST);
        for (int y = BASE + 1; y < BASE + 38; y++) {
            world.setBlock(new BlockPos(CX - 4, y, CZ + 2), ladder, 3);
        }
        for (int d = -R; d <= R; d++) {
            if (d % 2 == 0) {
                set(world, CX + d, BASE + 38, CZ - R, Blocks.COBBLED_DEEPSLATE_WALL);
                set(world, CX + d, BASE + 38, CZ + R, Blocks.COBBLED_DEEPSLATE_WALL);
                set(world, CX - R, BASE + 38, CZ + d, Blocks.COBBLED_DEEPSLATE_WALL);
                set(world, CX + R, BASE + 38, CZ + d, Blocks.COBBLED_DEEPSLATE_WALL);
            }
        }
        set(world, MARKER.getX(), MARKER.getY(), MARKER.getZ(), Blocks.LODESTONE);
    }

    private static void set(ServerLevel world, int x, int y, int z, Block block) {
        world.setBlock(new BlockPos(x, y, z), block.defaultBlockState(), 3);
    }
}
