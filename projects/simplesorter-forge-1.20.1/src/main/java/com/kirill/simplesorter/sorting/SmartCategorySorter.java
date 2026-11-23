package com.kirill.simplesorter.sorting;

import com.kirill.simplesorter.config.SorterSettings;
import com.kirill.simplesorter.settings.SortingWandSettings;
import com.kirill.simplesorter.visual.SortingVisuals;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Smart Categories 2: one immutable routing decision per item type, per action.
 * No opportunistic reclassification after moving an item. This prevents
 * oscillating between equally ranked chests and preserves player-seeded homes.
 */
public final class SmartCategorySorter {
    private static final double CATEGORY_TARGET_PURITY = 0.68D;
    private static final double SOURCE_ALREADY_SORTED_PURITY = 0.68D;

    private SmartCategorySorter() {}

    public static SortingResult sort(ServerLevel level, List<ContainerRef> containers,
                                     int radius, SortingWandSettings settings) {
        boolean allowCategoryFallback = settings.mode() == com.kirill.simplesorter.settings.SortingMode.SMART_CATEGORIES;
        List<StorageSnapshot> snapshots = new ArrayList<>();
        Map<Item, SortCategory> categoryCache = new IdentityHashMap<>();

        for (ContainerRef ref : containers) {
            snapshots.add(snapshot(ref, categoryCache));
        }
        // A stable order, independent of HashMap iteration or BE discovery.
        snapshots.sort(Comparator.comparingLong(s -> s.ref().pos().asLong()));

        Map<Item, StorageSnapshot> homes = new IdentityHashMap<>();
        for (StorageSnapshot snapshot : snapshots) {
            for (Item item : snapshot.items().keySet()) {
                homes.computeIfAbsent(item, key -> selectHome(key, snapshots, categoryCache, allowCategoryFallback));
            }
        }

        int movedItems = 0;
        int movedStacks = 0;
        int unassigned = 0;
        List<MoveRecord> animations = new ArrayList<>();
        Set<BlockPos> changed = new HashSet<>();
        MoveListener observer = record -> {
            changed.add(record.sourcePos());
            changed.add(record.targetPos());
            if (animations.size() < SorterSettings.MAX_MOVE_PARTICLE_TRACES) {
                animations.add(record);
            }
        };

        for (StorageSnapshot source : snapshots) {
            IItemHandler handler = source.ref().handler();
            for (int slot = 0; slot < handler.getSlots(); slot++) {
                ItemStack stack = handler.getStackInSlot(slot);
                if (stack.isEmpty() || !category(stack, categoryCache).moveAllowed()) {
                    continue;
                }
                StorageSnapshot home = homes.get(stack.getItem());
                if (home == null || home == source) {
                    if (home == null) unassigned++;
                    continue;
                }
                // The destination was decided from the initial state; it cannot
                // turn into a newly discovered "category" midway through sorting.
                int moved = InventoryMover.moveSlotToTargets(
                        source.ref(), slot, List.of(home.ref()), observer);
                if (moved > 0) {
                    movedItems += moved;
                    movedStacks++;
                }
            }
        }

        for (StorageSnapshot snapshot : snapshots) {
            int packed = InventoryMover.cleanUpContainer(snapshot.ref(), observer);
            if (packed > 0) {
                movedItems += packed;
                movedStacks++;
            }
        }

        if (movedItems > 0) {
            SortingVisuals.playSortingEffects(level, changed, animations, settings);
        }
        return new SortingResult(containers.size(), movedItems, movedStacks,
                changed.size(), unassigned, radius, settings.mode().id());
    }

