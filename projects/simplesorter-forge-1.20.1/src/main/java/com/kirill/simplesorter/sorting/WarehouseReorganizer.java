package com.kirill.simplesorter.sorting;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
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
 * Stable one-click reorganization. Creates ONE immutable ownership/layout
 * snapshot and runs it to completion in bounded server-tick slices.
 *
 * The old version recalculated shelf roles after every 9ms partial pass and
 * used a rotating source start; this could send materials A->B->A across
 * repeated clicks. This session never moves a stack OUT of its established
 * category column or a newly claimed destination.
 */
public final class WarehouseReorganizer {
    private static final int MAX_PASSES = 3;
    private static final int MAX_OPERATIONS = 4096;
    private static final int MIN_OWNED_SLOTS = 1;

    private WarehouseReorganizer() {}

    public record Result(int movedItems, int movedOperations) {}

    private record Profile(int occupied, Map<String, Integer> strictVotes,
                           Map<String, Integer> coarseVotes) {
        // A chest containing 9 ingots and 1 stick is NOT a pure ingot chest,
        // even if it would pass the old 74% acceptance threshold.
        boolean belongsTo(String strictFamily) {
            return occupied >= MIN_OWNED_SLOTS
                    && strictVotes.getOrDefault(strictFamily, 0) == occupied;
        }

        boolean mixed() {
            return strictVotes.size() > 1;
        }
    }

    public static final class Session {
        private final List<ContainerRef> containers;
        private final List<Profile> profiles;
        private final WarehouseLayout layout;
        private final WarehouseColumns columns;
        private final Map<Item, SortCategory> itemCategoryCache = new IdentityHashMap<>();
        private final Map<String, List<Integer>> candidateOrder = new HashMap<>();
        private final Map<String, Boolean> anchoredFamilyCache = new HashMap<>();
        private final Map<Integer, String> claimed = new HashMap<>();

        private int sourceIndex = 0;
        private int slotIndex = 0;
        private int pass = 0;
        private int passMoved = 0;
        private int totalItems = 0;
        private int operations = 0;
        private boolean done = false;

        public Session(List<ContainerRef> all) {
            containers = List.copyOf(all);
            profiles = new ArrayList<>(containers.size());
            List<WarehouseLayout.Node> nodes = new ArrayList<>(containers.size());

            // Build the semantic snapshot exactly once per user activation.
            for (ContainerRef ref : containers) {
                Profile profile = profile(ref, itemCategoryCache);
                profiles.add(profile);
                BlockPos p = ref.pos();
                nodes.add(new WarehouseLayout.Node(
                        p.getX(), p.getY(), p.getZ(),
                        profile.occupied(), profile.coarseVotes()));
            }
            layout = WarehouseLayout.analyze(nodes, false);
            columns = WarehouseColumns.analyze(nodes.stream()
                    .map(n -> new WarehouseColumns.Node(
                            n.x(), n.y(), n.z(), n.occupied(), n.labels()))
                    .toList());
        }

        public boolean isDone() { return done; }
        public int movedItems() { return totalItems; }
        public int movedOperations() { return operations; }
        public int examinedPasses() { return pass + 1; }

