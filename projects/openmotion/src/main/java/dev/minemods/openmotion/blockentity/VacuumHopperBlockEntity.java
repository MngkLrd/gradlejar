package dev.minemods.openmotion.blockentity;

import dev.minemods.openmotion.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import java.util.List;

/** Server-owned nine-slot vacuum hopper. No client rendering references. */
public final class VacuumHopperBlockEntity extends BlockEntity {
    private static final int MAX_TARGETS = 18;
    private static final AABB LOCAL_BOUNDS = new AABB(-3.5, -2.2, -3.5, 4.5, 3.1, 4.5);
    private final ItemStackHandler items = new ItemStackHandler(9) {
        @Override protected void onContentsChanged(int slot) {
            VacuumHopperBlockEntity.this.setChanged();
        }
    };

    public VacuumHopperBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.VACUUM_HOPPER.get(), pos, state);
    }

    public ItemStackHandler inventory() { return items; }

    public ItemStack takeFirstStack() {
        for (int slot = 0; slot < items.getSlots(); slot++) {
            if (!items.getStackInSlot(slot).isEmpty()) {
                return items.extractItem(slot, items.getStackInSlot(slot).getCount(), false);
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Inventory", items.serializeNBT(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Inventory", Tag.TAG_COMPOUND)) {
            items.deserializeNBT(registries, tag.getCompound("Inventory"));
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, VacuumHopperBlockEntity blockEntity) {
        if (!(level instanceof ServerLevel server)) return;
        // Redstone blocks the collector, allowing players to pause farms.
        if (level.hasNeighborSignal(pos)) return;
        long tick = level.getGameTime();
        if (tick % 4 == 0) blockEntity.collect(server, pos);
        if (tick % 8 == 0) blockEntity.output(server, pos);
    }

    private void collect(ServerLevel level, BlockPos pos) {
        Vec3 target = Vec3.atCenterOf(pos).add(0, .45, 0);
        List<ItemEntity> entities = level.getEntitiesOfClass(ItemEntity.class, LOCAL_BOUNDS.move(pos),
                item -> item.isAlive() && !item.getItem().isEmpty());
        int processed = 0;
        for (ItemEntity item : entities) {
            if (processed++ >= MAX_TARGETS) break;
            Vec3 offset = target.subtract(item.position());
            double distanceSq = offset.lengthSqr();
            if (distanceSq > 25 || distanceSq < 0.00001) continue;
            if (level.clip(new ClipContext(item.position(), target, ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE, item)).getType() != HitResult.Type.MISS) continue;
            if (distanceSq <= 0.85) {
                ItemStack remainder = tryInsert(item.getItem());
                if (remainder.getCount() == item.getItem().getCount()) continue;
                if (remainder.isEmpty()) item.discard();
                else item.setItem(remainder);
            } else if (canAccept(item.getItem())) {
                Vec3 motion = item.getDeltaMovement().scale(.62).add(offset.normalize().scale(.13));
                item.setDeltaMovement(motion);
                item.hasImpulse = true;
                item.hurtMarked = true;
            }
        }
    }

    private boolean canAccept(ItemStack input) {
        ItemStack remainder = input.copy();
        for (int slot = 0; slot < items.getSlots() && !remainder.isEmpty(); slot++)
            remainder = items.insertItem(slot, remainder, true);
        return remainder.getCount() < input.getCount();
    }

    private ItemStack tryInsert(ItemStack stack) {
        ItemStack remainder = stack.copy();
        for (int slot = 0; slot < items.getSlots() && !remainder.isEmpty(); slot++)
            remainder = items.insertItem(slot, remainder, false);
        return remainder;
    }

    private void output(ServerLevel level, BlockPos pos) {
        // Native NeoForge capability permits chests and compatible modded storage.
        IItemHandler below = level.getCapability(Capabilities.ItemHandler.BLOCK, pos.below(), Direction.UP);
        if (below == null) return;
        // Transfer at most one full stack each operation to keep the cost bounded.
        for (int from = 0; from < items.getSlots(); from++) {
            ItemStack source = items.getStackInSlot(from);
            if (source.isEmpty()) continue;
            for (int to = 0; to < below.getSlots(); to++) {
                ItemStack preview = below.insertItem(to, source.copy(), true);
                int couldMove = source.getCount() - preview.getCount();
                if (couldMove <= 0) continue;
                ItemStack moving = source.copyWithCount(couldMove);
                ItemStack leftover = below.insertItem(to, moving, false);
                int moved = couldMove - leftover.getCount();
                if (moved > 0) {
                    items.extractItem(from, moved, false);
                    return;
                }
            }
        }
    }
}
