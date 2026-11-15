package dev.minemods.openmotion.block;

import com.mojang.serialization.MapCodec;
import dev.minemods.openmotion.config.OpenMotionConfig;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;

/** Launch an item by hand, or extract an item from the container directly behind on a redstone rising edge. */
public final class ItemCannonBlock extends HorizontalMachineBlock {
    public static final MapCodec<ItemCannonBlock> CODEC = simpleCodec(ItemCannonBlock::new);
    private static final VoxelShape SHAPE = Block.box(2, 0, 0, 14, 15.5, 16);
    private static final int SIGNAL_SCAN_TICKS = 5;

    @Override
    protected MapCodec<ItemCannonBlock> codec() {
        return CODEC;
    }

    public ItemCannonBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level,
                                  BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (level.isClientSide || oldState.is(state.getBlock())) return;
        boolean powered = level.hasNeighborSignal(pos);
        if (state.getValue(POWERED) != powered) {
            level.setBlock(pos, state.setValue(POWERED, powered), Block.UPDATE_ALL);
        }
        // Placement next to an already-on torch counts as the initial positive edge.
        if (powered && level instanceof ServerLevel server) {
            fireOrClick(server, pos, state.getValue(FACING));
        }
        level.scheduleTick(pos, this, SIGNAL_SCAN_TICKS);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block fromBlock, BlockPos fromPos, boolean isMoving) {
        if (!level.isClientSide) syncSignal(level, pos, state);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // Fallback reconciliation for chunk/modded redstone changes.
        syncSignal(level, pos, state);
        level.scheduleTick(pos, this, SIGNAL_SCAN_TICKS);
    }

    private static void syncSignal(Level level, BlockPos pos, BlockState state) {
        boolean powered = level.hasNeighborSignal(pos);
        if (powered == state.getValue(POWERED)) return;
        level.setBlock(pos, state.setValue(POWERED, powered), Block.UPDATE_ALL);
        if (powered && level instanceof ServerLevel server) {
            fireOrClick(server, pos, state.getValue(FACING));
        }
    }

    private static void fireOrClick(ServerLevel level, BlockPos pos, Direction facing) {
        if (!fireFromAttachedInventory(level, pos, facing)) {
            level.playSound(null, pos, SoundEvents.DISPENSER_FAIL, SoundSource.BLOCKS, 0.6F, 1.2F);
        }
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level,
                                               BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.isEmpty()) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!level.isClientSide && level instanceof ServerLevel server) {
            ItemStack fired = stack.copyWithCount(1);
            launch(server, pos, state.getValue(FACING), fired);
            if (!player.getAbilities().instabuild) stack.shrink(1);
        }
        return ItemInteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level instanceof ServerLevel server) {
            if (!fireFromAttachedInventory(server, pos, state.getValue(FACING))) {
                level.playSound(null, pos, SoundEvents.DISPENSER_FAIL, SoundSource.BLOCKS, 0.65F, 1.0F);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static boolean fireFromAttachedInventory(ServerLevel level, BlockPos cannonPos, Direction facing) {
        // Rear input first, top input second.
        return fireFromInventoryAt(level, cannonPos, facing, cannonPos.relative(facing.getOpposite()), facing)
                || fireFromInventoryAt(level, cannonPos, facing, cannonPos.above(), Direction.DOWN);
    }

    private static boolean fireFromInventoryAt(ServerLevel level, BlockPos cannonPos, Direction facing,
                                               BlockPos inventoryPos, Direction extractionSide) {
        // Respect the actual machine-facing side for modded inventories.
        IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, inventoryPos, extractionSide);
        if (handler != null) {
            for (int slot = 0; slot < handler.getSlots(); slot++) {
                ItemStack simulated = handler.extractItem(slot, 1, true);
                if (simulated.isEmpty()) continue;
                ItemStack extracted = handler.extractItem(slot, 1, false);
                if (extracted.isEmpty()) continue;
                launch(level, cannonPos, facing, extracted);
                return true;
            }
            return false;
        }

        // Vanilla dispenser and other Containers that do not expose item capabilities.
        if (!(level.getBlockEntity(inventoryPos) instanceof Container container)) return false;
        int[] slots;
        if (container instanceof WorldlyContainer sided) {
            slots = sided.getSlotsForFace(extractionSide);
        } else {
            slots = new int[container.getContainerSize()];
            for (int i = 0; i < slots.length; i++) slots[i] = i;
        }
        for (int slot : slots) {
            if (slot < 0 || slot >= container.getContainerSize()) continue;
            ItemStack stack = container.getItem(slot);
            if (stack.isEmpty()) continue;
            if (container instanceof WorldlyContainer sided
                    && !sided.canTakeItemThroughFace(slot, stack, extractionSide)) continue;
            ItemStack extracted = container.removeItem(slot, 1);
            if (extracted.isEmpty()) continue;
            container.setChanged();
            launch(level, cannonPos, facing, extracted);
            return true;
        }
        return false;
    }

    private static void launch(ServerLevel level, BlockPos pos, Direction facing, ItemStack stack) {
        Vec3 direction = Vec3.atLowerCornerOf(facing.getNormal());
        Vec3 muzzle = Vec3.atCenterOf(pos).add(direction.scale(0.82));
        ItemEntity item = new ItemEntity(level, muzzle.x, muzzle.y - 0.18, muzzle.z, stack);
        // Manual use is a short shot; stronger input signal increases throw distance.
        int power = level.getBestNeighborSignal(pos);
        double launchSpeed = OpenMotionConfig.CANNON_SPEED.get() + Math.max(0, power - 1) * 0.045;
        item.setDeltaMovement(direction.scale(launchSpeed).add(0, 0.12, 0));
        item.setDefaultPickUpDelay();
        level.addFreshEntity(item);
        level.playSound(null, pos, SoundEvents.DISPENSER_DISPENSE, SoundSource.BLOCKS, 0.8F, 0.85F);
        level.levelEvent(2000, pos, facing.get3DDataValue());
    }
}
