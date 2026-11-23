package com.kirill.simplesorter.sorting;

import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import com.kirill.simplesorter.sorting.IItemHandler;

import java.util.List;

public final class InventoryMover {
    private static final int PLAYER_MAIN_INVENTORY_SLOTS = 36;

    private InventoryMover() {}

    public static int moveSlotToTargets(ContainerRef source, int sourceSlot, List<ContainerRef> targets, MoveListener moveListener) {
        if (targets == null || targets.isEmpty()) {
            return 0;
        }

        IItemHandler sourceHandler = source.handler();
        ItemStack sourceStack = sourceHandler.getStackInSlot(sourceSlot);
        if (sourceStack.isEmpty()) {
            return 0;
        }

        int moved = 0;

        for (ContainerRef target : targets) {
            if (sameInventory(source, target)) {
                continue;
            }
            moved += insertIntoExistingStacks(source, sourceSlot, target, moveListener);
            if (sourceHandler.getStackInSlot(sourceSlot).isEmpty()) {
                return moved;
            }
        }

        for (ContainerRef target : targets) {
            if (sameInventory(source, target)) {
                continue;
            }
            moved += insertIntoEmptySlots(source, sourceSlot, target, moveListener);
            if (sourceHandler.getStackInSlot(sourceSlot).isEmpty()) {
                return moved;
            }
        }

        return moved;
    }

    public static int mergeSlotIntoExistingStacks(ContainerRef source, int sourceSlot, List<ContainerRef> targets, MoveListener moveListener) {
        if (targets == null || targets.isEmpty()) {
            return 0;
        }

        IItemHandler sourceHandler = source.handler();
        if (sourceHandler.getStackInSlot(sourceSlot).isEmpty()) {
            return 0;
        }

        int moved = 0;
        for (ContainerRef target : targets) {
            if (sameInventory(source, target)) {
                continue;
            }
            moved += insertIntoExistingStacks(source, sourceSlot, target, moveListener);
            if (sourceHandler.getStackInSlot(sourceSlot).isEmpty()) {
                return moved;
            }
        }
        return moved;
    }

    public static int movePlayerSlotToTargets(Player player, int playerSlot, List<ContainerRef> targets, MoveListener moveListener) {
        if (player == null || targets == null || targets.isEmpty()) {
            return 0;
        }

        Inventory inventory = player.getInventory();
        if (playerSlot < 0 || playerSlot >= Math.min(PLAYER_MAIN_INVENTORY_SLOTS, inventory.getContainerSize())) {
            return 0;
        }

        ItemStack sourceStack = inventory.getItem(playerSlot);
        if (sourceStack.isEmpty()) {
            return 0;
        }

        int moved = 0;

        for (ContainerRef target : targets) {
            moved += insertPlayerSlotIntoExistingStacks(player, playerSlot, target, moveListener);
            if (inventory.getItem(playerSlot).isEmpty()) {
                return moved;
            }
        }

        for (ContainerRef target : targets) {
            moved += insertPlayerSlotIntoEmptySlots(player, playerSlot, target, moveListener);
            if (inventory.getItem(playerSlot).isEmpty()) {
                return moved;
            }
        }

        return moved;
    }

    private static int insertIntoExistingStacks(ContainerRef sourceRef, int sourceSlot, ContainerRef targetRef, MoveListener moveListener) {
        IItemHandler source = sourceRef.handler();
        IItemHandler target = targetRef.handler();
        int moved = 0;

        for (int targetSlot = 0; targetSlot < target.getSlots(); targetSlot++) {
            ItemStack currentSource = source.getStackInSlot(sourceSlot);
            if (currentSource.isEmpty()) {
                break;
            }

            ItemStack targetStack = target.getStackInSlot(targetSlot);
            if (!isCleanUpMovable(targetStack) || targetStack.getCount() >= targetStack.getMaxStackSize()) {
                continue;
            }

            if (!ItemStack.isSameItemSameTags(targetStack, currentSource)) {
                continue;
            }

            moved += moveIntoSlot(sourceRef, sourceSlot, targetRef, targetSlot, currentSource.getCount(), moveListener);
        }

        return moved;
    }

    private static int insertIntoEmptySlots(ContainerRef sourceRef, int sourceSlot, ContainerRef targetRef, MoveListener moveListener) {
        IItemHandler source = sourceRef.handler();
        IItemHandler target = targetRef.handler();
        int moved = 0;

        for (int targetSlot = 0; targetSlot < target.getSlots(); targetSlot++) {
            ItemStack currentSource = source.getStackInSlot(sourceSlot);
            if (currentSource.isEmpty()) {
                break;
            }

            ItemStack targetStack = target.getStackInSlot(targetSlot);
            if (!targetStack.isEmpty()) {
                continue;
            }

            moved += moveIntoSlot(sourceRef, sourceSlot, targetRef, targetSlot, currentSource.getCount(), moveListener);
        }

        return moved;
    }

