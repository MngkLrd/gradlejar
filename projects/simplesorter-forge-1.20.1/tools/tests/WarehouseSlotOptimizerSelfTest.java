package com.kirill.simplesorter.sorting;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Reproduces fully occupied, many-family warehouses and TFC-like restricted
 * slots without loading Minecraft or any other mod.
 */
public final class WarehouseSlotOptimizerSelfTest {
    private WarehouseSlotOptimizerSelfTest() {}

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void fullWarehouseWithRestrictedSlots() {
        int chestCount = 12;
        int slots = 36;
        int[] sizes = new int[chestCount];
        java.util.Arrays.fill(sizes, slots);
        List<WarehouseSlotOptimizer.Stack> items = new ArrayList<>();
        // Every chest starts FULL and mixed. Fifteen distinct categories
        // compete for twelve chests, with some slots requiring their own
        // original item (specialized TFC/GTCEu-like behavior).
        for (int c = 0; c < chestCount; c++) {
            for (int s = 0; s < slots; s++) {
                String family = "family_" + ((c * 7 + s * 11 + s / 6) % 15);
                items.add(new WarehouseSlotOptimizer.Stack(c, s, family));
            }
        }
        WarehouseSlotOptimizer.Accepts restriction = (index, chest, slot) -> {
            WarehouseSlotOptimizer.Stack original = items.get(index);
            if (slot == 0) return original.chest() == chest && original.slot() == 0;
            return true;
        };
        var result = WarehouseSlotOptimizer.organize(items, sizes, restriction);
        verify(items, sizes, result, restriction);
        check(result.crossChestStacks() > 0,
                "At least one cross-chest transfer required with 15 families and 12 FULL mixed chests");
        check(result.improvement() > 0, "Family concentration must improve");
        for (int c = 0; c < chestCount; c++) {
            int pinnedIndex = c * slots;
            check(result.chestByStack()[pinnedIndex] == c
                            && result.slotByStack()[pinnedIndex] == 0,
                    "Restricted TFC-style slot must not move at " + c);
        }
    }

    private static void reversibleAndIdempotent() {
        int[] sizes = {4,4,4,4};
        List<WarehouseSlotOptimizer.Stack> items = List.of(
                new WarehouseSlotOptimizer.Stack(0,0,"wood"),
                new WarehouseSlotOptimizer.Stack(0,1,"stone"),
                new WarehouseSlotOptimizer.Stack(0,2,"food"),
                new WarehouseSlotOptimizer.Stack(0,3,"metal"),
                new WarehouseSlotOptimizer.Stack(1,0,"stone"),
                new WarehouseSlotOptimizer.Stack(1,1,"wood"),
                new WarehouseSlotOptimizer.Stack(1,2,"food"),
                new WarehouseSlotOptimizer.Stack(1,3,"metal"),
                new WarehouseSlotOptimizer.Stack(2,0,"metal"),
                new WarehouseSlotOptimizer.Stack(2,1,"stone"),
                new WarehouseSlotOptimizer.Stack(2,2,"wood"),
                new WarehouseSlotOptimizer.Stack(2,3,"food"),
                new WarehouseSlotOptimizer.Stack(3,0,"food"),
                new WarehouseSlotOptimizer.Stack(3,1,"metal"),
                new WarehouseSlotOptimizer.Stack(3,2,"stone"),
                new WarehouseSlotOptimizer.Stack(3,3,"wood")
        );
        var accepts = (WarehouseSlotOptimizer.Accepts) (i,c,s) -> true;
        var result = WarehouseSlotOptimizer.organize(items, sizes, accepts);
        verify(items, sizes, result, accepts);
        check(result.crossChestStacks() > 0, "Fully occupied 4 chest exchange must move items");
        check(result.improvement() > 0, "Fully occupied 4 chest exchange must improve families");
        List<WarehouseSlotOptimizer.Stack> after = new ArrayList<>();
        int[] dstChest = result.chestByStack(), dstSlot = result.slotByStack();
        for (int i = 0; i < items.size(); i++)
            after.add(new WarehouseSlotOptimizer.Stack(
                    dstChest[i], dstSlot[i], items.get(i).family()));
        var again = WarehouseSlotOptimizer.organize(after, sizes, accepts);
        verify(after, sizes, again, accepts);
        check(again.improvement() == 0, "Already optimized layout should be stable on repeated use");
    }

    private static void verify(List<WarehouseSlotOptimizer.Stack> items, int[] sizes,
                               WarehouseSlotOptimizer.Layout result,
                               WarehouseSlotOptimizer.Accepts rule) {
        int[] targets = result.chestByStack();
        int[] positions = result.slotByStack();
        check(targets.length == items.size(), "Missing item assignments");
        Set<String> slots = new HashSet<>();
        for (int i = 0; i < items.size(); i++) {
            check(targets[i] >= 0 && targets[i] < sizes.length, "Invalid destination chest");
            check(positions[i] >= 0 && positions[i] < sizes[targets[i]], "Invalid destination slot");
            check(rule.test(i, targets[i], positions[i]), "Invalid placement for item " + i);
            check(slots.add(targets[i] + ":" + positions[i]), "Two items assigned to same slot");
        }
    }

    private static void newEmptyChestBeyondOldBatch() {
        int[] sizes=new int[22];
        java.util.Arrays.fill(sizes,4);
        List<WarehouseSlotOptimizer.Stack> items=new ArrayList<>();
        for(int c=0;c<21;c++) for(int slot=0;slot<4;slot++)
            items.add(new WarehouseSlotOptimizer.Stack(c,slot,"family_"+c+"_"+(slot/2)));
        var accepts=(WarehouseSlotOptimizer.Accepts)(i,c,s)->true;
        var result=WarehouseSlotOptimizer.organize(items,sizes,accepts);
        verify(items,sizes,result,accepts);
        check(java.util.Arrays.stream(result.chestByStack()).anyMatch(c->c==21),
                "New empty chest #22 must receive an entire family");
        check(result.crossChestStacks()>=2,"Empty chest must receive a whole family");
    }

    public static void main(String[] args) {
        newEmptyChestBeyondOldBatch();
        fullWarehouseWithRestrictedSlots();
        reversibleAndIdempotent();
        System.out.println("Wand buffer: full 12 chest no-empty regression, locked slots, total preservation and repeat click passed.");
    }
}
