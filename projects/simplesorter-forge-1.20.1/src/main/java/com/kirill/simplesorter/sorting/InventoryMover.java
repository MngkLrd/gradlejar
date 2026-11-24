package com.kirill.simplesorter.sorting;

import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.nbt.CompoundTag;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
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

        int extractedCount=actuallyExtracted.getCount();
        ItemStack actualRemainder = target.insertItem(targetSlot, actuallyExtracted.copy(), false);
        int actuallyMoved = extractedCount - actualRemainder.getCount();

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

        int extractedCount=extracted.getCount();
        ItemStack actualRemainder = target.insertItem(targetSlot, extracted.copy(), false);
        int actuallyMoved = extractedCount - actualRemainder.getCount();

        if (!actualRemainder.isEmpty()) {
            returnPlayerRemainder(player, playerSlot, actualRemainder);
        }

        if (actuallyMoved > 0 && moveListener != null) {
            moveListener.onMove(new MoveRecord(player.blockPosition(), targetRef.pos(), actuallyMoved, visualStack));
        }

        return Math.max(0, actuallyMoved);
    }

    /**
     * Cheap O(slots) preflight for Smart Categories. No actual insert/extract
     * simulation occurs unless there are holes or compatible partial stacks.
     */
    public static boolean needsCompaction(ContainerRef containerRef) {
        if (containerRef == null || containerRef.handler() == null) return false;
        IItemHandler handler = containerRef.handler();
        Set<StackFingerprint> partials = new HashSet<>();
        boolean hasHoleBefore = false;
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack stack = handler.getStackInSlot(slot);
            if (stack.isEmpty()) {
                hasHoleBefore = true;
                continue;
            }
            if (!isCleanUpMovable(stack)) continue;
            if (hasHoleBefore) return true;
            if (stack.getCount() < stack.getMaxStackSize()
                    && !partials.add(StackFingerprint.of(stack))) return true;
        }
        return false;
    }

    /**
     * Linear expected time compaction, versus the old nested all-slot sweeps.
     * First combine NBT-identical partial stacks, then close holes.
     * Every edit uses the existing simulated transfer transaction so modded
     * slot rules and TFC insertion limits are always respected.
     */
    public static int cleanUpContainer(ContainerRef containerRef, MoveListener moveListener) {
        if (containerRef == null || containerRef.handler() == null) return 0;
        IItemHandler handler = containerRef.handler();
        int moved = 0;
        Map<StackFingerprint, Integer> partialByKey = new HashMap<>();

        for (int sourceSlot = 0; sourceSlot < handler.getSlots(); sourceSlot++) {
            ItemStack source = handler.getStackInSlot(sourceSlot);
            if (!isCleanUpMovable(source)) continue;
            StackFingerprint key = StackFingerprint.of(source);
            Integer partialTarget = partialByKey.get(key);
            if (partialTarget != null && partialTarget != sourceSlot) {
                ItemStack previous = handler.getStackInSlot(partialTarget);
                if (!previous.isEmpty() && previous.getCount() < previous.getMaxStackSize()
                        && ItemStack.isSameItemSameTags(previous, source)) {
                    moved += moveWithinAudited(containerRef,sourceSlot,partialTarget,source.getCount(),moveListener);
                }
                ItemStack afterTarget = handler.getStackInSlot(partialTarget);
                if (afterTarget.isEmpty() || afterTarget.getCount() >= afterTarget.getMaxStackSize()) {
                    partialByKey.remove(key);
                }
            }
            ItemStack remaining = handler.getStackInSlot(sourceSlot);
            if (!remaining.isEmpty() && remaining.getCount() < remaining.getMaxStackSize()) {
                partialByKey.putIfAbsent(key, sourceSlot);
            }
        }

        int firstHole = 0;
        for (int sourceSlot = 0; sourceSlot < handler.getSlots(); sourceSlot++) {
            ItemStack current = handler.getStackInSlot(sourceSlot);
            if (!isCleanUpMovable(current)) continue;
            while (firstHole < sourceSlot && !handler.getStackInSlot(firstHole).isEmpty()) {
                firstHole++;
            }
            if (firstHole < sourceSlot) {
                moved += moveWithinAudited(containerRef,sourceSlot,firstHole,current.getCount(),moveListener);
            }
        }
        return moved;
    }

    /**
     * Compaction must not trust mutable arguments or a capability's claimed
     * remainder. Measure both physical slots, restore the proven remainder,
     * and fail closed on unexpected identity/count changes.
     */
    private static int moveWithinAudited(ContainerRef ref,int from,int to,int requested,MoveListener listener){
        if(from==to)return 0;
        IItemHandler h=ref.handler();
        ItemStack original=h.getStackInSlot(from).copy();
        ItemStack dest=h.getStackInSlot(to).copy();
        if(original.isEmpty()||(!dest.isEmpty()&&!InventoryConservation.sameIdentity(original,dest)))return 0;
        ItemStack simulated=h.extractItem(from,requested,true);
        if(simulated.isEmpty()||!InventoryConservation.sameIdentity(original,simulated))return 0;
        int offered=simulated.getCount();
        ItemStack remainder=h.insertItem(to,simulated.copy(),true);
        int accepted=offered-remainder.getCount();
        if(accepted<=0||accepted>original.getCount())return 0;
        RuntimeException problem=null;
        try{h.extractItem(from,accepted,false);}catch(RuntimeException ex){problem=ex;}
        int removed=original.getCount()-matching(h.getStackInSlot(from),original);
        if(removed<0||removed>accepted)throw new IllegalStateException("Unexpected compaction extraction count");
        if(removed==0){if(problem!=null)throw problem;return 0;}
        ItemStack carried=original.copyWithCount(removed);
        int inserted=0;
        if(problem==null){
            try{h.insertItem(to,carried.copy(),false);}catch(RuntimeException ex){problem=ex;}
            inserted=matching(h.getStackInSlot(to),original)-dest.getCount();
            if(inserted<0||inserted>removed)throw new IllegalStateException("Unexpected compaction insertion count");
        }
        int rest=removed-inserted;
        if(rest>0){
            int before=matching(h.getStackInSlot(from),original);
            try{h.insertItem(from,original.copyWithCount(rest),false);}
            catch(RuntimeException ex){problem=ex;}
            int restored=matching(h.getStackInSlot(from),original)-before;
            if(restored<0||restored>rest)throw new IllegalStateException("Unexpected compaction restoration count");
            rest-=restored;
            // Try other slots only for the observed remainder. Never overwrite.
            for(int s=0;s<h.getSlots()&&rest>0;s++){
                if(s==from||s==to)continue;
                ItemStack old=h.getStackInSlot(s);
                if(!old.isEmpty()&&!InventoryConservation.sameIdentity(old,original))continue;
                before=old.getCount();
                try{h.insertItem(s,original.copyWithCount(rest),false);}
                catch(RuntimeException ex){problem=ex;}
                restored=matching(h.getStackInSlot(s),original)-before;
                if(restored<0||restored>rest)throw new IllegalStateException("Unexpected recovery count");
                rest-=restored;
            }
            if(rest>0){
                Containers.dropItemStack(ref.level(),ref.pos().getX()+0.5,ref.pos().getY()+0.7,ref.pos().getZ()+0.5,
                        original.copyWithCount(rest));
                throw new IllegalStateException("Compaction emergency recovery; inspect dropped items");
            }
        }
        if(problem!=null)throw new IllegalStateException("Compaction capability threw; observed remainder restored",problem);
        if(inserted>0&&listener!=null)listener.onMove(new MoveRecord(ref.pos(),ref.pos(),inserted,original.copyWithCount(1)));
        return inserted;
    }
    private static int matching(ItemStack live,ItemStack sample){
        if(live.isEmpty())return 0;
        if(!InventoryConservation.sameIdentity(live,sample))throw new IllegalStateException("Inventory transformed item during compaction");
        return live.getCount();
    }

    private record StackFingerprint(Item item, CompoundTag tag) {
        static StackFingerprint of(ItemStack stack) {
            CompoundTag tag = stack.getTag();
            return new StackFingerprint(stack.getItem(), tag == null ? null : tag.copy());
        }
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
