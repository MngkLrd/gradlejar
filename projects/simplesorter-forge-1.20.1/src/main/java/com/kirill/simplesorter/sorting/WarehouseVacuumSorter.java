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
    // Planning is global; execution is partitioned into independent transactions.
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
        final SemanticItemProfile semantic;
        Chest destination;
        int destinationSlot = -1;
        ItemStack carried = ItemStack.EMPTY;

        Entry(Chest source, int sourceSlot, ItemStack original, SemanticItemProfile semantic) {
            this.source = source;
            this.sourceSlot = sourceSlot;
            this.original = original;
            this.semantic = semantic;
            this.family = semantic.legacyFamily();
        }
    }

    private record Placement(Entry entry, Chest chest, int slot, int count) {}

    public static final class Session {
        private final List<ContainerRef> containers;
        private List<List<Entry>> components=List.of();
        private boolean planned;
        private int componentIndex,totalComponents,processedEntries,totalEntries;
        private int activeInventories,semanticGroups;
        private long beforeCost,afterCost;
        private int processed;
        private int movedItems;
        private int moves;
        private int skippedChests;
        private int rejectedBatches;
        private String lastFailure = "none";

        private Session(List<ContainerRef> containers) {
            this.containers = List.copyOf(containers);

        }

        public int movedItems() { return movedItems; }
        public int movedOperations() { return moves; }
        public int attemptedTransfers() { return moves + rejectedBatches; }
        public int rejectedTransfers() { return rejectedBatches; }
        public int skippedChests() { return skippedChests; }
        public String lastFailure() { return lastFailure; }
        public int currentChestIndex() { return processed; }
        public int batchCount() { return containers.isEmpty()?0:1; }
        public int totalComponents(){return totalComponents;}
        public int completedComponents(){return componentIndex;}
        public int activeInventories(){return activeInventories;}
        public int semanticGroups(){return semanticGroups;}
        public long beforeCost(){return beforeCost;}
        public long afterCost(){return afterCost;}
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
         * One global plan per activation. Execute independent components
         * synchronously; never retain extracted items across ticks.
         */
        public boolean step(ServerLevel level, MoveListener listener,int slotLimit,long timeLimit) {
            if(!planned){
                planned=true;
                try { prepare(level); }
                catch(RuntimeException ex){
                    rejectedBatches++;lastFailure="global planning failed: "+ex.getClass().getSimpleName();
                    LOG.error("Global warehouse planning failed",ex);
                    return true;
                }
                if(components.isEmpty())processed=containers.size();
                return components.isEmpty();
            }
            long start=System.nanoTime();
            int budget=Math.max(1,slotLimit),used=0;
            while(componentIndex<components.size()){
                List<Entry> group=components.get(componentIndex);
                if(used>0 && (used+group.size()>budget || System.nanoTime()-start>=timeLimit))break;
                componentIndex++;used+=group.size();
                boolean ok=false;
                try{ok=commitComponent(level,group,listener);}
                catch(RuntimeException ex){LOG.error("Warehouse component failed",ex);}
                if(ok){
                    for(Entry e:group)if(e.source!=e.destination){movedItems+=e.original.getCount();moves++;}
                }else{
                    rejectedBatches++;
                    lastFailure="component changed or inventory refused transfer (see latest.log)";
                }
                processedEntries+=group.size();
                processed=totalEntries==0?containers.size():(int)((long)processedEntries*containers.size()/totalEntries);
            }
            return componentIndex>=components.size();
        }

        private void prepare(ServerLevel level){
            List<Chest> chests=new ArrayList<>();
            List<Entry> entries=new ArrayList<>();
            for(ContainerRef ref:containers){
                Chest chest=snapshotEligible(level,ref,chests.size(),entries);
                if(chest==null)skippedChests++;else chests.add(chest);
            }
            if(chests.size()<2||entries.isEmpty())return;
            List<AdaptiveWarehousePlanner.Storage> boxes=new ArrayList<>();
            for(Chest chest:chests){
                BlockPos p=chest.ref.pos(),q=chest.ref.partnerPos();
                boxes.add(new AdaptiveWarehousePlanner.Storage(chest.layout.length,
                        p.getX()+(q==null?p.getX():q.getX()),p.getY(),
                        p.getZ()+(q==null?p.getZ():q.getZ())));
            }
            List<AdaptiveWarehousePlanner.Stack> items=new ArrayList<>();
            for(Entry e:entries)items.add(new AdaptiveWarehousePlanner.Stack(e.source.sequence,e.sourceSlot,e.semantic));
            AdaptiveWarehousePlanner.Plan plan=AdaptiveWarehousePlanner.plan(items,boxes,
                    (item,chest,slot)->canInsertAfterExtraction(chests.get(chest).ref.handler(),
                            slot,entries.get(item).original),false);
            int[] owners=plan.chestByStack(),slots=plan.slotByStack();
            List<WarehouseTransferComponents.Slot> sources=new ArrayList<>(),targets=new ArrayList<>();
            for(int i=0;i<entries.size();i++){
                Entry e=entries.get(i);
                chests.get(owners[i]).reserve(slots[i],e);
                sources.add(new WarehouseTransferComponents.Slot(e.source.sequence,e.sourceSlot));
                targets.add(new WarehouseTransferComponents.Slot(owners[i],slots[i]));
            }
            beforeCost=plan.beforeCost();afterCost=plan.afterCost();
            activeInventories=plan.activeInventories();semanticGroups=plan.groups();
            if(!unchanged(level,chests)){rejectedBatches++;lastFailure="inventory changed during planning";return;}
            List<List<Entry>> jobs=new ArrayList<>();
            for(List<Integer> component:WarehouseTransferComponents.decompose(sources,targets)){
                List<Entry> group=new ArrayList<>();
                for(int i:component)group.add(entries.get(i));
                jobs.add(List.copyOf(group));totalEntries+=group.size();
            }
            components=List.copyOf(jobs);totalComponents=components.size();
            LOG.info("Global plan: {} eligible inventories, {} stacks, {} semantic groups, {} active inventories, cost {} -> {}, {} components",
                    chests.size(),entries.size(),semanticGroups,activeInventories,beforeCost,afterCost,totalComponents);
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
                    CategoryResolution resolution = ItemCategoryResolver.resolveDetailed(stack);
                    if (!resolution.category().moveAllowed()
                            || !canInsertAfterExtraction(ref.handler(), slot, stack)) return null;
                    ItemStack simulated = ref.handler().extractItem(slot, stack.getCount(), true);
                    if (!sameStackAndCount(stack, simulated)) return null;
                    SemanticItemProfile semantic = StrictStackClassifier.profile(stack,resolution);
                    chest.votes.merge(semantic.legacyFamily(), 1, Integer::sum);
                    local.add(new Entry(chest, slot, stack, semantic));
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
            return ref.level() == level && ref.isValid();
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

        private static boolean commitComponent(ServerLevel level,List<Entry> displaced,MoveListener listener) {
            // Re-snapshot touched inventories; independent components may run across ticks.
            Map<Chest,ItemStack[]> expected=new java.util.LinkedHashMap<>();
            for(Entry e:displaced){
                for(Chest chest:List.of(e.source,e.destination)){
                    if(!valid(level,chest.ref)||chest.ref.slots()!=chest.layout.length)return false;
                    if(!expected.containsKey(chest)){
                        ItemStack[] row=new ItemStack[chest.layout.length];
                        for(int s=0;s<row.length;s++)row[s]=chest.ref.handler().getStackInSlot(s).copy();
                        expected.put(chest,row);
                    }
                }
                if(!sameStackAndCount(expected.get(e.source)[e.sourceSlot],e.original))return false;
                if(!canInsertAfterExtraction(e.destination.ref.handler(),e.destinationSlot,e.original))return false;
                if(!sameStackAndCount(e.original,e.source.ref.handler().extractItem(e.sourceSlot,e.original.getCount(),true)))return false;
            }
            for(Entry e:displaced)expected.get(e.source)[e.sourceSlot]=ItemStack.EMPTY;
            for(Entry e:displaced){
                ItemStack[] row=expected.get(e.destination);
                if(!row[e.destinationSlot].isEmpty())return false;
                row[e.destinationSlot]=e.original.copy();
            }
            List<Placement> placed=new ArrayList<>();
            boolean success = false;
            try {
                for (Entry entry : displaced) {
                    IItemHandler source = entry.source.ref.handler();
                    // Validate again IMMEDIATELY before touching a physical
                    // slot. A duplicate/aliased view must never manufacture
                    // a buffer item from the older snapshot.
                    ItemStack before = source.getStackInSlot(entry.sourceSlot).copy();
                    if (!sameStackAndCount(before, entry.original)) {
                        throw new IllegalStateException("Source no longer matches snapshot at "
                                + entry.source.ref.pos() + ":" + entry.sourceSlot);
                    }
                    ItemStack reported;
                    try {
                        reported = source.extractItem(
                                entry.sourceSlot, entry.original.getCount(), false);
                    } catch (RuntimeException ex) {
                        // A broken mod may mutate and then throw; use observed
                        // slot contents, not its untrusted return value.
                        entry.carried = observedRemoval(entry, before);
                        throw ex;
                    }
                    entry.carried = observedRemoval(entry, before);
                    if (!sameStackAndCount(reported, entry.carried)
                            || !sameStackAndCount(entry.carried, entry.original)) {
                        throw new IllegalStateException("Extraction did not remove the expected stack at "
                                + entry.source.ref.pos() + ":" + entry.sourceSlot);
                    }
                }

                for (Entry entry : displaced) {
                    IItemHandler destination = entry.destination.ref.handler();
                    if (!destination.getStackInSlot(entry.destinationSlot).isEmpty()) {
                        throw new IllegalStateException("Destination unexpectedly occupied at "
                                + entry.destination.ref.pos() + ":" + entry.destinationSlot);
                    }
                    ItemStack before = entry.carried.copy();
                    ItemStack reported;
                    try {
                        reported = destination.insertItem(
                                entry.destinationSlot, before, false);
                    } catch (RuntimeException ex) {
                        observePlacement(entry, before, placed);
                        throw ex;
                    }
                    observePlacement(entry, before, placed);
                    if (!sameStackAndCount(reported, entry.carried)) {
                        throw new IllegalStateException("Insertion remainder disagrees with actual chest at "
                                + entry.destination.ref.pos() + ":" + entry.destinationSlot);
                    }
                    if (!entry.carried.isEmpty()) {
                        throw new IllegalStateException("Inventory rejected buffer item at "
                                + entry.destination.ref.pos() + ":" + entry.destinationSlot);
                    }
                }

                for(var check:expected.entrySet()){
                    Chest chest=check.getKey();
                    for(int slot=0;slot<check.getValue().length;slot++){
                        if(!sameStackAndCount(check.getValue()[slot],chest.ref.handler().getStackInSlot(slot)))
                            throw new IllegalStateException("Unexpected inventory mutation at "+chest.ref.pos()+":"+slot);
                    }
                }
                success = true;
            } catch (RuntimeException ex) {
                LOG.error("Wand buffer transaction failed; restoring original chest contents", ex);
            } finally {
                if (!success) rollback(level, displaced, placed);
            }
            if (!success) return false;

            for (Entry entry : displaced) {
                if (entry.source != entry.destination) {
                    try { listener.onMove(new MoveRecord(entry.source.ref.pos(),
                            entry.destination.ref.pos(), entry.original.getCount(),
                            entry.original.copy())); }
                    catch(RuntimeException ex){LOG.warn("Transfer visual failed after committed move",ex);}
                }
            }
            return true;
        }

        /**
         * Compute a removal from what is actually missing in the source.
         * Forge capability simulations and returned ItemStacks are not
         * sufficient evidence for exotic modded handlers.
         */
        private static ItemStack observedRemoval(Entry entry, ItemStack before) {
            ItemStack now = entry.source.ref.handler().getStackInSlot(entry.sourceSlot);
            int remaining = 0;
            if (!now.isEmpty()) {
                if (!ItemStack.isSameItemSameTags(now, before)
                        || now.getCount() > before.getCount()) {
                    throw new IllegalStateException("Source transformed the stack unexpectedly at "
                            + entry.source.ref.pos());
                }
                remaining = now.getCount();
            }
            int removed = before.getCount() - remaining;
            return removed == 0 ? ItemStack.EMPTY : before.copyWithCount(removed);
        }

        private static void observePlacement(Entry entry, ItemStack before,
                                             List<Placement> placed) {
            ItemStack now = entry.destination.ref.handler().getStackInSlot(entry.destinationSlot);
            int inserted = 0;
            if (!now.isEmpty()) {
                if (!ItemStack.isSameItemSameTags(now, before)
                        || now.getCount() > before.getCount()) {
                    throw new IllegalStateException("Destination transformed the stack at "
                            + entry.destination.ref.pos());
                }
                inserted = now.getCount();
            }
            if (inserted > 0) {
                placed.add(new Placement(entry, entry.destination, entry.destinationSlot, inserted));
            }
            int remaining = before.getCount() - inserted;
            entry.carried = remaining == 0
                    ? ItemStack.EMPTY : before.copyWithCount(remaining);
        }

        private static void rollback(ServerLevel level, List<Entry> entries,
                                     List<Placement> placed) {
            for (int i = placed.size() - 1; i >= 0; i--) {
                Placement move = placed.get(i);
                try {
                    IItemHandler handler = move.chest.ref.handler();
                    ItemStack before = handler.getStackInSlot(move.slot).copy();
                    if (before.isEmpty()
                            || !ItemStack.isSameItemSameTags(before, move.entry.original)) {
                        LOG.error("Rollback destination was modified externally: {} slot {}",
                                move.chest.ref.pos(), move.slot);
                        continue;
                    }
                    handler.extractItem(move.slot, Math.min(move.count, before.getCount()), false);
                    ItemStack after = handler.getStackInSlot(move.slot);
                    int actualLeft = after.isEmpty() ? 0 : after.getCount();
                    if (!after.isEmpty()
                            && !ItemStack.isSameItemSameTags(after, before)) {
                        LOG.error("Rollback changed destination type: {}", move.chest.ref.pos());
                        continue;
                    }
                    int recovered = before.getCount() - actualLeft;
                    if (recovered > 0) {
                        if (move.entry.carried.isEmpty()) {
                            move.entry.carried = move.entry.original.copyWithCount(recovered);
                        } else {
                            move.entry.carried.grow(recovered);
                        }
                    }
                } catch (RuntimeException ex) {
                    LOG.error("Could not recover placed stack at {} slot {}",
                            move.chest.ref.pos(), move.slot, ex);
                }
            }

            for (Entry entry : entries) {
                if (entry.carried.isEmpty()) continue;
                ItemStack remaining = entry.carried.copy();
                IItemHandler handler = entry.source.ref.handler();
                remaining = restoreToSlot(handler, entry.sourceSlot, remaining);
                for (int slot = 0; !remaining.isEmpty() && slot < entry.source.ref.slots(); slot++) {
                    if (slot != entry.sourceSlot) {
                        remaining = restoreToSlot(handler, slot, remaining);
                    }
                }
                if (!remaining.isEmpty()) emergencyDrop(level, entry.source.ref.pos(), remaining);
                entry.carried = ItemStack.EMPTY;
            }
        }

        private static ItemStack restoreToSlot(IItemHandler handler, int slot, ItemStack stack) {
            if (stack.isEmpty()) return ItemStack.EMPTY;
            int originalCount = countMatching(handler.getStackInSlot(slot), stack);
            try {
                handler.insertItem(slot, stack.copy(), false);
            } catch (RuntimeException ex) {
                LOG.error("Inventory threw while restoring slot {}", slot, ex);
            }
            int afterCount = countMatching(handler.getStackInSlot(slot), stack);
            int accepted = Math.max(0, Math.min(stack.getCount(), afterCount - originalCount));
            int rest = stack.getCount() - accepted;
            return rest == 0 ? ItemStack.EMPTY : stack.copyWithCount(rest);
        }

        private static int countMatching(ItemStack live, ItemStack sample) {
            return !live.isEmpty() && ItemStack.isSameItemSameTags(live, sample)
                    ? live.getCount() : 0;
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
