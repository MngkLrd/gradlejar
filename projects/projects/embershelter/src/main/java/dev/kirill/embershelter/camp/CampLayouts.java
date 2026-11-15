package dev.kirill.embershelter.camp;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;

/** A seven-by-seven camp, with a front opening oriented toward the player. */
public final class CampLayouts {
    private CampLayouts() {}

    public static List<CampBuildStep> create(BlockPos center, Direction front) {
        if (!front.getAxis().isHorizontal()) {
            throw new IllegalArgumentException("Camp front must be horizontal");
        }
        Direction right = front.getClockWise();
        List<CampBuildStep> steps = new ArrayList<>();
        add(steps, center, right, front, 0, 0, 0, Blocks.CAMPFIRE.defaultBlockState());
        // Perimeter: octagonal corners, open three-block entrance at the front.
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                boolean perimeter = Math.max(Math.abs(x), Math.abs(z)) == 3;
                boolean corner = Math.abs(x) == 3 && Math.abs(z) == 3;
                boolean entrance = z == 3 && Math.abs(x) <= 1;
                if (perimeter && !corner && !entrance) {
                    add(steps, center, right, front, x, 0, z, Blocks.OAK_FENCE.defaultBlockState());
                }
            }
        }
        // Six roof-support logs; roof remains clear of the central campfire.
        for (int y = 0; y < 3; y++) {
            for (int x : new int[] { -2, 2 }) {
                add(steps, center, right, front, x, y, -2, Blocks.OAK_LOG.defaultBlockState());
            }
        }
        // Eight wool pieces form a tiny lean-to, built after supports.
        for (int x = -1; x <= 1; x++) {
            add(steps, center, right, front, x, 3, -2, Blocks.WHITE_WOOL.defaultBlockState());
        }
        for (int x = -1; x <= 1; x++) {
            add(steps, center, right, front, x, 2, -1, Blocks.WHITE_WOOL.defaultBlockState());
        }
        for (int x : new int[] { -1, 1 }) {
            add(steps, center, right, front, x, 1, -2, Blocks.WHITE_WOOL.defaultBlockState());
        }
        // Storage and a low horizontal oak log seat.
        add(steps, center, right, front, 2, 0, 1, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, front));
        add(steps, center, right, front, -2, 0, 1,
                Blocks.OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, right.getAxis()));
        // Two fence-top torches.
        add(steps, center, right, front, -3, 1, 0, Blocks.TORCH.defaultBlockState());
        add(steps, center, right, front, 3, 1, 0, Blocks.TORCH.defaultBlockState());
        return List.copyOf(steps);
    }

    private static void add(List<CampBuildStep> list, BlockPos origin, Direction right,
                            Direction front, int x, int y, int z, BlockState block) {
        list.add(new CampBuildStep(origin.relative(right, x).relative(front, z).above(y), block));
    }
}
