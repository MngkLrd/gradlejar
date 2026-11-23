package com.kirill.simplesorter.sorting;

import com.kirill.simplesorter.config.SorterSettings;
import com.kirill.simplesorter.registry.ModItems;
import com.kirill.simplesorter.settings.SortingMode;
import com.kirill.simplesorter.settings.SortingWandSettings;
import com.kirill.simplesorter.visual.SortingVisuals;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import com.kirill.simplesorter.sorting.IItemHandler;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class SortingManager {
    private static final double STRONG_TARGET_CONFIDENCE = 0.60D;
    private static final int PLAYER_MAIN_INVENTORY_SLOTS = 36;

    private SortingManager() {}

    public static SortingResult sortAroundPlayer(ServerLevel level, Player player, SortingWandSettings settings) {
        SortingWandSettings safe = settings.sanitized();
        int radius = safe.radius();
        List<ContainerRef> containers = ContainerScanner.scanRadius(level, player, player.blockPosition(), radius);
        if (containers.isEmpty()) {
            return new SortingResult(0, 0, 0, 0, 0, radius, safe.mode().id());
        }

        return switch (safe.mode()) {
            case SMART_CATEGORIES -> sortSmartCategories(level, containers, radius, safe);
            case COMPACT_STACKS -> sortCompactStacks(level, containers, radius, safe);
            case PLAYER_DEPOSIT -> sortPlayerDeposit(level, player, containers, radius, safe);
            case MATCH_EXISTING -> sortMatchExisting(level, containers, radius, safe);
        };
    }

    private static SortingResult sortMatchExisting(ServerLevel level, List<ContainerRef> containers, int radius, SortingWandSettings settings) {
        int movedItems = 0;
        int movedStacks = 0;
        int noTargetStacks = 0;
        List<MoveRecord> moves = new ArrayList<>();
        Set<BlockPos> changedContainers = new HashSet<>();
        MoveListener moveListener = trackingListener(moves, changedContainers);

        for (ContainerRef source : containers) {
            IItemHandler sourceHandler = source.handler();
            for (int slot = 0; slot < sourceHandler.getSlots(); slot++) {
                ItemStack stack = sourceHandler.getStackInSlot(slot);
                if (!canMove(stack)) {
                    continue;
                }

                List<ContainerRef> destinations = containersContainingExactStack(containers, stack, source);
                if (destinations.isEmpty()) {
                    noTargetStacks++;
                    continue;
                }

                int moved = InventoryMover.moveSlotToTargets(source, slot, destinations, moveListener);
                if (moved > 0) {
                    movedItems += moved;
                    movedStacks++;
                }
            }
        }

        int cleanedItems = cleanUpContainers(containers, moveListener);
        if (cleanedItems > 0) {
            movedItems += cleanedItems;
            movedStacks++;
        }

        playEffects(level, changedContainers, moves, settings, movedItems);
        return new SortingResult(containers.size(), movedItems, movedStacks, countNonEmptyContainers(changedContainers), noTargetStacks, radius, settings.mode().id());
    }

    private static SortingResult sortCompactStacks(ServerLevel level, List<ContainerRef> containers, int radius, SortingWandSettings settings) {
        int movedItems = 0;
        int movedStacks = 0;
        int noTargetStacks = 0;
        List<MoveRecord> moves = new ArrayList<>();
        Set<BlockPos> changedContainers = new HashSet<>();
        MoveListener moveListener = trackingListener(moves, changedContainers);

        for (ContainerRef source : containers) {
            IItemHandler sourceHandler = source.handler();
            for (int slot = 0; slot < sourceHandler.getSlots(); slot++) {
                ItemStack stack = sourceHandler.getStackInSlot(slot);
                if (!canMove(stack)) {
                    continue;
                }

                List<ContainerRef> destinations = containersContainingPartialExactStack(containers, stack, source);
                if (destinations.isEmpty()) {
                    noTargetStacks++;
                    continue;
                }

                int moved = InventoryMover.mergeSlotIntoExistingStacks(source, slot, destinations, moveListener);
                if (moved > 0) {
                    movedItems += moved;
                    movedStacks++;
                }
            }
        }

        int cleanedItems = cleanUpContainers(containers, moveListener);
        if (cleanedItems > 0) {
            movedItems += cleanedItems;
            movedStacks++;
        }

        playEffects(level, changedContainers, moves, settings, movedItems);
        return new SortingResult(containers.size(), movedItems, movedStacks, countNonEmptyContainers(changedContainers), noTargetStacks, radius, settings.mode().id());
    }

    private static SortingResult sortPlayerDeposit(ServerLevel level, Player player, List<ContainerRef> containers, int radius, SortingWandSettings settings) {
        int movedItems = 0;
        int movedStacks = 0;
        int noTargetStacks = 0;
        List<MoveRecord> moves = new ArrayList<>();
        Set<BlockPos> changedContainers = new HashSet<>();
        MoveListener moveListener = trackingListener(moves, changedContainers);
        Inventory inventory = player.getInventory();
        int maxSlot = Math.min(PLAYER_MAIN_INVENTORY_SLOTS, inventory.getContainerSize());

        for (int slot = 0; slot < maxSlot; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!canMove(stack) || stack.getItem() == ModItems.SORTING_WAND.get()) {
                continue;
            }

            List<ContainerRef> destinations = containersContainingExactStack(containers, stack, null);
            if (destinations.isEmpty()) {
                noTargetStacks++;
                continue;
            }

            int moved = InventoryMover.movePlayerSlotToTargets(player, slot, destinations, moveListener);
            if (moved > 0) {
                movedItems += moved;
                movedStacks++;
            }
        }

        playEffects(level, changedContainers, moves, settings, movedItems);
        return new SortingResult(containers.size(), movedItems, movedStacks, countNonEmptyContainers(changedContainers), noTargetStacks, radius, settings.mode().id());
    }

    private static SortingResult sortSmartCategories(ServerLevel level, List<ContainerRef> containers, int radius, SortingWandSettings settings) {
        return SmartCategorySorter.sort(level, containers, radius, settings);
    }

    private static int cleanUpContainers(List<ContainerRef> containers, MoveListener moveListener) {
        int moved = 0;
        for (ContainerRef container : containers) {
            moved += InventoryMover.cleanUpContainer(container, moveListener);
        }
        return moved;
    }

    private static MoveListener trackingListener(List<MoveRecord> moves, Set<BlockPos> changedContainers) {
        return record -> {
            if (moves.size() < SorterSettings.MAX_MOVE_PARTICLE_TRACES) {
                moves.add(record);
            }
            changedContainers.add(record.sourcePos());
            changedContainers.add(record.targetPos());
        };
    }

    private static void playEffects(ServerLevel level, Set<BlockPos> changedContainers, List<MoveRecord> moves, SortingWandSettings settings, int movedItems) {
        if (movedItems > 0) {
            SortingVisuals.playSortingEffects(level, changedContainers, moves, settings);
        }
    }

    private static boolean canMove(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        return ItemCategoryResolver.resolve(stack).moveAllowed();
    }

    private static List<ContainerRef> containersContainingExactStack(List<ContainerRef> containers, ItemStack stack, ContainerRef source) {
        List<ContainerRef> result = new ArrayList<>();
        for (ContainerRef ref : containers) {
            if (source != null && sameContainer(ref, source)) {
                continue;
            }
            if (containsExactStack(ref.handler(), stack)) {
                result.add(ref);
            }
        }

        result.sort(Comparator
                .comparingInt((ContainerRef ref) -> countExactStack(ref.handler(), stack)).reversed()
                .thenComparingInt(ref -> ref.pos().getX())
                .thenComparingInt(ref -> ref.pos().getY())
                .thenComparingInt(ref -> ref.pos().getZ()));
        return result;
    }

    private static List<ContainerRef> containersContainingPartialExactStack(List<ContainerRef> containers, ItemStack stack, ContainerRef source) {
        List<ContainerRef> result = new ArrayList<>();
        for (ContainerRef ref : containers) {
            if (source != null && sameContainer(ref, source)) {
                continue;
            }
            if (containsPartialExactStack(ref.handler(), stack)) {
                result.add(ref);
            }
        }

        result.sort(Comparator
                .comparingInt((ContainerRef ref) -> countPartialExactStack(ref.handler(), stack)).reversed()
                .thenComparingInt(ref -> ref.pos().getX())
                .thenComparingInt(ref -> ref.pos().getY())
                .thenComparingInt(ref -> ref.pos().getZ()));
        return result;
    }

    private static boolean containsExactStack(IItemHandler handler, ItemStack stack) {
        return countExactStack(handler, stack) > 0;
    }

    private static int countExactStack(IItemHandler handler, ItemStack stack) {
        int count = 0;
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack other = handler.getStackInSlot(slot);
            if (!other.isEmpty() && ItemStack.isSameItemSameTags(other, stack)) {
                count += other.getCount();
            }
        }
        return count;
    }

    private static boolean containsPartialExactStack(IItemHandler handler, ItemStack stack) {
        return countPartialExactStack(handler, stack) > 0;
    }

    private static int countPartialExactStack(IItemHandler handler, ItemStack stack) {
        int count = 0;
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack other = handler.getStackInSlot(slot);
            if (!other.isEmpty() && other.getCount() < other.getMaxStackSize() && ItemStack.isSameItemSameTags(other, stack)) {
                count += other.getMaxStackSize() - other.getCount();
            }
        }
        return count;
    }

    private static int countNonEmptyContainers(Set<BlockPos> changedContainers) {
        return changedContainers == null ? 0 : changedContainers.size();
    }

    private static Map<ContainerRef, ChestProfile> analyzeContainers(List<ContainerRef> containers) {
        Map<ContainerRef, ChestProfile> result = new HashMap<>();

        for (ContainerRef ref : containers) {
            result.put(ref, analyzeContainer(ref.handler()));
        }

        return result;
    }

    private static ChestProfile analyzeContainer(IItemHandler container) {
        Map<String, Integer> scores = new HashMap<>();
        int totalScore = 0;
        int occupiedSlots = 0;

        for (int slot = 0; slot < container.getSlots(); slot++) {
            ItemStack stack = container.getStackInSlot(slot);
            if (stack.isEmpty()) {
                continue;
            }

            occupiedSlots++;
            SortCategory category = ItemCategoryResolver.resolve(stack);
            if (!category.moveAllowed()) {
                continue;
            }

            int weight = category.weight(stack.getCount());
            scores.merge(category.id(), weight, Integer::sum);
            totalScore += weight;
        }

        if (occupiedSlots == 0 || totalScore <= 0) {
            return ChestProfile.empty();
        }

        return new ChestProfile(scores, totalScore, occupiedSlots);
    }

    private static Map<ContainerRef, String> assignRoles(List<ContainerRef> containers, Map<ContainerRef, ChestProfile> profiles) {
        Map<ContainerRef, String> roles = new HashMap<>();
        Map<String, Integer> globalScores = buildGlobalScores(profiles);

        List<String> desiredCategories = globalScores.entrySet().stream()
                .filter(entry -> entry.getValue() > 0)
                .sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder()))
                .map(Map.Entry::getKey)
                .toList();

        if (desiredCategories.isEmpty()) {
            for (ContainerRef ref : containers) {
                roles.put(ref, SortCategoryRegistry.MISC_ID);
            }
            return roles;
        }

        List<ContainerRef> emptyContainers = new ArrayList<>();

        for (ContainerRef ref : containers) {
            ChestProfile profile = profiles.get(ref);
            if (profile.isEmpty()) {
                emptyContainers.add(ref);
                continue;
            }

            String primary = profile.primaryCategoryId().orElse(SortCategoryRegistry.MISC_ID);
            roles.put(ref, primary);
        }

        // Do not take ownership of intentionally empty storage or change existing chest roles.
        // Players can seed a category by placing one matching stack into a chest.

        return roles;
    }

    private static void assignEmptyContainers(List<ContainerRef> emptyContainers, List<String> desiredCategories, Map<ContainerRef, String> roles) {
        if (emptyContainers.isEmpty()) {
            return;
        }

        int index = 0;

        for (String category : desiredCategories) {
            if (index >= emptyContainers.size()) {
                return;
            }

            roles.put(emptyContainers.get(index), category);
            index++;
        }

        int categoryIndex = 0;
        while (index < emptyContainers.size()) {
            String category = desiredCategories.get(categoryIndex % desiredCategories.size());
            roles.put(emptyContainers.get(index), category);
            index++;
            categoryIndex++;
        }
    }

    private static void coverMissingDesiredCategories(List<ContainerRef> containers, Map<ContainerRef, ChestProfile> profiles, Map<ContainerRef, String> roles, List<String> desiredCategories) {
        int maxRoles = Math.min(containers.size(), desiredCategories.size());
        for (int i = 0; i < maxRoles; i++) {
            String desired = desiredCategories.get(i);
            if (roles.containsValue(desired)) {
                continue;
            }

            Optional<ContainerRef> candidate = findReassignCandidate(desired, profiles, roles);
            candidate.ifPresent(ref -> roles.put(ref, desired));
        }
    }

    private static Optional<ContainerRef> findReassignCandidate(String desiredCategory, Map<ContainerRef, ChestProfile> profiles, Map<ContainerRef, String> roles) {
        Map<String, Integer> roleCounts = new HashMap<>();
        roles.values().forEach(role -> roleCounts.merge(role, 1, Integer::sum));

        return roles.keySet().stream()
                .filter(ref -> roleCounts.getOrDefault(roles.get(ref), 0) > 1)
                .filter(ref -> profiles.get(ref).scoreFor(desiredCategory) > 0 || profiles.get(ref).confidence() < STRONG_TARGET_CONFIDENCE)
                .max(Comparator
                        .comparingInt((ContainerRef ref) -> profiles.get(ref).scoreFor(desiredCategory))
                        .thenComparing(ref -> -profiles.get(ref).scoreFor(roles.get(ref)))
                        .thenComparing(ref -> -profiles.get(ref).occupiedSlots()));
    }

    private static Map<String, Integer> buildGlobalScores(Map<ContainerRef, ChestProfile> profiles) {
        Map<String, Integer> result = new HashMap<>();

        for (ChestProfile profile : profiles.values()) {
            for (Map.Entry<String, Integer> entry : profile.copyScores().entrySet()) {
                result.merge(entry.getKey(), entry.getValue(), Integer::sum);
            }
        }

        return result;
    }

    private static Map<String, List<ContainerRef>> buildTargetMap(Map<ContainerRef, String> roles, Map<ContainerRef, ChestProfile> profiles) {
        Map<String, List<ContainerRef>> result = new LinkedHashMap<>();

        for (Map.Entry<ContainerRef, String> entry : roles.entrySet()) {
            result.computeIfAbsent(entry.getValue(), ignored -> new ArrayList<>()).add(entry.getKey());
        }

        for (Map.Entry<String, List<ContainerRef>> entry : result.entrySet()) {
            String category = entry.getKey();
            entry.getValue().sort(targetComparator(category, profiles));
        }

        return result;
    }

    private static Comparator<ContainerRef> targetComparator(String category, Map<ContainerRef, ChestProfile> profiles) {
        return Comparator
                .comparingInt((ContainerRef ref) -> targetPriority(category, profiles.get(ref)))
                .thenComparing((ContainerRef ref) -> profiles.get(ref).scoreFor(category), Comparator.reverseOrder())
                .thenComparing((ContainerRef ref) -> profiles.get(ref).occupiedSlots(), Comparator.reverseOrder())
                .thenComparingInt(ref -> countFreeSlots(ref.handler()))
                .thenComparingInt(ref -> ref.pos().getX())
                .thenComparingInt(ref -> ref.pos().getY())
                .thenComparingInt(ref -> ref.pos().getZ());
    }

    private static int targetPriority(String category, ChestProfile profile) {
        if (profile == null || profile.isEmpty()) {
            return 1;
        }

        Optional<String> primary = profile.primaryCategoryId();
        if (primary.isPresent() && primary.get().equals(category) && profile.confidence() >= STRONG_TARGET_CONFIDENCE) {
            return 0;
        }

        if (primary.isPresent() && primary.get().equals(category)) {
            return 2;
        }

        if (profile.scoreFor(category) > 0) {
            return 3;
        }

        return 4;
    }

    private static List<ContainerRef> selectDestinations(ContainerRef source, List<ContainerRef> orderedTargets) {
        if (orderedTargets == null || orderedTargets.isEmpty()) {
            return List.of();
        }

        int sourceIndex = indexOfSameContainer(orderedTargets, source);
        if (sourceIndex == 0) {
            return List.of();
        }

        List<ContainerRef> result = new ArrayList<>();
        int limit = sourceIndex > 0 ? sourceIndex : orderedTargets.size();

        for (int i = 0; i < limit; i++) {
            ContainerRef target = orderedTargets.get(i);
            if (!sameContainer(target, source)) {
                result.add(target);
            }
        }

        return result;
    }

    private static int indexOfSameContainer(List<ContainerRef> refs, ContainerRef source) {
        for (int i = 0; i < refs.size(); i++) {
            if (sameContainer(refs.get(i), source)) {
                return i;
            }
        }
        return -1;
    }

    private static boolean sameContainer(ContainerRef a, ContainerRef b) {
        return a == b || a.identity() == b.identity() || a.handler() == b.handler() || a.pos().equals(b.pos());
    }

    private static int countFreeSlots(IItemHandler container) {
        int free = 0;
        for (int slot = 0; slot < container.getSlots(); slot++) {
            if (container.getStackInSlot(slot).isEmpty()) {
                free++;
            }
        }
        return free;
    }

    private static int countUsefulRoles(Map<String, List<ContainerRef>> targets) {
        Set<String> useful = new HashSet<>();
        for (Map.Entry<String, List<ContainerRef>> entry : targets.entrySet()) {
            if (!entry.getKey().equals(SortCategoryRegistry.MISC_ID) && !entry.getValue().isEmpty()) {
                useful.add(entry.getKey());
            }
        }
        return useful.size();
    }
}
