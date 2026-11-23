package com.kirill.simplesorter.sorting;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Server-only transient "wand inventory" for Smart Categories.
 *
 * No items are ever written to the wand's NBT or made accessible via a GUI.
 * A whole group is planned without changing any inventory, then extracted
 * into an in-memory buffer and refilled in the SAME server tick. A failed
 * extraction/insertion triggers rollback; unrecoverable remainders are dropped
 * visibly at their original chest instead of being silently voided.
 *
 * The physical capacity is unchanged: this does NOT create long-term
 * storage or allow the player to carry extra items.
 */
public final class WarehouseVacuumSorter {
    private static final Logger LOG = LoggerFactory.getLogger("SimpleSorter/WandBuffer");
    // One batch is a single synchronous transaction. Bound the worst-case
    // tick cost while still allowing a fully occupied 12-chest warehouse.
    private static final int MAX_CHESTS_PER_BATCH = 16;
    private static final int MAX_SLOTS_PER_CHEST = 108;

    private WarehouseVacuumSorter() {}

    public static Session begin(List<ContainerRef> containers) {
        return new Session(containers);
    }

    private static final class Chest {
        final ContainerRef ref;
        final int sequence;
        final ItemStack[] snapshot;
        final Entry[] layout;
        final Map<String, Integer> votes = new HashMap<>();
        final Map<String, Integer> allocatedFamilies = new HashMap<>();
        int placed;

        Chest(ContainerRef ref, int sequence) {
            this.ref = ref;
            this.sequence = sequence;
            this.snapshot = new ItemStack[ref.slots()];
            this.layout = new Entry[ref.slots()];
        }

        int freeSlots() { return layout.length - placed; }
        void reserve(int slot, Entry entry) {
            layout[slot] = entry;
            allocatedFamilies.merge(entry.family, 1, Integer::sum);
            placed++;
            entry.destination = this;
            entry.destinationSlot = slot;
        }
    }

    private static final class Entry {
        final Chest source;
        final int sourceSlot;
        final ItemStack original;
        final String family;
        Chest destination;
        int destinationSlot = -1;
        ItemStack carried = ItemStack.EMPTY;

        Entry(Chest source, int sourceSlot, ItemStack original, String family) {
            this.source = source;
            this.sourceSlot = sourceSlot;
            this.original = original;
            this.family = family;
        }
    }

    private record Placement(Entry entry, Chest chest, int slot, int count) {}

    public static final class Session {
        private final List<ContainerRef> containers;
        private final List<List<ContainerRef>> batches;
        private int batchIndex;
        private int processed;
        private int movedItems;
        private int moves;
        private int skippedChests;
        private int rejectedBatches;

        private Session(List<ContainerRef> containers) {
            this.containers = List.copyOf(containers);
            List<WarehouseFamilyPlan.Node> nodes = new ArrayList<>();
            for (ContainerRef ref : containers) {
                BlockPos p = ref.pos();
                nodes.add(new WarehouseFamilyPlan.Node(p.getX(), p.getY(), p.getZ(), Map.of()));
            }
            WarehouseFamilyPlan geometry = WarehouseFamilyPlan.analyze(nodes);
            Map<Integer, List<ContainerRef>> regions = new LinkedHashMap<>();
            for (int i = 0; i < containers.size(); i++) {
                regions.computeIfAbsent(geometry.region(i), key -> new ArrayList<>())
                        .add(containers.get(i));
            }
            batches = new ArrayList<>();
            for (List<ContainerRef> region : regions.values()) {
                for (int i = 0; i < region.size(); i += MAX_CHESTS_PER_BATCH) {
                    batches.add(List.copyOf(region.subList(
                            i, Math.min(i + MAX_CHESTS_PER_BATCH, region.size()))));
                }
            }
        }

        public int movedItems() { return movedItems; }
        public int movedOperations() { return moves; }
        public int attemptedTransfers() { return moves + rejectedBatches; }
        public int rejectedTransfers() { return rejectedBatches; }
        public int skippedChests() { return skippedChests; }
        public int currentChestIndex() { return processed; }
        public int assignmentMissingFamilies() { return 0; }

        // Diagnostics are derived from actual post-sort inventories, not a
        // frozen pre-sort snapshot. Unknown "misc" is a valid fallback family.
        public void refreshContainer(int ignored) {}

        public int unresolvedMixedChests() {
            int result = 0;
            for (ContainerRef ref : containers) {
                try {
                    Map<String, Integer> families = new HashMap<>();
                    for (int s = 0; s < ref.slots(); s++) {
                        ItemStack stack = ref.handler().getStackInSlot(s);
                        if (!stack.isEmpty()) {
                            String family = StrictStackClassifier.family(stack);
                            families.merge(family, 1, Integer::sum);
                        }
                    }
                    if (families.size() > 1) result++;
                } catch (RuntimeException ex) {
                    LOG.warn("Cannot inspect chest {} after sorting", ref.pos(), ex);
                }
            }
            return result;
        }

