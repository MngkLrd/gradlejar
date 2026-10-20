package dev.minemods.openmotion.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/** Low-cost water-source-powered irrigation machine. No tanks, pipes or client-only code. */
public final class SprinklerBlock extends Block {
    public static final MapCodec<SprinklerBlock> CODEC = simpleCodec(SprinklerBlock::new);
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");
    private static final VoxelShape SHAPE = box(1.5, 0, 1.5, 14.5, 15.5, 14.5);
    private static final int INTERVAL = 100;

    public SprinklerBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(ACTIVE, false));
    }

    @Override protected MapCodec<? extends Block> codec() { return CODEC; }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ACTIVE);
    }

    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
                                           CollisionContext context) { return SHAPE; }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(ACTIVE, hasSource(context.getLevel(), context.getClickedPos()));
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos,
                           BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide && !oldState.is(state.getBlock())) level.scheduleTick(pos, this, 2);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos,
                                   Block fromBlock, BlockPos fromPos, boolean isMoving) {
        if (!level.isClientSide) level.scheduleTick(pos, this, 2);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        boolean active = hasSource(level, pos) && !level.hasNeighborSignal(pos);
        if (state.getValue(ACTIVE) != active)
            level.setBlock(pos, state.setValue(ACTIVE, active), Block.UPDATE_ALL);
        if (active) {
            // One supported target per five seconds: noticeable but safe for modded crops.
            for (int trial = 0; trial < 24; trial++) {
                int dx = random.nextInt(9) - 4;
                int dz = random.nextInt(9) - 4;
                if (dx * dx + dz * dz > 16) continue;
                BlockPos farmPos = pos.offset(dx, -(1 + random.nextInt(3)), dz);
                BlockState crop = level.getBlockState(farmPos);
                if (crop.getBlock() instanceof BonemealableBlock growable
                        && growable.isValidBonemealTarget(level, farmPos, crop)
                        && growable.isBonemealSuccess(level, random, farmPos, crop)) {
                    growable.performBonemeal(level, random, farmPos, crop);
                    break;
                }
            }
            level.sendParticles(ParticleTypes.SPLASH,
                    pos.getX() + .5, pos.getY() + .95, pos.getZ() + .5,
                    5, .7, .3, .7, .07);
        }
        level.scheduleTick(pos, this, INTERVAL);
    }

    private static boolean hasSource(Level level, BlockPos pos) {
        // Searches local irrigation sources; no liquid is created or consumed.
        for (int y = -2; y <= 0; y++)
            for (int x = -2; x <= 2; x++)
                for (int z = -2; z <= 2; z++) {
                    if (x * x + z * z > 4) continue;
                    if (level.getFluidState(pos.offset(x, y, z)).isSource()
                            && level.getFluidState(pos.offset(x, y, z)).is(net.minecraft.tags.FluidTags.WATER))
                        return true;
                }
        return false;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(ACTIVE) || random.nextInt(4) != 0) return;
        double x = pos.getX() + .5 + (random.nextDouble() - .5) * 1.6;
        double z = pos.getZ() + .5 + (random.nextDouble() - .5) * 1.6;
        level.addParticle(ParticleTypes.SPLASH, x, pos.getY() + .75, z, 0, -.04, 0);
    }
}
