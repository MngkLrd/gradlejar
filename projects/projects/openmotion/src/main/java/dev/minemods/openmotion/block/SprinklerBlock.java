package dev.minemods.openmotion.block;

import com.mojang.serialization.MapCodec;
import dev.minemods.openmotion.config.OpenMotionConfig;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.BlockPos;
import dev.minemods.openmotion.water.WaterNetwork;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.CropBlock;
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
import net.minecraft.world.phys.shapes.Shapes;
import org.jetbrains.annotations.Nullable;

/** Pipe-fed irrigation machine. It will not function from adjacent lake water alone. */
public final class SprinklerBlock extends Block {
    public static final MapCodec<SprinklerBlock> CODEC = simpleCodec(SprinklerBlock::new);
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");
    private static final VoxelShape SHAPE = Shapes.or(
            box(2, 0, 2, 14, 3.7, 14),
            box(6.0, 3.7, 6.0, 10, 13.65, 10),
            box(1, 10.8, 6.2, 15, 13.6, 9.8),
            box(6.2, 10.8, 1, 9.8, 13.6, 15));
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
        return defaultBlockState().setValue(ACTIVE, false);
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
        WaterNetwork.refresh(level, pos);
        boolean active = level.getBlockState(pos).getValue(ACTIVE);
        if (state.getValue(ACTIVE) != active)
            level.setBlock(pos, state.setValue(ACTIVE, active), Block.UPDATE_ALL);
        if (active) {
            // One supported target per five seconds: noticeable but safe for modded crops.
            int radius = OpenMotionConfig.SPRINKLER_RADIUS.get();
            for (int trial = 0; trial < 24; trial++) {
                int dx = random.nextInt(radius * 2 + 1) - radius;
                int dz = random.nextInt(radius * 2 + 1) - radius;
                if (dx * dx + dz * dz > radius * radius) continue;
                BlockPos farmPos = pos.offset(dx, -(1 + random.nextInt(3)), dz);
                BlockState crop = level.getBlockState(farmPos);
                if ((crop.is(BlockTags.CROPS) || crop.getBlock() instanceof CropBlock)
                        && crop.getBlock() instanceof BonemealableBlock growable
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
        level.scheduleTick(pos, this, OpenMotionConfig.SPRINKLER_INTERVAL.get());
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (!level.isClientSide) {
            player.displayClientMessage(Component.translatable(
                    state.getValue(ACTIVE) ? "message.openmotion.sprinkler_on"
                            : "message.openmotion.sprinkler_off"), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(ACTIVE) || random.nextInt(4) != 0) return;
        double x = pos.getX() + .5 + (random.nextDouble() - .5) * 1.6;
        double z = pos.getZ() + .5 + (random.nextDouble() - .5) * 1.6;
        level.addParticle(ParticleTypes.SPLASH, x, pos.getY() + .75, z, 0, -.04, 0);
    }
}