    private static StorageSnapshot snapshot(ContainerRef ref, Map<Item, SortCategory> cache) {
        Map<Item, Integer> items = new IdentityHashMap<>();
        Map<Item, ItemStack> samples = new IdentityHashMap<>();
        Map<String, Integer> categories = new HashMap<>();
        int occupied = 0;
        IItemHandler handler = ref.handler();
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack stack = handler.getStackInSlot(slot);
            if (stack.isEmpty()) continue;
            occupied++;
            items.merge(stack.getItem(), stack.getCount(), Integer::sum);
            samples.putIfAbsent(stack.getItem(), stack.copyWithCount(1));
            SortCategory cat = category(stack, cache);
            if (cat.moveAllowed() && !SortCategoryRegistry.MISC_ID.equals(cat.id())) {
                // Slots, not stack size: a 64-stack must not drown out a whole
                // category of smaller modded stacks and their differing max sizes.
                categories.merge(cat.id(), 1, Integer::sum);
            }
        }
        return new StorageSnapshot(ref, items, samples, categories, occupied);
    }

    private static SortCategory category(ItemStack stack, Map<Item, SortCategory> cache) {
        return cache.computeIfAbsent(stack.getItem(), ignored -> ItemCategoryResolver.resolve(stack));
    }

    private static StorageSnapshot selectHome(Item item, List<StorageSnapshot> snapshots,
                                               Map<Item, SortCategory> categories, boolean allowCategoryFallback) {
        SortCategory cat = categories.get(item);
        if (cat == null || !cat.moveAllowed()) return null;
        String categoryId = cat.id();

        List<StorageSnapshot> originals = new ArrayList<>();
        for (StorageSnapshot s : snapshots) {
            if (s.items().containsKey(item)) originals.add(s);
        }
        if (originals.isEmpty()) return null;
        ItemStack sample = originals.get(0).samples().get(item);

        // If the player has seeded the item in multiple chests, consolidate to
        // ONE consistent home. Pure category context wins before stack quantity.
        if (originals.size() >= 2) {
            return originals.stream()
                    .max(Comparator
                            .comparingDouble((StorageSnapshot s) -> s.affinity(categoryId))
                            .thenComparingInt(s -> s.items().getOrDefault(item, 0))
                            .thenComparingLong(s -> -s.ref().pos().asLong()))
                    .orElse(originals.get(0));
        }

        StorageSnapshot origin = originals.get(0);
        if (!allowCategoryFallback) return origin;
        // Unknown categories must never be guessed into random misc chests.
        if (!isSafeFallbackCategory(categoryId)) return origin;
        // The already dominant chest is the canonical home unless the player
        // deliberately seeded the item in a second chest (handled above).
        if (origin.purity(categoryId) >= SOURCE_ALREADY_SORTED_PURITY) return origin;

        return snapshots.stream()
                .filter(s -> s != origin && s.occupied() > 0)
                .filter(s -> s.purity(categoryId) >= CATEGORY_TARGET_PURITY)
                .filter(s -> s.categories().getOrDefault(categoryId, 0) > 0)
                .filter(s -> canReceiveSample(s.ref().handler(), sample))
                .max(Comparator
                        .comparingDouble((StorageSnapshot s) -> s.purity(categoryId))
                        .thenComparingInt(s -> s.categories().getOrDefault(categoryId, 0))
                        .thenComparingLong(s -> -s.ref().pos().asLong()))
                .orElse(origin);
    }

    /**
     * Candidate destinations are checked through the actual insertion API.
     * A modded inventory's slot rules (TFC size, sided slots etc.) are final.
     * Item type is sufficient for routing, but NBT is never stripped on moves.
     */
    private static boolean isSafeFallbackCategory(String id) {
        return switch (id) {
            case "tiny_dust", "small_dust", "dusts", "ores", "raw_ores",
                    "ingots", "nuggets", "metal_sheets", "plates", "rods",
                    "wires", "gears", "gems", "adhesives", "fruit_tree_parts",
                    "molds", "loose_stones", "tools", "weapons", "armor",
                    "potions", "books", "food", "plants", "wood", "redstone" -> true;
            default -> false; // broad or unknown: exact-match only
        };
    }

    private static boolean canReceiveSample(IItemHandler handler, ItemStack sample) {
        if (sample == null || sample.isEmpty()) return false;
        try {
            for (int i = 0; i < handler.getSlots(); i++) {
                ItemStack existing = handler.getStackInSlot(i);
                if (existing.isEmpty() ||
                        (ItemStack.isSameItemSameTags(existing, sample)
                                && existing.getCount() < existing.getMaxStackSize())) {
                    ItemStack remainder = handler.insertItem(i, sample, true);
                    if (remainder.getCount() < sample.getCount()) return true;
                }
            }
        } catch (RuntimeException ignored) {
            // Invalid capability: do not target it.
        }
        return false;
    }

    private record StorageSnapshot(ContainerRef ref, Map<Item, Integer> items,
                                   Map<Item, ItemStack> samples, Map<String, Integer> categories,
                                   int occupied) {
        double affinity(String categoryId) {
            if (occupied == 0) return 0D;
            double own = purity(categoryId);
            String family = family(categoryId);
            int related = 0;
            for (var entry : categories.entrySet()) {
                if (family.equals(family(entry.getKey()))) related += entry.getValue();
            }
            return own * 2.0D + (double) related / occupied;
        }

        private static String family(String id) {
            return switch (id) {
                case "ores", "raw_ores", "tiny_dust", "small_dust", "dusts", "gems", "loose_stones" -> "raw_minerals";
                case "ingots", "nuggets", "metal_sheets", "plates" -> "metal_forms";
                case "rods", "wires", "gears" -> "machine_components";
                default -> id;
            };
        }

        double purity(String categoryId) {
            if (occupied <= 0 || SortCategoryRegistry.MISC_ID.equals(categoryId)) return 0D;
            return (double) categories.getOrDefault(categoryId, 0) / (double) occupied;
        }
    }
}
