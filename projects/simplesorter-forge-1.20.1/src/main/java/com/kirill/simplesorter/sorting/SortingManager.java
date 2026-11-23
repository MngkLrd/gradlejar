package com.kirill.simplesorter.sorting;

import com.kirill.simplesorter.config.SorterSettings;
import com.kirill.simplesorter.registry.ModItems;
import com.kirill.simplesorter.settings.SortingWandSettings;
import com.kirill.simplesorter.visual.SortingVisuals;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Dispatches the four modes. Routing rules live in SmartCategorySorter. */
public final class SortingManager {
    private static final int MAIN_INVENTORY_SLOTS = 36;
    private SortingManager() {}

    public static SortingResult sortAroundPlayer(ServerLevel level, Player player, SortingWandSettings settings) {
        SortingWandSettings safe = settings.sanitized();
        List<ContainerRef> containers = ContainerScanner.scanRadius(level, player,
                player.blockPosition(), safe.radius());
        if (containers.isEmpty()) {
            return new SortingResult(0, 0, 0, 0, 0, safe.radius(), safe.mode().id());
        }

        return switch (safe.mode()) {
            case SMART_CATEGORIES, MATCH_EXISTING ->
                    SmartCategorySorter.sort(level, containers, safe.radius(), safe);
            case COMPACT_STACKS -> compactStacks(level, containers, safe);
            case PLAYER_DEPOSIT -> depositPlayerInventory(level, player, containers, safe);
        };
    }

    private static SortingResult compactStacks(ServerLevel level,
                                                List<ContainerRef> containers, SortingWandSettings settings) {
        int items = 0;
        int stacks = 0;
        int misses = 0;
        EffectRecorder effects = new EffectRecorder();

        for (ContainerRef source : containers) {
            IItemHandler handler = source.handler();
            for (int slot = 0; slot < handler.getSlots(); slot++) {
                ItemStack stack = handler.getStackInSlot(slot);
                if (!canMove(stack)) continue;

                List<ContainerRef> targets = matchingContainers(containers, stack, source, true);
                if (targets.isEmpty()) {
                    misses++;
                    continue;
                }
                int moved = InventoryMover.mergeSlotIntoExistingStacks(source, slot, targets, effects::add);
                if (moved > 0) {
                    items += moved;
                    stacks++;
                }
            }
        }

        for (ContainerRef ref : containers) {
            int packed = InventoryMover.cleanUpContainer(ref, effects::add);
            if (packed > 0) {
                items += packed;
                stacks++;
            }
        }
        effects.play(level, settings, items);
        return new SortingResult(containers.size(), items, stacks, effects.changed.size(),
                misses, settings.radius(), settings.mode().id());
    }

    private static SortingResult depositPlayerInventory(ServerLevel level, Player player,
                                                         List<ContainerRef> containers, SortingWandSettings settings) {
        int items = 0;
        int stacks = 0;
        int misses = 0;
        EffectRecorder effects = new EffectRecorder();

        Inventory inv = player.getInventory();
        int bound = Math.min(MAIN_INVENTORY_SLOTS, inv.getContainerSize());
        for (int slot = 0; slot < bound; slot++) {
            ItemStack stack = inv.getItem(slot);
            if (!canMove(stack) || stack.getItem() == ModItems.SORTING_WAND.get()) continue;

            List<ContainerRef> targets = matchingContainers(containers, stack, null, false);
            if (targets.isEmpty()) {
                misses++;
                continue;
            }
            int moved = InventoryMover.movePlayerSlotToTargets(player, slot, targets, effects::add);
            if (moved > 0) {
                items += moved;
                stacks++;
            }
        }

        effects.play(level, settings, items);
        return new SortingResult(containers.size(), items, stacks, effects.changed.size(),
                misses, settings.radius(), settings.mode().id());
    }

    private static List<ContainerRef> matchingContainers(List<ContainerRef> all, ItemStack stack,
                                                         ContainerRef source, boolean partialOnly) {
        List<ContainerRef> result = new ArrayList<>();
        for (ContainerRef ref : all) {
            if (source != null && (ref.pos().equals(source.pos()) ||
                    ref.handler() == source.handler() || ref.identity() == source.identity())) continue;
            if (countMatches(ref.handler(), stack, partialOnly) > 0) result.add(ref);
        }
        result.sort(Comparator
                .comparingInt((ContainerRef ref) -> countMatches(ref.handler(), stack, partialOnly))
                .reversed()
                .thenComparingLong(ref -> ref.pos().asLong()));
        return result;
    }

    private static int countMatches(IItemHandler handler, ItemStack stack, boolean partialOnly) {
        int count = 0;
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack other = handler.getStackInSlot(slot);
            if (other.isEmpty()) continue;
            if (partialOnly) {
                if (ItemStack.isSameItemSameTags(other, stack) && other.getCount() < other.getMaxStackSize())
                    count += other.getMaxStackSize() - other.getCount();
            } else if (other.getItem() == stack.getItem()) {
                // Same registered type is a valid destination; merger still
                // verifies exact NBT on occupied stacks.
                count += other.getCount();
            }
        }
        return count;
    }

    private static boolean canMove(ItemStack stack) {
        return !stack.isEmpty() && ItemCategoryResolver.resolve(stack).moveAllowed();
    }

    private static final class EffectRecorder {
        private final List<MoveRecord> moves = new ArrayList<>();
        private final Set<BlockPos> changed = new HashSet<>();

        void add(MoveRecord move) {
            changed.add(move.sourcePos());
            changed.add(move.targetPos());
            if (moves.size() < SorterSettings.MAX_MOVE_PARTICLE_TRACES) moves.add(move);
        }

        void play(ServerLevel level, SortingWandSettings settings, int moved) {
            if (moved > 0) SortingVisuals.playSortingEffects(level, changed, moves, settings);
        }
    }
}