    private static int moveIntoSlot(ContainerRef sourceRef, int sourceSlot, ContainerRef targetRef, int targetSlot, int requestedAmount, MoveListener moveListener) {
        IItemHandler source = sourceRef.handler();
        IItemHandler target = targetRef.handler();

        ItemStack simulatedExtract = source.extractItem(sourceSlot, requestedAmount, true);
        if (simulatedExtract.isEmpty()) {
            return 0;
        }

        ItemStack simulatedRemainder = target.insertItem(targetSlot, simulatedExtract, true);
        int accepted = simulatedExtract.getCount() - simulatedRemainder.getCount();
        if (accepted <= 0) {
            return 0;
        }

        ItemStack actuallyExtracted = source.extractItem(sourceSlot, accepted, false);
        if (actuallyExtracted.isEmpty()) {
            return 0;
        }

        ItemStack visualStack = actuallyExtracted.copy();
        visualStack.setCount(1);

        ItemStack actualRemainder = target.insertItem(targetSlot, actuallyExtracted, false);
        int actuallyMoved = actuallyExtracted.getCount() - actualRemainder.getCount();

        if (!actualRemainder.isEmpty()) {
            ItemStack leftover = returnRemainderToSource(source, sourceSlot, actualRemainder);
            if (!leftover.isEmpty()) {
                // Failing modded containers can reject a real transfer after accepting its simulation.
                // Never discard the remainder: return it to the world at the source container.
                Containers.dropItemStack(sourceRef.level(), sourceRef.pos().getX() + 0.5D,
                        sourceRef.pos().getY() + 0.7D, sourceRef.pos().getZ() + 0.5D, leftover);
            }
        }

        if (actuallyMoved > 0 && moveListener != null) {
            moveListener.onMove(new MoveRecord(sourceRef.pos(), targetRef.pos(), actuallyMoved, visualStack));
        }

        return Math.max(0, actuallyMoved);
    }

    private static int insertPlayerSlotIntoExistingStacks(Player player, int playerSlot, ContainerRef targetRef, MoveListener moveListener) {
        Inventory inventory = player.getInventory();
        IItemHandler target = targetRef.handler();
        int moved = 0;

        for (int targetSlot = 0; targetSlot < target.getSlots(); targetSlot++) {
            ItemStack currentSource = inventory.getItem(playerSlot);
            if (currentSource.isEmpty()) {
                break;
            }

            ItemStack targetStack = target.getStackInSlot(targetSlot);
            if (!isCleanUpMovable(targetStack) || targetStack.getCount() >= targetStack.getMaxStackSize()) {
                continue;
            }

            if (!ItemStack.isSameItemSameTags(targetStack, currentSource)) {
                continue;
            }

            moved += movePlayerIntoSlot(player, playerSlot, targetRef, targetSlot, currentSource.getCount(), moveListener);
        }

        return moved;
    }

    private static int insertPlayerSlotIntoEmptySlots(Player player, int playerSlot, ContainerRef targetRef, MoveListener moveListener) {
        Inventory inventory = player.getInventory();
        IItemHandler target = targetRef.handler();
        int moved = 0;

        for (int targetSlot = 0; targetSlot < target.getSlots(); targetSlot++) {
            ItemStack currentSource = inventory.getItem(playerSlot);
            if (currentSource.isEmpty()) {
                break;
            }

            if (!target.getStackInSlot(targetSlot).isEmpty()) {
                continue;
            }

            moved += movePlayerIntoSlot(player, playerSlot, targetRef, targetSlot, currentSource.getCount(), moveListener);
        }

        return moved;
    }

    private static int movePlayerIntoSlot(Player player, int playerSlot, ContainerRef targetRef, int targetSlot, int requestedAmount, MoveListener moveListener) {
        Inventory inventory = player.getInventory();
        IItemHandler target = targetRef.handler();

        ItemStack sourceStack = inventory.getItem(playerSlot);
        if (sourceStack.isEmpty()) {
            return 0;
        }

        ItemStack simulatedExtract = sourceStack.copy();
        simulatedExtract.setCount(Math.min(requestedAmount, sourceStack.getCount()));
        ItemStack simulatedRemainder = target.insertItem(targetSlot, simulatedExtract, true);
        int accepted = simulatedExtract.getCount() - simulatedRemainder.getCount();
        if (accepted <= 0) {
            return 0;
        }

        ItemStack extracted = inventory.removeItem(playerSlot, accepted);
        if (extracted.isEmpty()) {
            return 0;
        }

        ItemStack visualStack = extracted.copy();
        visualStack.setCount(1);

        ItemStack actualRemainder = target.insertItem(targetSlot, extracted, false);
        int actuallyMoved = extracted.getCount() - actualRemainder.getCount();

        if (!actualRemainder.isEmpty()) {
            returnPlayerRemainder(player, playerSlot, actualRemainder);
        }

        if (actuallyMoved > 0 && moveListener != null) {
            moveListener.onMove(new MoveRecord(player.blockPosition(), targetRef.pos(), actuallyMoved, visualStack));
        }

        return Math.max(0, actuallyMoved);
    }

