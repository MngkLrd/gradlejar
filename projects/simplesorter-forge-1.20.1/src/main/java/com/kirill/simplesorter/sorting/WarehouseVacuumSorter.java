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
        return new Session(containers,ref->true,false);
    }

    public static Session begin(List<ContainerRef> containers,java.util.function.Predicate<ContainerRef> access){
        return new Session(containers,access,false);
    }

    public static Session begin(List<ContainerRef> containers,java.util.function.Predicate<ContainerRef> access,boolean byMod){
        return new Session(containers,access,byMod);
    }

    private static final class Chest {
        final ContainerRef ref;
        final int sequence;
        final ItemStack[] snapshot;
        final Entry[] layout;
        final Map<String, Integer> votes = new HashMap<>();
        final Map<String, Integer> allocatedFamilies = new HashMap<>();
        final java.util.Set<Integer> pinnedSlots=new java.util.HashSet<>();
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
        final boolean movable;
        Chest destination;
        int destinationSlot = -1;
        ItemStack carried = ItemStack.EMPTY;

        Entry(Chest source, int sourceSlot, ItemStack original, SemanticItemProfile semantic,boolean movable) {
            this.source = source;
            this.sourceSlot = sourceSlot;
            this.original = original;
            this.semantic = semantic;
            this.movable=movable;
            this.family = semantic.legacyFamily();
        }
    }

    private record Placement(Entry entry, Chest chest, int slot, int count) {}

    public static final class Session {
        private final List<ContainerRef> containers;
        private List<List<Entry>> components=List.of();
        private boolean planned;
        private boolean unsafe;
        private final java.util.function.Predicate<ContainerRef> access;
        private final boolean byMod;
        private final List<Chest> planningChests=new ArrayList<>();
        private final List<Entry> planningEntries=new ArrayList<>();
        private int snapshotIndex,validationIndex;
        private AdaptiveWarehousePlanner.Search search;
        private final java.util.Set<ContainerRef> eligible=new java.util.HashSet<>();
        private int componentIndex,totalComponents,processedEntries,totalEntries;
        private int activeInventories,semanticGroups;
        private long beforeCost,afterCost;
        private int processed;
        private int movedItems;
        private int moves;
        private int skippedChests,pinnedSlots,eligibleEmpty,eligibleSlots;
        private final java.util.Map<String,Integer> pinReasons=new java.util.TreeMap<>();
        private int rejectedBatches;
        private String lastFailure = "none";

        private Session(List<ContainerRef> containers,java.util.function.Predicate<ContainerRef> access,boolean byMod) {
            this.containers = List.copyOf(containers);
            this.access=access;
            this.byMod=byMod;

        }

        public int movedItems() { return movedItems; }
        public int movedOperations() { return moves; }
        public int attemptedTransfers() { return moves + rejectedBatches; }
        public int rejectedTransfers() { return rejectedBatches; }
        public int skippedChests() { return skippedChests; }
        public int pinnedSlots(){return pinnedSlots;}
        public String eligibilitySummary(){return (containers.size()-skippedChests)+" eligible / "+containers.size()+" scanned; "+
                eligibleEmpty+" empty eligible; "+eligibleSlots+" eligible slots; "+pinnedSlots+" fixed slots; reasons "+pinReasons;}
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
        public boolean eligibleForCompaction(ContainerRef ref){return !unsafe&&eligible.contains(ref)&&access.test(ref);}
        public boolean unsafe(){return unsafe;}
        public boolean isPlanning(){return !planned;}

        // Diagnostics are derived from actual post-sort inventories, not a
        // frozen pre-sort snapshot. Unknown "misc" is a valid fallback family.
        public void refreshContainer(int ignored) {}

        public record Quality(int mixed,int occupied,int score){}
        public Quality quality(){
            int mixed=0,occupied=0;long sum=0,slots=0;
            for(ContainerRef ref:containers){
                try{
                    if(!ref.isValid())continue;
                    List<SemanticItemProfile> profiles=new ArrayList<>();
                    for(int s=0;s<ref.slots();s++){
                        ItemStack stack=ref.handler().getStackInSlot(s);
                        if(!stack.isEmpty()){
                            var profile=StrictStackClassifier.profile(stack);
                            profiles.add(byMod?ModOriginProfile.of(profile):profile);
                        }
                    }
                    var q=SemanticChestQuality.evaluate(profiles);
                    if(q.occupied()==0)continue;
                    occupied++;if(q.mixed())mixed++;
                    slots+=q.occupied();sum+=(long)q.score()*q.occupied();
                }catch(RuntimeException ex){LOG.warn("Cannot inspect chest quality at {}",ref.pos(),ex);}
            }
            return new Quality(mixed,occupied,slots==0?100:(int)Math.round((double)sum/slots));
        }
        public int unresolvedMixedChests(){return quality().mixed();}

        public int unknownFamilySlots() {
            if(byMod)return 0;
            int count = 0;
            for (ContainerRef ref : containers) {
                try {
                    for (int s = 0; s < ref.slots(); s++) {
                        ItemStack stack = ref.handler().getStackInSlot(s);
                        if (!stack.isEmpty() && "unknown".equals(StrictStackClassifier.profile(stack).group())) {
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
            if(byMod)return "";
            Map<String, Integer> counts = new HashMap<>();
            for (ContainerRef ref : containers) {
                try {
                    for (int s = 0; s < ref.slots(); s++) {
                        ItemStack stack = ref.handler().getStackInSlot(s);
                        if (!stack.isEmpty() && "unknown".equals(StrictStackClassifier.profile(stack).group())) {
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
                try { if(!prepareStep(level,slotLimit,timeLimit))return false; planned=true; }
                catch(RuntimeException ex){
                    planned=true;unsafe=true;eligible.clear();
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
                try{
                    boolean allowed=true;
                    for(Entry e:group)if(!access.test(e.source.ref)||!access.test(e.destination.ref)){allowed=false;break;}
                    if(allowed)ok=commitComponent(level,group,listener);
                }catch(RuntimeException ex){
                    unsafe=true;LOG.error("Warehouse component failed audit; stop remaining transfers",ex);
                }
                if(ok){
                    for(Entry e:group)if(e.source!=e.destination){movedItems+=e.original.getCount();moves++;}
                }else{
                    rejectedBatches++;
                    lastFailure="component changed or inventory refused transfer (see latest.log)";
                }
                if(unsafe){eligible.clear();return true;}
                processedEntries+=group.size();
                processed=totalEntries==0?containers.size():(int)((long)processedEntries*containers.size()/totalEntries);
            }
            return componentIndex>=components.size();
        }

        private boolean prepareStep(ServerLevel level,int slotLimit,long timeLimit){
            List<Chest> chests=planningChests;
            List<Entry> entries=planningEntries;
            long start=System.nanoTime();
            int read=0;
            if(snapshotIndex<containers.size()){
                while(snapshotIndex<containers.size() && (read==0||
                        (read<Math.max(1,slotLimit)&&System.nanoTime()-start<timeLimit))){
                    ContainerRef ref=containers.get(snapshotIndex++);
                    Chest chest=access.test(ref)?snapshotEligible(level,ref,chests.size(),entries):null;
                    if(chest==null){
                        skippedChests++;LOG.warn("Planning excluded inventory {} (invalid, inaccessible or unsupported)",ref.pos());
                    }else {
                        chests.add(chest);eligibleSlots+=chest.layout.length;
                        if(chest.votes.isEmpty())eligibleEmpty++;
                        // Compaction must never touch a fixed slot, even if
                        // another stack in this inventory is movable.
                        if(chest.pinnedSlots.isEmpty())eligible.add(ref);
                    }
                    read+=Math.max(1,ref.slots());
                }
                processed=snapshotIndex;
                return false;
            }
            if(search==null)LOG.info("Planning input: {} eligible inventories, {} empty, {} slots, {} fixed slots, reasons {}",
                    chests.size(),eligibleEmpty,eligibleSlots,pinnedSlots,pinReasons);
            if(chests.size()<2||entries.isEmpty())return true;
            if(search==null){
                List<AdaptiveWarehousePlanner.Storage> boxes=new ArrayList<>();
                for(Chest chest:chests){
                    BlockPos p=chest.ref.pos(),q=chest.ref.partnerPos();
                    boxes.add(new AdaptiveWarehousePlanner.Storage(chest.layout.length,
                            p.getX()+(q==null?p.getX():q.getX()),p.getY(),
                            p.getZ()+(q==null?p.getZ():q.getZ())));
                }
                List<AdaptiveWarehousePlanner.Stack> items=new ArrayList<>();
                for(Entry e:entries)items.add(new AdaptiveWarehousePlanner.Stack(e.source.sequence,e.sourceSlot,e.semantic,e.movable));
                search=new AdaptiveWarehousePlanner.Search(items,boxes,
                        (item,chest,slot)->canInsertAfterExtraction(chests.get(chest).ref.handler(),
                                slot,entries.get(item).original),true);
                return false;
            }
            if(!search.advance())return false;
            // Revalidate incrementally after search. Each execution component
            // also checks its live source and destination just before mutation.
            while(validationIndex<chests.size() && (read==0||
                    (read<Math.max(1,slotLimit)&&System.nanoTime()-start<timeLimit))){
                Chest chest=chests.get(validationIndex++);
                if(!access.test(chest.ref)||!unchanged(level,List.of(chest))){
                    rejectedBatches++;lastFailure="inventory changed during planning at "+chest.ref.pos();eligible.clear();return true;
                }
                read+=chest.snapshot.length;
            }
            if(validationIndex<chests.size())return false;
            AdaptiveWarehousePlanner.Plan plan=search.result();
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
            // Validation was sliced above; per-component preflight is mandatory.
            List<List<Entry>> jobs=new ArrayList<>();
            for(List<Integer> component:WarehouseTransferComponents.decompose(sources,targets)){
                List<Entry> group=new ArrayList<>();
                for(int i:component)group.add(entries.get(i));
                jobs.add(List.copyOf(group));totalEntries+=group.size();
            }
            components=List.copyOf(jobs);totalComponents=components.size();
            LOG.info("Global plan: {} eligible inventories, {} stacks, {} semantic groups, {} active inventories, cost {} -> {}, {} components",
                    chests.size(),entries.size(),semanticGroups,activeInventories,beforeCost,afterCost,totalComponents);
            planningChests.clear();planningEntries.clear();search=null;processed=0;
            return true;
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
                    SemanticItemProfile semantic=StrictStackClassifier.profile(stack,resolution);
                    if(byMod)semantic=ModOriginProfile.of(semantic);
                    String reason=null;
                    if(!resolution.category().moveAllowed())reason="protected";
                    else if(!canInsertAfterExtraction(ref.handler(),slot,stack))reason="cannot reinsert";
                    else{
                        try{
                            ItemStack simulated=ref.handler().extractItem(slot,stack.getCount(),true);
                            if(!sameStackAndCount(stack,simulated))reason="extraction simulation differs";
                        }catch(RuntimeException ex){
                            reason="extraction simulation threw";
                            LOG.debug("Cannot simulate extraction {}:{}",ref.pos(),slot,ex);
                        }
                    }
                    if(reason!=null){
                        chest.pinnedSlots.add(slot);pinnedSlots++;pinReasons.merge(reason,1,Integer::sum);
                        LOG.debug("Fixed slot {}:{} {} ({})",ref.pos(),slot,semantic.itemId(),reason);
                    }
                    chest.votes.merge(semantic.legacyFamily(),1,Integer::sum);
                    local.add(new Entry(chest,slot,stack,semantic,reason==null));
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
                    ItemStack live=chest.ref.handler().getStackInSlot(slot);
                    if(chest.pinnedSlots.contains(slot)){
                        if(chest.snapshot[slot].getCount()!=live.getCount()||!chest.snapshot[slot].is(live.getItem()))return false;
                    }else if(!sameStackAndCount(chest.snapshot[slot],live)){
                        LOG.warn("Planning snapshot changed at {} slot {}",chest.ref.pos(),slot);return false;
                    }
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
                    && ItemStack.isSameItemSameTags(a,b)
                    && InventoryConservation.sameIdentity(a,b);
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
            List<ItemStack> auditBefore=new ArrayList<>();
            for(ItemStack[] row:expected.values())java.util.Collections.addAll(auditBefore,row);
            var ledger=InventoryConservation.count(auditBefore);
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
                                entry.destinationSlot, before.copy(), false);
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
                InventoryConservation.requireEqual(ledger,auditLive(expected.keySet(),List.of()),"successful component");
                success = true;
            } catch (RuntimeException ex) {
                LOG.error("Wand buffer transaction failed; restoring original chest contents", ex);
            } finally {
                if (!success){
                    List<ItemStack> recovered=rollback(level,displaced,placed);
                    InventoryConservation.requireEqual(ledger,auditLive(expected.keySet(),recovered),"component rollback");
                    if(!recovered.isEmpty())throw new IllegalStateException("Emergency recovery dropped items; stop and inspect warehouse");
                }
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

        private static java.util.Map<net.minecraft.nbt.CompoundTag,Long> auditLive(
                java.util.Set<Chest> chests,List<ItemStack> recovered){
            List<ItemStack> stacks=new ArrayList<>(recovered);
            for(Chest chest:chests)stacks.addAll(InventoryConservation.snapshot(chest.ref.handler()));
            return InventoryConservation.count(stacks);
        }

        private static List<ItemStack> rollback(ServerLevel level,List<Entry> entries,
                                     List<Placement> placed) {
            List<ItemStack> recoveredDrops=new ArrayList<>();
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
                    try{handler.extractItem(move.slot,Math.min(move.count,before.getCount()),false);}
                    catch(RuntimeException ex){LOG.error("Rollback extraction threw; measuring actual slot delta",ex);}
                    ItemStack after = handler.getStackInSlot(move.slot);
                    int actualLeft = after.isEmpty() ? 0 : after.getCount();
                    if (!after.isEmpty()
                            && !ItemStack.isSameItemSameTags(after, before)) {
                        LOG.error("Rollback changed destination type: {}", move.chest.ref.pos());
                        continue;
                    }
                    int recovered = Math.min(move.count,before.getCount() - actualLeft);
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
                if(!remaining.isEmpty()){
                    if(emergencyDrop(level,entry.source.ref.pos(),remaining))recoveredDrops.add(remaining.copy());
                    else LOG.error("Emergency item entity rejected at {}",entry.source.ref.pos());
                }
                entry.carried = ItemStack.EMPTY;
            }
            return recoveredDrops;
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

        private static boolean emergencyDrop(ServerLevel level, BlockPos origin, ItemStack stack) {
            if (stack.isEmpty()) return true;
            LOG.error("EMERGENCY RECOVERY: dropping {} x {} at {} after inventory rollback failure",
                    stack.getCount(), BuiltInRegistries.ITEM.getKey(stack.getItem()), origin);
            ItemEntity entity = new ItemEntity(level,
                    origin.getX() + 0.5, origin.getY() + 1.0,
                    origin.getZ() + 0.5, stack.copy());
            entity.setDefaultPickUpDelay();
            return level.addFreshEntity(entity);
        }
    }
}
