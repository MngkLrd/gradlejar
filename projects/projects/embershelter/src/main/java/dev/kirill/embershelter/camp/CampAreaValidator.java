package dev.kirill.embershelter.camp;
import dev.kirill.embershelter.util.BlockReplaceRules;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Validate full airspace but NEVER alter the floor (center.y - 1). */
public final class CampAreaValidator {
    public enum Result { OK, WATER, BLOCKED }
    private CampAreaValidator() {}
    public static Result check(ServerLevel level, ServerPlayer player, BlockPos center, List<CampBuildStep> steps) {
        Set<BlockPos> placements = new HashSet<>();
        for (CampBuildStep s : steps) placements.add(s.pos());
        if (placements.size() != steps.size()) return Result.BLOCKED;
        for (int x=-3;x<=3;x++) for(int z=-3;z<=3;z++) {
            BlockPos floor=center.offset(x,-1,z);
            if (!level.hasChunkAt(floor) || !level.mayInteract(player,floor)) return Result.BLOCKED;
            BlockState fs=level.getBlockState(floor);
            if(!fs.getFluidState().isEmpty()) return Result.WATER;
            if(!fs.isFaceSturdy(level,floor,Direction.UP)) return Result.BLOCKED;
            for(int y=0;y<4;y++) {
                BlockPos pos=center.offset(x,y,z);
                if(level.isOutsideBuildHeight(pos) || !level.hasChunkAt(pos) || !level.mayInteract(player,pos))
                    return Result.BLOCKED;
                BlockState state=level.getBlockState(pos);
                if(!state.getFluidState().isEmpty()) return Result.WATER;
                if(!BlockReplaceRules.replaceable(state)) return Result.BLOCKED;
            }
        }
        return Result.OK;
    }
    public static void clearSoftBlocks(ServerLevel level, BlockPos center) {
        for(int x=-3;x<=3;x++) for(int z=-3;z<=3;z++) for(int y=0;y<4;y++) {
            BlockPos pos=center.offset(x,y,z);
            if(!level.getBlockState(pos).isAir())
                level.setBlock(pos,Blocks.AIR.defaultBlockState(),3);
        }
    }
}