        public int unknownFamilySlots() {
            int count = 0;
            for (ContainerRef ref : containers) {
                try {
                    for (int s = 0; s < ref.slots(); s++) {
                        ItemStack stack = ref.handler().getStackInSlot(s);
                        if (!stack.isEmpty() && "misc".equals(StrictStackClassifier.family(stack))) {
                            count++;
                        }
                    }
                } catch (RuntimeException ex) {
                    LOG.warn("Cannot inspect unknown items at {}", ref.pos(), ex);
                }
            }
            return count;
        }

        public String unknownIdExamples(int limit) {
            Map<String, Integer> counts = new HashMap<>();
            for (ContainerRef ref : containers) {
                try {
                    for (int s = 0; s < ref.slots(); s++) {
                        ItemStack stack = ref.handler().getStackInSlot(s);
                        if (!stack.isEmpty() && "misc".equals(StrictStackClassifier.family(stack))) {
                            counts.merge(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(),
                                    1, Integer::sum);
                        }
                    }
                } catch (RuntimeException ex) {
                    LOG.warn("Cannot inspect item IDs at {}", ref.pos(), ex);
                }
            }
            return counts.entrySet().stream()
                    .sorted(Comparator.<Map.Entry<String, Integer>>comparingInt(
                            Map.Entry::getValue).reversed().thenComparing(Map.Entry::getKey))
                    .limit(Math.max(0, limit))
                    .map(e -> e.getKey() + " (" + e.getValue() + ")")
                    .collect(java.util.stream.Collectors.joining(", "));
        }

        /**
         * At most one bounded warehouse batch per server tick. The entire
         * extraction/refill/rollback is synchronous inside that one tick:
         * there is never a persistent item buffer across saves or logouts.
         */
        public boolean step(ServerLevel level, MoveListener listener,
                            int ignoredSlotLimit, long ignoredTimeLimit) {
            if (batchIndex >= batches.size()) return true;
            List<ContainerRef> batch = batches.get(batchIndex++);
            processed += batch.size();
            try {
                reorganize(level, batch, listener);
            } catch (RuntimeException ex) {
                // Preflight exceptions do not mutate inventories. Commit
                // exceptions are handled and rolled back inside commit().
                rejectedBatches++;
                LOG.error("Wand buffer skipped broken inventory batch", ex);
            }
            return batchIndex >= batches.size();
        }

        private void reorganize(ServerLevel level, List<ContainerRef> refs,
                                MoveListener listener) {
            List<Chest> chests = new ArrayList<>();
            List<Entry> entries = new ArrayList<>();
            for (ContainerRef ref : refs) {
                Chest chest = snapshotEligible(level, ref, chests.size(), entries);
                if (chest == null) {
                    skippedChests++;
                } else {
                    chests.add(chest);
                }
            }
            if (chests.size() < 2 || entries.isEmpty()) return;

            // Every existing occupied slot becomes one independent buffer
            // stack. This preserves TFC food NBT, unique item components,
            // durability and stack size without inventing extra capacity.
            Map<String, List<Entry>> byFamily = new HashMap<>();
            for (Entry entry : entries) {
                byFamily.computeIfAbsent(entry.family, ignored -> new ArrayList<>())
                        .add(entry);
            }
            List<String> families = new ArrayList<>(byFamily.keySet());
            families.sort(Comparator
                    .comparingInt((String family) -> "misc".equals(family) ? 1 : 0)
                    .thenComparing(Comparator.comparingInt(
                            (String family) -> byFamily.get(family).size()).reversed())
                    .thenComparing(String::compareTo));

            for (String family : families) {
                List<Chest> candidates = new ArrayList<>(chests);
                // Preserve existing affinities, prefer untouched chests,
                // and only mix families when there are no better vacancies.
                candidates.sort(Comparator
                        .comparingInt((Chest c) -> c.allocatedFamilies.containsKey(family)
                                ? 0 : c.placed == 0 ? 1 : 2)
                        .thenComparingInt(c -> -c.votes.getOrDefault(family, 0))
                        .thenComparingInt(c -> -c.freeSlots())
                        .thenComparingInt(c -> c.sequence));
                for (Entry entry : byFamily.get(family)) {
                    boolean allocated = false;
                    for (Chest chest : candidates) {
                        int slot = firstCompatibleSlot(chest, entry);
                        if (slot < 0) continue;
                        chest.reserve(slot, entry);
                        allocated = true;
                        break;
                    }
                    if (!allocated) {
                        // This batch contains a modded slot restriction or
                        // insufficient compatible capacity. Nothing was
                        // extracted; leave every inventory unchanged.
                        rejectedBatches++;
                        return;
                    }
                }
            }

            int crossChestItems = 0;
            for (Entry entry : entries) {
                if (entry.source != entry.destination) crossChestItems += entry.original.getCount();
            }
            if (crossChestItems == 0) return;

            if (!unchanged(level, chests)) {
                rejectedBatches++;
                return;
            }
            if (commit(level, entries, listener)) {
                movedItems += crossChestItems;
                moves += entries.stream().filter(e -> e.source != e.destination).count();
            } else {
                rejectedBatches++;
            }
        }

