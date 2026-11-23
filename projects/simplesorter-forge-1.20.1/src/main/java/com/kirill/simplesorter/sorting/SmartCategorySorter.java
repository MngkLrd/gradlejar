package com.kirill.simplesorter.sorting;

import com.kirill.simplesorter.config.SorterSettings;
import com.kirill.simplesorter.settings.SortingMode;
import com.kirill.simplesorter.settings.SortingWandSettings;
import com.kirill.simplesorter.visual.SortingVisuals;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Context-aware, deterministic sorting of ordinary inventories.
 *
 * Never accept the current chest merely because it is compatible: ALL
 * candidate chests are compared against the current chest's contextual fit.
 * One scan builds many-label chest profiles and compact reverse indexes.
 * Routing is planned from that immutable snapshot before any stack is moved.
 */
public final class SmartCategorySorter {
    private SmartCategorySorter() {}

    public static SortingResult sort(ServerLevel level, List<ContainerRef> containers,
                                     int radius, SortingWandSettings settings) {
        boolean categoryMode = settings.mode() == SortingMode.SMART_CATEGORIES;
        List<StorageSnapshot> snapshots = new ArrayList<>();
        Map<StackKey, SortCategory> categories = new LinkedHashMap<>();
        Map<StackKey, ItemStack> samples = new LinkedHashMap<>();
        Map<StackKey, List<StorageSnapshot>> byExactStack = new LinkedHashMap<>();
        Map<String, List<StorageSnapshot>> byCategory = new HashMap<>();
        Map<String, List<StorageSnapshot>> compatibilityCache = new HashMap<>();

        for (ContainerRef ref : containers) {
            snapshots.add(snapshot(ref, categories, samples));
        }
        snapshots.sort(Comparator.comparingLong(s -> s.ref().pos().asLong()));

        // A one-shot spatial layout plan. Never saved to the world and never
        // requires pre-labelling chests. Strongly seeded rows are learned from
        // the snapshot; otherwise ordinary warehouse ordering is used.
        Map<StorageSnapshot, Integer> snapshotIndex = new IdentityHashMap<>();
        List<WarehouseLayout.Node> positions = new ArrayList<>(snapshots.size());
        for (int i = 0; i < snapshots.size(); i++) {
            StorageSnapshot current = snapshots.get(i);
            snapshotIndex.put(current, i);
            BlockPos p = current.ref().pos();
            positions.add(new WarehouseLayout.Node(
                    p.getX(), p.getY(), p.getZ(),
                    current.occupied(), current.labels()));
        }
        WarehouseLayout layout = categoryMode
                ? WarehouseLayout.analyze(positions)
                : WarehouseLayout.analyze(List.of());


        for (StorageSnapshot snapshot : snapshots) {
            for (StackKey key : snapshot.counts().keySet()) {
                byExactStack.computeIfAbsent(key, ignored -> new ArrayList<>()).add(snapshot);
            }
            for (String label : snapshot.labels().keySet()) {
                byCategory.computeIfAbsent(label, ignored -> new ArrayList<>()).add(snapshot);
            }
        }

        // Phase 1: find the best *existing* destinations, using one frozen
        // snapshot. Phase 2: reserve empty chests for unsupported categories
        // that are genuinely mixed into incompatible stores.
        Map<StackKey, StorageSnapshot> homes = new LinkedHashMap<>();
        Set<StackKey> emptyAllocations = new HashSet<>();
        Map<String, List<StorageSnapshot>> reservedPartitions = new LinkedHashMap<>();
        Map<StorageSnapshot, Integer> virtualFreeSlots = new IdentityHashMap<>();
        Set<StorageSnapshot> reservedContainers = new HashSet<>();
        List<StorageSnapshot> emptyChests = snapshots.stream()
                .filter(snapshot -> snapshot.occupied() == 0)
                .toList();

        for (StackKey key : samples.keySet()) {
            SortCategory resolved = categories.get(key);
            if (resolved == null || !resolved.moveAllowed()) continue;

            String label = resolved.id();
            ItemStack sample = samples.get(key);
            List<StorageSnapshot> originals = byExactStack.getOrDefault(key, List.of());
            // One category-to-container intersection per distinct form.
            // Thousands of NBT-different GregTech/TFC stacks reuse this index.
            List<StorageSnapshot> related = categoryMode
                    ? compatibilityCache.computeIfAbsent(label, requested -> {
                        Set<StorageSnapshot> result = new LinkedHashSet<>();
                        if (RoutingPolicy.allowsCategoryFallback(requested)) {
                            for (Map.Entry<String, List<StorageSnapshot>> entry : byCategory.entrySet()) {
                                if (RoutingPolicy.compatibility(requested, entry.getKey()) >= 0.50D) {
                                    result.addAll(entry.getValue());
                                }
                            }
                        }
                        return List.copyOf(result);
                    })
                    : List.of();
            StorageSnapshot best = chooseHome(key, label, sample, categoryMode,
                    originals, related, layout, snapshotIndex);

            // Prefer an established chest over a brand-new empty one. An
            // empty chest is useful ONLY when no better existing home exists.
            if (categoryMode && originals.size() == 1 && best != null
                    && best == originals.get(0)) {
                StorageSnapshot source = originals.get(0);
                String partition = RoutingPolicy.emptyPartition(label);
                int otherSlots = source.occupied() - source.slotCounts().getOrDefault(key, 0);
                boolean wrongShelfWithEmpty = layout.shouldUsePreferredEmpty(
                        snapshotIndex.get(source), label, emptyChests.stream()
                                .map(snapshotIndex::get).toList());
                if (!partition.isEmpty() &&
                        (RoutingPolicy.shouldClaimEmpty(
                                source.fit(key, label), otherSlots, false)
                                || wrongShelfWithEmpty)) {
                    int requestedSlots = Math.max(1,
                            (source.counts().getOrDefault(key, 1)
                                    + sample.getMaxStackSize() - 1)
                                    / Math.max(1, sample.getMaxStackSize()));
                    StorageSnapshot reserved = null;
                    for (StorageSnapshot candidate : reservedPartitions.getOrDefault(partition, List.of())) {
                        if (virtualFreeSlots.getOrDefault(candidate, 0) >= requestedSlots
                                && canReceiveSample(candidate.ref().handler(), sample)) {
                            reserved = candidate;
                            break;
                        }
                    }
                    if (reserved == null) {
                        int sourceIndex = snapshotIndex.get(source);
                        List<StorageSnapshot> orderedEmpties = emptyChests.stream()
                                .filter(candidate -> !reservedContainers.contains(candidate))
                                .sorted(Comparator
                                        .comparingDouble((StorageSnapshot candidate) ->
                                                layout.emptyPlacementScore(sourceIndex,
                                                        snapshotIndex.get(candidate), label)).reversed()
                                        .thenComparingDouble(candidate ->
                                                candidate.ref().pos().distSqr(source.ref().pos()))
                                        .thenComparingLong(candidate -> candidate.ref().pos().asLong()))
                                .toList();
                        for (StorageSnapshot candidate : orderedEmpties) {
                            if (candidate.ref().handler().getSlots() < requestedSlots
                                    || !canReceiveSample(candidate.ref().handler(), sample)) continue;
                            reserved = candidate;
                            reservedPartitions.computeIfAbsent(partition,
                                    ignored -> new ArrayList<>()).add(candidate);
                            virtualFreeSlots.put(candidate, candidate.ref().handler().getSlots());
                            reservedContainers.add(candidate);
                            break;
                        }
                    }
                    if (reserved != null && canReceiveSample(reserved.ref().handler(), sample)) {
                        virtualFreeSlots.put(reserved,
                                virtualFreeSlots.getOrDefault(reserved, 0) - requestedSlots);
                        best = reserved;
                        emptyAllocations.add(key);
                    }
                }
            }
            homes.put(key, best);
        }

        int movedItems = 0;
        int movedStacks = 0;
        int misses = 0;
        List<MoveRecord> visualTransfers = new ArrayList<>();
        Set<BlockPos> modified = new HashSet<>();
        MoveListener recordMove = record -> {
            modified.add(record.sourcePos());
            modified.add(record.targetPos());
            if (visualTransfers.size() < SorterSettings.MAX_MOVE_PARTICLE_TRACES
                    && !record.sourcePos().equals(record.targetPos())) {
                visualTransfers.add(record);
            }
        };

        boolean independentReorganization =
                categoryMode && settings.autoOrganize() && layout.analyzedBlocks() >= 3;
        if (!independentReorganization) {
        for (StorageSnapshot source : snapshots) {
            IItemHandler handler = source.ref().handler();
            for (int slot = 0; slot < handler.getSlots(); slot++) {
                ItemStack stack = handler.getStackInSlot(slot);
                if (stack.isEmpty()) continue;
                StackKey key = StackKey.of(stack);
                SortCategory cat = categories.get(key);
                if (cat == null || !cat.moveAllowed()) continue;

                StorageSnapshot home = homes.get(key);
                if (home == null || home == source) {
                    if (home == null) misses++;
                    continue;
                }
                double sourceFit = source.fit(key, cat.id());
                double targetFit = home.fit(key, cat.id());
                double sourceRank = source.rank(key, cat.id());
                double targetRank = home.rank(key, cat.id());
                int sourceCount = source.counts().getOrDefault(key, 0);
                int targetCount = home.counts().getOrDefault(key, 0);
                boolean exactHome = targetCount > 0;

                boolean shouldMove = RoutingPolicy.shouldRelocate(sourceFit, targetFit,
                        sourceRank, targetRank, exactHome);
                // Equal-quality chests with identical items must still compact to
                // one canonical destination; stable position breaks exact ties.
                if (!shouldMove && exactHome && Math.abs(targetRank - sourceRank) < 0.00001D
                        && targetFit + 0.07D >= sourceFit
                        && targetCount >= sourceCount) {
                    shouldMove = true;
                }
                // Position only resolves close semantic scores, never forces
                // blocks / ores / plants into a conflicting class of storage.
                if (!shouldMove && categoryMode && home.occupied() > 0
                        && layout.sameRegion(snapshotIndex.get(source), snapshotIndex.get(home))
                        && targetFit >= 0.78D && targetFit >= sourceFit - 0.015D
                        && targetRank >= sourceRank - 0.035D
                        && layout.preference(snapshotIndex.get(home), cat.id())
                           - layout.preference(snapshotIndex.get(source), cat.id()) >= 0.58D) {
                    shouldMove = true;
                }
                // Empty storage has zero contextual votes by definition.
                // Its assignment was explicitly planned from an incompatible
                // source, so don't reject the move by the usual score threshold.
                if (emptyAllocations.contains(key) && home.occupied() == 0) {
                    shouldMove = true;
                }
                if (!shouldMove) continue;

                int transferred = InventoryMover.moveSlotToTargets(
                        source.ref(), slot, List.of(home.ref()), recordMove);
                if (transferred > 0) {
                    movedItems += transferred;
                    movedStacks++;
                }
            }
        }

        }

        if (independentReorganization) {
            // Re-plan from the current inventories, ignoring inferred historical
            // chest roles. All actual moves obey Forge inventory simulation,
            // two-pass and operations caps; no forced swaps or item deletion.
            WarehouseReorganizer.Result reorganized =
                    WarehouseReorganizer.organize(containers, recordMove);
            movedItems += reorganized.movedItems();
            movedStacks += reorganized.movedOperations();
        }

        // Smart Categories ALWAYS includes partial-stack merging and filling
        // holes, even when there were no inter-chest transfers. A cheap
        // linear preflight skips chests already in compact form.
        // Match Existing retains its focused "changed chests only" behavior.
        for (StorageSnapshot snapshot : snapshots) {
            if (!categoryMode && !modified.contains(snapshot.ref().pos())) continue;
            if (!InventoryMover.needsCompaction(snapshot.ref())) continue;
            int packed = InventoryMover.cleanUpContainer(snapshot.ref(), recordMove);
            if (packed > 0) {
                movedItems += packed;
                movedStacks++;
            }
        }
        if (movedItems > 0) {
            SortingVisuals.playSortingEffects(level, modified, visualTransfers, settings);
        }
        return new SortingResult(containers.size(), movedItems, movedStacks,
                modified.size(), misses, radius, settings.mode().id());
    }