    public static int cleanUpContainer(ContainerRef containerRef, MoveListener moveListener) {
        if (containerRef == null || containerRef.handler() == null) {
            return 0;
        }

        int moved = 0;
        IItemHandler handler = containerRef.handler();

        for (int sourceSlot = handler.getSlots() - 1; sourceSlot >= 0; sourceSlot--) {
            moved += mergeSlotWithinSameContainer(containerRef, sourceSlot, moveListener);
        }

        for (int targetSlot = 0; targetSlot < handler.getSlots(); targetSlot++) {
            if (!handler.getStackInSlot(targetSlot).isEmpty()) {
                continue;
            }

            int sourceSlot = findNextMovableStack(handler, targetSlot + 1);
            if (sourceSlot < 0) {
                break;
            }

            ItemStack stack = handler.getStackInSlot(sourceSlot);
            if (!isCleanUpMovable(stack)) {
                continue;
            }

            moved += moveIntoSlot(containerRef, sourceSlot, containerRef, targetSlot, stack.getCount(), moveListener);
        }

        return moved;
    }

    private static int mergeSlotWithinSameContainer(ContainerRef containerRef, int sourceSlot, MoveListener moveListener) {
        IItemHandler handler = containerRef.handler();
        ItemStack sourceStack = handler.getStackInSlot(sourceSlot);
        if (!isCleanUpMovable(sourceStack)) {
            return 0;
        }

        int moved = 0;
        for (int targetSlot = 0; targetSlot < handler.getSlots(); targetSlot++) {
            if (targetSlot == sourceSlot) {
                continue;
            }

            ItemStack currentSource = handler.getStackInSlot(sourceSlot);
            if (currentSource.isEmpty()) {
                break;
            }

            ItemStack targetStack = handler.getStackInSlot(targetSlot);
            if (!isCleanUpMovable(targetStack) || targetStack.getCount() >= targetStack.getMaxStackSize()) {
                continue;
            }

            if (!ItemStack.isSameItemSameTags(targetStack, currentSource)) {
                continue;
            }

            moved += moveIntoSlot(containerRef, sourceSlot, containerRef, targetSlot, currentSource.getCount(), moveListener);
        }
        return moved;
    }

    private static int findNextMovableStack(IItemHandler handler, int startSlot) {
        for (int slot = startSlot; slot < handler.getSlots(); slot++) {
            ItemStack stack = handler.getStackInSlot(slot);
            if (isCleanUpMovable(stack)) {
                return slot;
            }
        }
        return -1;
    }

    private static boolean isCleanUpMovable(ItemStack stack) {
        return !stack.isEmpty() && ItemCategoryResolver.resolve(stack).moveAllowed();
    }

    private static ItemStack returnRemainderToSource(IItemHandler source, int preferredSlot, ItemStack remainder) {
        ItemStack leftover = source.insertItem(preferredSlot, remainder, false);
        if (leftover.isEmpty()) {
            return ItemStack.EMPTY;
        }

        for (int slot = 0; slot < source.getSlots(); slot++) {
            leftover = source.insertItem(slot, leftover, false);
            if (leftover.isEmpty()) {
                return ItemStack.EMPTY;
            }
        }
        return leftover;
    }

    private static void returnPlayerRemainder(Player player, int preferredSlot, ItemStack remainder) {
        Inventory inventory = player.getInventory();
        ItemStack current = inventory.getItem(preferredSlot);
        if (current.isEmpty()) {
            inventory.setItem(preferredSlot, remainder);
            return;
        }

        if (ItemStack.isSameItemSameTags(current, remainder) && current.getCount() + remainder.getCount() <= current.getMaxStackSize()) {
            current.grow(remainder.getCount());
            return;
        }

        inventory.add(remainder);
        if (!remainder.isEmpty()) {
            player.drop(remainder.copy(), false);
        }
    }

    private static boolean sameInventory(ContainerRef a, ContainerRef b) {
        return a == b || a.identity() == b.identity() || a.handler() == b.handler() || a.pos().equals(b.pos());
    }
}
