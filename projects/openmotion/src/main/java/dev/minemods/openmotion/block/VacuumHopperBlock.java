package dev.minemods.openmotion.block;

import com.mojang.serialization.MapCodec;
import dev.minemods.openmotion.blockentity.VacuumHopperBlockEntity;
import dev.minemods.openmotion.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Independent item collector with a small persistent buffer.
 * Neighboring machines can access the nine slots using NeoForge item capabilities.
 */
public final class VacuumHopperBlock extends BaseEntityBlock {
    public static final MapCodec<VacuumHopperBlock> CODEC = simpleCodec(VacuumHopperBlock::new);
    private static final VoxelShape SHAPE = box(1, 0, 1, 15, 14, 15);

    public VacuumHopperBlock(Properties properties) { super(properties); }

    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Override
    protected VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level,
                                  BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new VacuumHopperBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null
                : createTickerHelper(type, ModBlockEntities.VACUUM_HOPPER.get(), VacuumHopperBlockEntity::serverTick);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                                Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof VacuumHopperBlockEntity hopper) {
            // Empty hand retrieves the first nonempty slot; no custom GUI is required.
            if (player.getMainHandItem().isEmpty()) {
                ItemStack extracted = hopper.takeFirstStack();
                if (!extracted.isEmpty()) player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, extracted);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos,
                            BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && !level.isClientSide
                && level.getBlockEntity(pos) instanceof VacuumHopperBlockEntity hopper) {
            // Drop the complete buffer before the block entity is discarded.
            for (int i = 0; i < hopper.inventory().getSlots(); i++) {
                ItemStack stack = hopper.inventory().getStackInSlot(i);
                if (!stack.isEmpty()) {
                    Containers.dropItemStack(level, pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5, stack.copy());
                    hopper.inventory().setStackInSlot(i, ItemStack.EMPTY);
                }
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
