package dev.minemods.worldlogicterrain.block;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;

public final class TerrainTallPlantBlock extends DoublePlantBlock {
    public TerrainTallPlantBlock(Properties properties) { super(properties); }
    @Override protected boolean mayPlaceOn(BlockState floor, BlockGetter level, BlockPos pos) {
        return super.mayPlaceOn(floor,level,pos)||floor.is(Blocks.MUD)||floor.is(Blocks.CLAY);
    }
}
