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

        for (ContainerRef ref : containers) {
            snapshots.add(snapshot(ref, categories, samples));
        }
        snapshots.sort(Comparator.comparingLong(s -> s.ref().pos().asLong()));

        for (StorageSnapshot snapshot : snapshots) {
            for (StackKey key : snapshot.counts().keySet()) {
                byExactStack.computeIfAbsent(key, ignored -> new ArrayList<>()).add(snapshot);
            }
            for (String label : snapshot.labels().keySet()) {
                byCategory.computeIfAbsent(label, ignored -> new ArrayList<>()).add(snapshot);
            }
        }

        // Freeze all decisions BEFORE moving anything, so a newly filled chest
        // cannot change destination during the same sorting operation.
        Map<StackKey, StorageSnapshot> homes = new LinkedHashMap<>();
        for (StackKey key : samples.keySet()) {
            SortCategory resolved = categories.get(key);
            if (resolved == null || !resolved.moveAllowed()) continue;
            homes.put(key, chooseHome(key, resolved.id(), samples.get(key), categoryMode,
                    byExactStack.getOrDefault(key, List.of()), byCategory));
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
                if (!shouldMove) continue;

                int transferred = InventoryMover.moveSlotToTargets(
                        source.ref(), slot, List.of(home.ref()), recordMove);
                if (transferred > 0) {
                    movedItems += transferred;
                    movedStacks++;
                }
            }
        }

        // Clean only inventories that actually took part in a transfer.
        // The old implementation repacked ALL 160 chests at every click.
        if (movedItems > 0) {
            for (StorageSnapshot snapshot : snapshots) {
                if (!modified.contains(snapshot.ref().pos())) continue;
                int packed = InventoryMover.cleanUpContainer(snapshot.ref(), recordMove);
                if (packed > 0) {
                    movedItems += packed;
                    movedStacks++;
                }
            }
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
                                             Map<String, List<StorageSnapshot>> categoryIndex) {
        if (originalHomes.isEmpty()) return null;

        Set<StorageSnapshot> candidates = new LinkedHashSet<>(originalHomes);
        if (allowCategoryFallback && RoutingPolicy.allowsCategoryFallback(category)) {
            // Look up only containers with at least one COMPATIBLE label,
            // rather than rescanning all the block entities for every item.
            for (Map.Entry<String, List<StorageSnapshot>> indexed : categoryIndex.entrySet()) {
                if (RoutingPolicy.compatibility(category, indexed.getKey()) >= 0.50D) {
                    candidates.addAll(indexed.getValue());
                }
            }
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

        ranked.sort(Comparator
                .comparingDouble((StorageSnapshot s) -> s.rank(key, category)).reversed()
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
