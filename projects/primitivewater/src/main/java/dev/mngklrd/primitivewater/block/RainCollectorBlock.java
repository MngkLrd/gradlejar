package dev.mngklrd.primitivewater.block;

import dev.mngklrd.primitivewater.blockentity.RainCollectorBlockEntity;
import dev.mngklrd.primitivewater.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class RainCollectorBlock extends BaseEntityBlock {
    private static final VoxelShape SHAPE = Shapes.or(
        box(2, 0, 2, 14, 2, 14),
        box(2, 2, 2, 4, 11, 14),
        box(12, 2, 2, 14, 11, 14),
        box(4, 2, 2, 12, 11, 4),
        box(4, 2, 12, 12, 11, 14)
    );
    public RainCollectorBlock(BlockBehaviour.Properties properties) { super(properties); }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return SHAPE; }
    @Nullable @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new RainCollectorBlockEntity(pos,state); }
    @Nullable @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.COLLECTOR.get(), RainCollectorBlockEntity::serverTick);
    }
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof RainCollectorBlockEntity be && player.getItemInHand(hand).isEmpty()) {
            player.displayClientMessage(Component.translatable("message.primitivewater.volume", be.getWater(), RainCollectorBlockEntity.CAPACITY), true);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