        private Chest snapshotEligible(ServerLevel level, ContainerRef ref, int sequence,
                                       List<Entry> entries) {
            if (!valid(level, ref) || ref.slots() <= 0 || ref.slots() > MAX_SLOTS_PER_CHEST) {
                return null;
            }
            Chest chest = new Chest(ref, sequence);
            List<Entry> local = new ArrayList<>();
            try {
                for (int slot = 0; slot < ref.slots(); slot++) {
                    ItemStack live = ref.handler().getStackInSlot(slot);
                    ItemStack stack = live.isEmpty() ? ItemStack.EMPTY : live.copy();
                    chest.snapshot[slot] = stack;
                    if (stack.isEmpty()) continue;
                    if (!ItemCategoryResolver.resolve(stack).moveAllowed()
                            || !canInsertAfterExtraction(ref.handler(), slot, stack)) return null;
                    ItemStack simulated = ref.handler().extractItem(slot, stack.getCount(), true);
                    if (!sameStackAndCount(stack, simulated)) return null;
                    String family = StrictStackClassifier.family(stack);
                    chest.votes.merge(family, 1, Integer::sum);
                    local.add(new Entry(chest, slot, stack, family));
                }
            } catch (RuntimeException ex) {
                LOG.warn("Skipping incompatible inventory at {}", ref.pos(), ex);
                return null;
            }
            entries.addAll(local);
            return chest;
        }

        private static int firstCompatibleSlot(Chest chest, Entry entry) {
            if (chest.freeSlots() == 0) return -1;
            if (chest == entry.source
                    && chest.layout[entry.sourceSlot] == null
                    && canInsertAfterExtraction(chest.ref.handler(), entry.sourceSlot,
                            entry.original)) return entry.sourceSlot;
            for (int slot = 0; slot < chest.layout.length; slot++) {
                if (chest.layout[slot] == null
                        && canInsertAfterExtraction(chest.ref.handler(), slot, entry.original)) {
                    return slot;
                }
            }
            return -1;
        }

        private static boolean unchanged(ServerLevel level, List<Chest> chests) {
            for (Chest chest : chests) {
                if (!valid(level, chest.ref)) return false;
                for (int slot = 0; slot < chest.snapshot.length; slot++) {
                    if (!sameStackAndCount(chest.snapshot[slot],
                            chest.ref.handler().getStackInSlot(slot))) return false;
                }
            }
            return true;
        }

        private static boolean valid(ServerLevel level, ContainerRef ref) {
            return level.hasChunkAt(ref.pos())
                    && level.getBlockEntity(ref.pos()) == ref.identity();
        }

        /**
         * Slot validity is checked independently of how FULL the slot
         * currently is. A simulated normal insert into a full warehouse
         * would falsely reject all destinations.
         */
        private static boolean canInsertAfterExtraction(IItemHandler handler,
                                                       int slot, ItemStack stack) {
            if (handler instanceof ContainerItemHandler container) {
                return container.container().canPlaceItem(slot, stack)
                        && stack.getCount() <= Math.min(
                        container.container().getMaxStackSize(), stack.getMaxStackSize());
            }
            if (handler instanceof ForgeItemHandlerAdapter forge) {
                return forge.delegate().isItemValid(slot, stack)
                        && stack.getCount() <= Math.min(
                        forge.delegate().getSlotLimit(slot), stack.getMaxStackSize());
            }
            if (handler instanceof DoubleChestItemHandler both) {
                return slot < both.first().getSlots()
                        ? canInsertAfterExtraction(both.first(), slot, stack)
                        : canInsertAfterExtraction(both.second(),
                                slot - both.first().getSlots(), stack);
            }
            return false; // unknown handler = no destructive operation
        }

        private static boolean sameStackAndCount(ItemStack a, ItemStack b) {
            return a.isEmpty() ? b.isEmpty()
                    : !b.isEmpty() && a.getCount() == b.getCount()
                    && ItemStack.isSameItemSameTags(a, b);
        }

