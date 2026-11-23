package com.kirill.simplesorter.sorting;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Smart Categories 0.9.12: each CHEST has its own strict semantic family.
 *
 * A vertical column is no longer a mutually exclusive category. It is only
 * a preference for the destination of a category which already has a strong
 * home elsewhere in the same warehouse. E.g. ore in column six moves into
 * spare capacity of column one, but tools/ingots/clay may occupy different
 * chests of the SAME column.
 *
 * The routing plan and original family anchors are calculated once per
 * activation; transfers run in time-limited server-tick slices. A destination
 * must be empty or 100% compatible with the moved stack. No arbitrary
 * "mostly matching" category percentages or cross-warehouse transfers.
 */
public final class WarehouseReorganizer {
    private static final int MAX_PASSES = 3;
    private static final int MAX_OPERATIONS = 4096;
    private static final int MAX_TARGETS_PER_STACK = 384;

    private WarehouseReorganizer() {}

    public record Result(int movedItems, int movedOperations) {}

    private static final class ChestState {
        private final Map<String, Integer> families = new HashMap<>();
        private int occupied;

        void refresh(ContainerRef ref, Map<Item, SortCategory> cache) {
            families.clear();
            occupied = 0;
            IItemHandler inventory = ref.handler();
            for (int slot = 0; slot < inventory.getSlots(); slot++) {
                ItemStack stack = inventory.getStackInSlot(slot);
                if (stack.isEmpty()) continue;
                occupied++;
                SortCategory category = cache.computeIfAbsent(
                        stack.getItem(), item -> ItemCategoryResolver.resolve(stack));
                String family = StrictStackClassifier.family(stack, category);
                families.merge(family, 1, Integer::sum);
            }
        }

        boolean mixed() { return families.size() > 1; }
        boolean pure(String family) {
            return occupied > 0 && families.getOrDefault(family, 0) == occupied;
        }
        boolean empty() { return occupied == 0; }

        String majority() {
            return families.entrySet().stream()
                    .sorted(Comparator
                            .comparingInt((Map.Entry<String, Integer> e) -> e.getValue())
                            .reversed()
                            .thenComparing(e -> "misc".equals(e.getKey()) ? 1 : 0)
                            .thenComparing(Map.Entry::getKey))
                    .map(Map.Entry::getKey).findFirst().orElse("");
        }

        boolean canAccept(String family, String claim) {
            if (family == null || "locked".equals(family)) return false;
            return StrictChestAdmission.canReceive(
                    family, families, occupied, claim);
        }

        Map<String, Integer> frozenVotes() {
            return Map.copyOf(families);
        }
    }

    public static final class Session {
        private final List<ContainerRef> containers;
        private final List<ChestState> states;
        private final WarehouseFamilyPlan plan;
        private final Map<Item, SortCategory> itemCategoryCache = new IdentityHashMap<>();
        private final Map<String, List<Integer>> candidateOrder = new HashMap<>();
        private final Map<Integer, String> reservations = new HashMap<>();
        private final List<String> keepAtSource = new ArrayList<>();

        private int sourceIndex;
        private int slotIndex;
        private int pass;
        private int movedThisPass;
        private int movedItems;
        private int operations;
        // Carry the inner candidate-loop cursor over to the next server
        // tick, instead of searching up to 384 modded inventories in one tick.
        private int pendingSource = -1;
        private int pendingSlot = -1;
        private int pendingCandidate;
        private boolean done;

        public Session(List<ContainerRef> all) {
            containers = List.copyOf(all);
            states = new ArrayList<>(containers.size());
            List<WarehouseFamilyPlan.Node> nodes = new ArrayList<>(containers.size());
            for (ContainerRef ref : containers) {
                ChestState state = new ChestState();
                state.refresh(ref, itemCategoryCache);
                states.add(state);
                keepAtSource.add(state.majority());
                BlockPos p = ref.pos();
                nodes.add(new WarehouseFamilyPlan.Node(
                        p.getX(), p.getY(), p.getZ(), state.frozenVotes()));
            }
            plan = WarehouseFamilyPlan.analyze(nodes);
        }

        public boolean isDone() { return done; }
        public int movedItems() { return movedItems; }
        public int movedOperations() { return operations; }
        public int examinedPasses() { return pass + 1; }

