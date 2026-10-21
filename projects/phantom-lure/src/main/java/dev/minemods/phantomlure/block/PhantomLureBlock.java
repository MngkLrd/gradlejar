package dev.minemods.phantomlure.block;

import com.mojang.serialization.MapCodec;
import dev.minemods.phantomlure.blockentity.PhantomLureBlockEntity;
import dev.minemods.phantomlure.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public final class PhantomLureBlock extends BaseEntityBlock {
    public static final MapCodec<PhantomLureBlock> CODEC = simpleCodec(PhantomLureBlock::new);
    public static final BooleanProperty CHARGED = BooleanProperty.create("charged");
    private static final VoxelShape SHAPE = Shapes.or(
        Block.box(1, 0, 1, 15, 3, 15),
        Block.box(3, 3, 3, 13, 5, 13),
        Block.box(2, 3, 2, 4, 8, 4),
        Block.box(12, 3, 2, 14, 8, 4),
        Block.box(2, 3, 12, 4, 8, 14),
        Block.box(12, 3, 12, 14, 8, 14)
    );

    public PhantomLureBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(CHARGED, false));
    }

    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CHARGED);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        // A lure is a nighttime outdoor trap. Refuse placement instead of consuming it in daylight.
        if (!PhantomLureBlockEntity.isLureNight(level) || !level.canSeeSky(context.getClickedPos().above())) {
            return null;
        }
        return defaultBlockState();
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PhantomLureBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null
            : createTickerHelper(type, ModBlockEntities.LURE.get(), PhantomLureBlockEntity::serverTick);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level,
                                              BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(Items.DIAMOND)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (state.getValue(CHARGED)) return ItemInteractionResult.FAIL;
        if (!level.isClientSide) {
            if (!player.getAbilities().instabuild) stack.shrink(1);
            level.setBlock(pos, state.setValue(CHARGED, true), Block.UPDATE_ALL);
            level.levelEvent(2005, pos.above(), 0);
        }
        return ItemInteractionResult.SUCCESS;
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        // Manual dismantling returns the diamond; timed/sleep cancellation does not.
        if (!level.isClientSide && !player.getAbilities().instabuild && state.getValue(CHARGED)) {
            popResource(level, pos, new ItemStack(Items.DIAMOND));
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(3) != 0) return;
        double x = pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.6;
        double z = pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.6;
        level.addParticle(state.getValue(CHARGED) ? ParticleTypes.END_ROD : ParticleTypes.PORTAL,
            x, pos.getY() + 0.55, z, 0, 0.02, 0);
    }
}