        /**
         * Run at most maxSlots visited and maxNanos per server tick. The task
         * continues automatically on subsequent ticks; no more RMB clicks.
         */
        public boolean step(ServerLevel level, MoveListener listener,
                            int maxSlots, long maxNanos) {
            if (done) return true;
            long deadline = System.nanoTime() + maxNanos;
            int inspected = 0;

            while (inspected < maxSlots && System.nanoTime() < deadline && !done) {
                if (operations >= MAX_OPERATIONS) {
                    done = true;
                    break;
                }
                if (sourceIndex >= containers.size()) {
                    pass++;
                    if (pass >= MAX_PASSES || passMoved == 0) {
                        done = true;
                        break;
                    }
                    sourceIndex = 0;
                    slotIndex = 0;
                    passMoved = 0;
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
                inspected++;
                ItemStack stack = handler.getStackInSlot(activeSlot);
                if (stack.isEmpty()) continue;

                SortCategory category = itemCategoryCache.computeIfAbsent(
                        stack.getItem(), ignored -> ItemCategoryResolver.resolve(stack));
                if (!category.moveAllowed()) continue;
                String broadFamily = WarehouseLayout.family(category.id());
                String family = strictFamily(stack, category);
                if ("locked".equals(family)) continue;

                int current = sourceIndex;
                if (layout.axis(current) == WarehouseLayout.Axis.NONE) continue;
                Profile sourceProfile = profiles.get(current);
                if (family.equals(claimed.get(current))) continue;
                // Purely populated storage is already a safe end state.
                // Moving it for a small positional improvement only causes
                // endless reshuffling after each RMB.
                if (sourceProfile.belongsTo(family)) continue;

                double sourceFit = layout.preference(current, category.id());
                String sourceRole = columns.role(current);
                String sourceClaim = claimed.getOrDefault(current, "");
                int region = columns.region(current);
                String grouping = region + ":" + family;
                boolean hasHome = anchoredFamilyCache.computeIfAbsent(grouping,
                        ignored -> columns.hasEstablishedColumn(current, category.id()));

                for (int targetIndex : sortedCandidates(grouping, current, category.id(), family)) {
                    if (targetIndex == current || !columns.sameRegion(current, targetIndex)) continue;
                    ContainerRef target = containers.get(targetIndex);
                    if (!valid(level, target)) continue;

                    Profile targetProfile = profiles.get(targetIndex);
                    String targetRole = columns.role(targetIndex);
                    String targetClaim = claimed.getOrDefault(targetIndex, "");
                    // Hard semantic gate. The 0.9.10 rule accepted a chest
                    // with 74% metals + 26% unrelated items as a metal home.
                    // Now any incompatible occupied slot rejects insertion.
                    if (!targetProfile.belongsTo(family) && targetProfile.occupied() > 0) continue;
                    if (!targetClaim.isEmpty() && !family.equals(targetClaim)) continue;
                    if (!targetRole.isEmpty() && !targetRole.equals(broadFamily)) continue;

                    boolean correctColumnExists = hasHome && !"misc".equals(broadFamily);
                    if (correctColumnExists && !columns.matches(targetIndex, category.id())) {
                        continue;
                    }

                    // When the source chest is mixed, moving the misplaced
                    // item into a pure matching or empty unclaimed container
                    // is an improvement regardless of a broad Y-level score.
                    // The destination then gets a permanent strict claim for
                    // this sorting session. This also handles unknown sticks
                    // and water bottles without pretending they are ingots.
                    boolean belongs = targetProfile.belongsTo(family)
                            || family.equals(targetClaim);
                    boolean allowed = sourceProfile.mixed()
                            && (targetProfile.occupied() == 0 || belongs)
                            && !correctColumnExists
                            || sourceProfile.mixed() && correctColumnExists && belongs;
                    if (!allowed && !"misc".equals(broadFamily)) {
                        allowed = WarehouseRoutingRules.mayMove(
                                broadFamily, sourceRole, sourceClaim, targetRole,
                                targetClaim.isEmpty() ? "" : broadFamily,
                                correctColumnExists, sourceFit,
                                layout.preference(targetIndex, category.id()),
                                targetProfile.occupied(), belongs);
                    }
                    if (!allowed) continue;

                    int moved = InventoryMover.moveSlotToTargets(
                            source, activeSlot, List.of(target), listener);
                    if (moved <= 0) continue;

                    claimed.put(targetIndex, family);
                    totalItems += moved;
                    passMoved += moved;
                    operations++;
                    // A source slot can be partially extracted by TFC capacity.
                    // Never send a subsequent part to a different category home.
                    if (handler.getStackInSlot(activeSlot).isEmpty()) break;
                }
            }
            return done;
        }

        private List<Integer> sortedCandidates(String grouping, int source,
                                               String category, String family) {
            return candidateOrder.computeIfAbsent(grouping, key -> {
                List<Integer> result = new ArrayList<>();
                for (int i = 0; i < containers.size(); i++) {
                    if (columns.sameRegion(source, i)) result.add(i);
                }
                result.sort(Comparator
                        .comparingDouble((Integer idx) ->
                                (profiles.get(idx).belongsTo(family) ? 250D : 0D)
                                + (columns.matches(idx, category) ? 100D : 0D)
                                + 10D * layout.preference(idx, category)).reversed()
                        .thenComparing(Comparator.comparingInt(
                                (Integer idx) -> profiles.get(idx).occupied()).reversed())
                        .thenComparingLong(idx -> containers.get(idx).pos().asLong()));
                return List.copyOf(result);
            });
        }

        private static boolean valid(ServerLevel level, ContainerRef ref) {
            return level.hasChunkAt(ref.pos())
                    && level.getBlockEntity(ref.pos()) == ref.identity();
        }
    }

    public static Session begin(List<ContainerRef> containers) {
        return new Session(containers);
    }

    /**
     * Compatibility entrypoint for older callsites. The primary server path
     * uses Session.step over ticks via WarehouseSortSessions.
     */
    public static Result organize(List<ContainerRef> containers, MoveListener listener) {
        if (containers.isEmpty()) return new Result(0, 0);
        Session session = begin(containers);
        // Never busy-wait on the main thread: a legacy direct call gets a
        // single bounded slice. No follow-up tick state is retained here.
        session.step(containers.get(0).level(), listener, 192, 4_000_000L);
        return new Result(session.movedItems(), session.movedOperations());
    }

    private static String strictFamily(ItemStack stack, SortCategory category) {
        String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        boolean waterPotion = "minecraft:potion".equals(itemId)
                && PotionUtils.getPotion(stack) == Potions.WATER;
        return StrictStorageFamily.of(itemId, category.id(), waterPotion);
    }

    private static Profile profile(ContainerRef ref, Map<Item, SortCategory> cache) {
        Map<String, Integer> strict = new HashMap<>();
        Map<String, Integer> coarse = new HashMap<>();
        int occupied = 0;
        IItemHandler handler = ref.handler();
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack stack = handler.getStackInSlot(slot);
            if (stack.isEmpty()) continue;
            occupied++;
            SortCategory category = cache.computeIfAbsent(stack.getItem(),
                    ignored -> ItemCategoryResolver.resolve(stack));
            String family = strictFamily(stack, category);
            strict.merge(family, 1, Integer::sum);
            String broad = category.moveAllowed()
                    ? WarehouseLayout.family(category.id()) : "misc";
            coarse.merge(broad, 1, Integer::sum);
        }
        return new Profile(occupied, Map.copyOf(strict), Map.copyOf(coarse));
    }
}