        /** Small, bounded main-thread slice; work automatically continues
         * on the next server tick, without any additional wand clicks. */
        public boolean step(ServerLevel level, MoveListener listener,
                            int maxSlots, long maxNanos) {
            if (done) return true;
            long deadline = System.nanoTime() + maxNanos;
            int checkedSlots = 0;

            while (checkedSlots < maxSlots && System.nanoTime() < deadline && !done) {
                if (operations >= MAX_OPERATIONS) {
                    done = true;
                    break;
                }

                if (sourceIndex >= containers.size()) {
                    pass++;
                    if (pass >= MAX_PASSES || movedThisPass == 0) {
                        done = true;
                        break;
                    }
                    movedThisPass = 0;
                    sourceIndex = 0;
                    slotIndex = 0;
                    continue;
                }

                ContainerRef source = containers.get(sourceIndex);
                if (!valid(level, source)) {
                    sourceIndex++;
                    slotIndex = 0;
                    continue;
                }
                IItemHandler handler = source.handler();
                if (slotIndex >= handler.getSlots()) {
                    sourceIndex++;
                    slotIndex = 0;
                    continue;
                }
                int activeSlot = slotIndex++;
                checkedSlots++;
                if (activeSlot == 0) {
                    // A player or other automation may have modified this
                    // chest between server ticks; never trust stale contents.
                    states.get(sourceIndex).refresh(source, itemCategoryCache);
                }

                ItemStack stack = handler.getStackInSlot(activeSlot);
                if (stack.isEmpty()) continue;
                SortCategory category = itemCategoryCache.computeIfAbsent(
                        stack.getItem(), item -> ItemCategoryResolver.resolve(stack));
                if (!category.moveAllowed()) continue;

                String family = StrictStackClassifier.family(stack, category);
                if ("locked".equals(family)) continue;
                ChestState sourceState = states.get(sourceIndex);

                boolean mixedSource = sourceState.mixed();
                if (!WarehouseConsolidationDecision.shouldRelocate(
                        plan, sourceIndex, family, mixedSource,
                        keepAtSource.get(sourceIndex))) continue;

                String pool = plan.region(sourceIndex) + ":" + family;
                List<Integer> choices = candidateOrder.computeIfAbsent(
                        pool, ignored -> candidateIndices(sourceIndex, family));
                int startAt = pendingSource == sourceIndex && pendingSlot == activeSlot
                        ? pendingCandidate : 0;

                for (int choiceIndex = startAt;
                        choiceIndex < choices.size()
                            && choiceIndex < MAX_TARGETS_PER_STACK; choiceIndex++) {
                    if (System.nanoTime() >= deadline) {
                        // Keep the source slot/candidate intact. The next
                        // server tick continues from exactly this target,
                        // with no repeated failed inventory scans.
                        pendingSource = sourceIndex;
                        pendingSlot = activeSlot;
                        pendingCandidate = choiceIndex;
                        slotIndex = activeSlot;
                        return false;
                    }

                    int targetIndex = choices.get(choiceIndex);
                    if (targetIndex == sourceIndex) continue;
                    ContainerRef target = containers.get(targetIndex);
                    if (!valid(level, target)) continue;

                    ChestState targetState = states.get(targetIndex);
                    // Be safe if another player added a foreign item while a
                    // multi-tick sort was working through the warehouse.
                    targetState.refresh(target, itemCategoryCache);
                    if (!WarehouseConsolidationDecision.eligibleTarget(
                            plan, sourceIndex, targetIndex, family, mixedSource,
                            targetState.pure(family), targetState.empty())) continue;
                    String claim = reservations.getOrDefault(targetIndex, "");
                    if (!targetState.canAccept(family, claim)) continue;

                    int moved = InventoryMover.moveSlotToTargets(
                            source, activeSlot, List.of(target), listener);
                    if (moved <= 0) continue;

                    sourceState.refresh(source, itemCategoryCache);
                    targetState.refresh(target, itemCategoryCache);
                    reservations.put(targetIndex, family);
                    movedItems += moved;
                    movedThisPass += moved;
                    operations++;
                    if (handler.getStackInSlot(activeSlot).isEmpty()) break;
                }
                pendingSource = -1;
                pendingSlot = -1;
                pendingCandidate = 0;
            }
            return done;
        }

        private List<Integer> candidateIndices(int source, String family) {
            List<Integer> result = plan.indicesInRegion(source);
            result.sort(Comparator
                    .comparingInt((Integer i) ->
                            plan.priority(i, family, states.get(i).pure(family))).reversed()
                    .thenComparing(Comparator
                            .comparingInt((Integer i) -> states.get(i).pure(family) ? 1 : 0)
                            .reversed())
                    .thenComparingLong(i -> containers.get(i).pos().asLong()));
            return List.copyOf(result);
        }

        private static boolean valid(ServerLevel level, ContainerRef ref) {
            return level.hasChunkAt(ref.pos())
                    && level.getBlockEntity(ref.pos()) == ref.identity();
        }
    }

    public static Session begin(List<ContainerRef> containers) {
        return new Session(containers);
    }

    /** Legacy compatibility for older callsites. New normal Smart Categories
     * runs Session over server ticks through WarehouseSortSessions. */
    public static Result organize(List<ContainerRef> containers, MoveListener listener) {
        if (containers.isEmpty()) return new Result(0, 0);
        Session session = begin(containers);
        session.step(containers.get(0).level(), listener, 120, 4_000_000L);
        return new Result(session.movedItems(), session.movedOperations());
    }
}