        /**
         * Extract everything, refill, and commit atomically on the game
         * thread. No client packets can withdraw from this virtual buffer.
         */
        private static boolean commit(ServerLevel level, List<Entry> entries,
                                      MoveListener listener) {
            List<Placement> placed = new ArrayList<>();
            boolean success = false;
            try {
                for (Entry entry : entries) {
                    ItemStack removed = entry.source.ref.handler().extractItem(
                            entry.sourceSlot, entry.original.getCount(), false);
                    if (!removed.isEmpty()) entry.carried = removed.copy();
                    if (!sameStackAndCount(removed, entry.original)) {
                        throw new IllegalStateException("Inventory extraction differs from simulation at "
                                + entry.source.ref.pos());
                    }
                }

                for (Entry entry : entries) {
                    ItemStack before = entry.carried.copy();
                    ItemStack remainder = entry.destination.ref.handler().insertItem(
                            entry.destinationSlot, before, false);
                    int remaining = remainder.isEmpty() ? 0 : remainder.getCount();
                    if (remaining < 0 || remaining > before.getCount()
                            || (!remainder.isEmpty()
                            && !ItemStack.isSameItemSameTags(before, remainder))) {
                        throw new IllegalStateException("Invalid inventory insertion remainder at "
                                + entry.destination.ref.pos());
                    }
                    int accepted = before.getCount() - remaining;
                    if (accepted > 0) {
                        placed.add(new Placement(entry, entry.destination,
                                entry.destinationSlot, accepted));
                    }
                    entry.carried = remainder.isEmpty() ? ItemStack.EMPTY : remainder.copy();
                    if (!entry.carried.isEmpty()) {
                        throw new IllegalStateException("Inventory rejected buffer item at "
                                + entry.destination.ref.pos());
                    }
                }
                success = true;
            } catch (RuntimeException ex) {
                LOG.error("Wand inventory transaction failed; rolling back all extracted stacks", ex);
            } finally {
                if (!success) rollback(level, entries, placed);
            }
            if (!success) return false;

            // Visual effects are emitted only AFTER all items have been
            // returned from the buffer successfully.
            for (Entry entry : entries) {
                if (entry.source != entry.destination) {
                    listener.onMove(new MoveRecord(entry.source.ref.pos(),
                            entry.destination.ref.pos(), entry.original.getCount(),
                            entry.original.copy()));
                }
            }
            return true;
        }

        private static void rollback(ServerLevel level, List<Entry> entries,
                                     List<Placement> placed) {
            // Reclaim every confirmed destination insertion first. The
            // original slots are now available for exact restoration.
            for (int i = placed.size() - 1; i >= 0; i--) {
                Placement move = placed.get(i);
                try {
                    ItemStack recovered = move.chest.ref.handler().extractItem(
                            move.slot, move.count, false);
                    if (!recovered.isEmpty()
                            && ItemStack.isSameItemSameTags(recovered, move.entry.original)) {
                        if (move.entry.carried.isEmpty()) {
                            move.entry.carried = recovered.copy();
                        } else {
                            move.entry.carried.grow(recovered.getCount());
                        }
                    } else if (!recovered.isEmpty()) {
                        emergencyDrop(level, move.chest.ref.pos(), recovered);
                    }
                } catch (RuntimeException ex) {
                    // An uncooperative modded inventory may retain an item
                    // at its destination. Never manufacture a duplicate.
                    LOG.error("Unable to reclaim placed stack at {} slot {}",
                            move.chest.ref.pos(), move.slot, ex);
                }
            }
            for (Entry entry : entries) {
                if (entry.carried.isEmpty()) continue;
                ItemStack remaining = entry.carried.copy();
                try {
                    remaining = entry.source.ref.handler().insertItem(
                            entry.sourceSlot, remaining, false);
                    for (int s = 0; !remaining.isEmpty()
                            && s < entry.source.ref.slots(); s++) {
                        if (s != entry.sourceSlot) {
                            remaining = entry.source.ref.handler().insertItem(s, remaining, false);
                        }
                    }
                } catch (RuntimeException ex) {
                    LOG.error("Original slot rejected rollback at {} slot {}",
                            entry.source.ref.pos(), entry.sourceSlot, ex);
                }
                if (!remaining.isEmpty()) {
                    emergencyDrop(level, entry.source.ref.pos(), remaining);
                }
                entry.carried = ItemStack.EMPTY;
            }
        }

        private static void emergencyDrop(ServerLevel level, BlockPos origin, ItemStack stack) {
            if (stack.isEmpty()) return;
            LOG.error("EMERGENCY RECOVERY: dropping {} x {} at {} after inventory rollback failure",
                    stack.getCount(), BuiltInRegistries.ITEM.getKey(stack.getItem()), origin);
            ItemEntity entity = new ItemEntity(level,
                    origin.getX() + 0.5, origin.getY() + 1.0,
                    origin.getZ() + 0.5, stack.copy());
            entity.setDefaultPickUpDelay();
            level.addFreshEntity(entity);
        }
    }
}