    private static StorageSnapshot snapshot(ContainerRef ref,
                                            Map<StackKey, SortCategory> resolved,
                                            Map<StackKey, ItemStack> samples) {
        Map<StackKey, Integer> counts = new LinkedHashMap<>();
        Map<StackKey, Integer> slotCounts = new HashMap<>();
        Map<String, Integer> labels = new HashMap<>();
        int occupied = 0;

        IItemHandler handler = ref.handler();
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack stack = handler.getStackInSlot(slot);
            if (stack.isEmpty()) continue;
            occupied++;
            StackKey key = StackKey.of(stack);
            counts.merge(key, stack.getCount(), Integer::sum);
            slotCounts.merge(key, 1, Integer::sum);
            samples.putIfAbsent(key, stack.copyWithCount(1));
            SortCategory category = resolved.computeIfAbsent(key, ignored -> ItemCategoryResolver.resolve(stack));
            if (category.moveAllowed() && !SortCategoryRegistry.MISC_ID.equals(category.id())) {
                labels.merge(category.id(), 1, Integer::sum);
            }
        }
        return new StorageSnapshot(ref, counts, slotCounts, labels, occupied);
    }

    private static StorageSnapshot chooseHome(StackKey key, String category, ItemStack sample,
                                             boolean allowCategoryFallback,
                                             List<StorageSnapshot> originalHomes,
                                             List<StorageSnapshot> compatibleHomes,
                                             WarehouseLayout layout,
                                             Map<StorageSnapshot, Integer> snapshotIndex) {
        if (originalHomes.isEmpty()) return null;

        Set<StorageSnapshot> candidates = new LinkedHashSet<>(originalHomes);
        if (allowCategoryFallback && RoutingPolicy.allowsCategoryFallback(category)) {
            candidates.addAll(compatibleHomes);
        }

        List<StorageSnapshot> ranked = new ArrayList<>();
        for (StorageSnapshot snapshot : candidates) {
            boolean existing = snapshot.counts().containsKey(key);
            double fit = snapshot.fit(key, category);
            if (!existing && (!allowCategoryFallback
                    || !RoutingPolicy.allowsCategoryFallback(category)
                    || fit < RoutingPolicy.MIN_NEW_HOME_FIT)) continue;
            ranked.add(snapshot);
        }
        if (ranked.isEmpty()) return originalHomes.get(0);

        StorageSnapshot sourceForLayout = originalHomes.get(0);
        int sourceIndex = snapshotIndex.get(sourceForLayout);
        ranked.sort(Comparator
                .comparingDouble((StorageSnapshot s) -> s.rank(key, category)
                        + (layout.sameRegion(sourceIndex, snapshotIndex.get(s))
                                ? 0.045D * layout.preference(snapshotIndex.get(s), category)
                                : 0D)).reversed()
                .thenComparing(Comparator.comparingInt(
                        (StorageSnapshot s) -> s.counts().getOrDefault(key, 0)).reversed())
                .thenComparingLong(s -> s.ref().pos().asLong()));

        // Check actual slot API only on promising candidates. TFC inventory rules,
        // varying maximum stack sizes and modded insertion restrictions apply.
        // Existing sources remain eligible as a fallback even if currently full.
        for (StorageSnapshot candidate : ranked) {
            if (canReceiveSample(candidate.ref().handler(), sample)) return candidate;
        }
        return ranked.stream()
                .filter(s -> s.counts().containsKey(key))
                .findFirst()
                .orElse(originalHomes.get(0));
    }

    private static boolean canReceiveSample(IItemHandler handler, ItemStack sample) {
        if (sample == null || sample.isEmpty()) return false;
        try {
            for (int slot = 0; slot < handler.getSlots(); slot++) {
                ItemStack other = handler.getStackInSlot(slot);
                if (!other.isEmpty() && (!ItemStack.isSameItemSameTags(other, sample)
                        || other.getCount() >= other.getMaxStackSize())) continue;
                ItemStack remaining = handler.insertItem(slot, sample, true);
                if (remaining.getCount() < sample.getCount()) return true;
            }
        } catch (RuntimeException ignored) {
            // Malformed third-party inventory must not abort the sort.
        }
        return false;
    }

    /** Tags are part of identity: distinct TFC spoilage and other NBT variants
     * are never treated as the same physical stack. */
    private record StackKey(Item item, CompoundTag tag) {
        static StackKey of(ItemStack stack) {
            CompoundTag tag = stack.getTag();
            return new StackKey(stack.getItem(), tag == null ? null : tag.copy());
        }
    }

    private record StorageSnapshot(ContainerRef ref,
                                   Map<StackKey, Integer> counts,
                                   Map<StackKey, Integer> slotCounts,
                                   Map<String, Integer> labels, int occupied) {
        double fit(StackKey key, String category) {
            return RoutingPolicy.contextualFit(labels, occupied, category,
                    category, slotCounts.getOrDefault(key, 0));
        }
        double rank(StackKey key, String category) {
            return RoutingPolicy.rank(fit(key, category), counts.getOrDefault(key, 0));
        }
    }
}
