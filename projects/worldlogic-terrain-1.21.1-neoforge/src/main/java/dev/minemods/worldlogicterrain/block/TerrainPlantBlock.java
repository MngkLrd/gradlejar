package dev.minemods.worldlogicterrain.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class TerrainPlantBlock extends BushBlock {
    public static final MapCodec<TerrainPlantBlock> CODEC = simpleCodec(TerrainPlantBlock::new);
    private static final TagKey<Block> DRY = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("worldlogicterrain", "dry_plants"));
    private static final TagKey<Block> FROST = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("worldlogicterrain", "frost_plants"));
    private static final TagKey<Block> COVER = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("worldlogicterrain", "ground_cover"));
    private static final VoxelShape LOW = Block.box(0,0,0,16,2,16);
    private static final VoxelShape PLANT = Block.box(2,0,2,14,15,14);
    public TerrainPlantBlock(Properties properties) { super(properties); }
    @Override public MapCodec<TerrainPlantBlock> codec() { return CODEC; }
    @Override protected boolean mayPlaceOn(BlockState floor, BlockGetter level, BlockPos pos) {
        if (super.mayPlaceOn(floor, level, pos) || floor.is(Blocks.MUD)) return true;
        if(defaultBlockState().is(FROST)&&floor.is(Blocks.SNOW_BLOCK)&&level.getBlockState(pos.below()).is(BlockTags.DIRT))return true;
        return defaultBlockState().is(DRY) && floor.is(BlockTags.SAND);
    }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.is(COVER) ? LOW : PLANT;
    }
}
