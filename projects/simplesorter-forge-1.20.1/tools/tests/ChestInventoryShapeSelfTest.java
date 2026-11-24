package com.kirill.simplesorter.sorting;

import java.util.Arrays;
import static com.kirill.simplesorter.sorting.ChestInventoryShape.Mode.*;

public final class ChestInventoryShapeSelfTest {
    private static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }

    private static void shape(int a, int b, int av, int bv, boolean shared,
                              ChestInventoryShape.Mode expected) {
        check(ChestInventoryShape.resolve(a,b,av,bv,shared) == expected,
                "Wrong shape " + a + "+" + b + " views " + av + "+" + bv);
    }

    private static void sharedView(int half) {
        // Two distinct wrapper objects may still alias the same physical
        // backing store. A scanner selecting one combined view must expose
        // exactly the physical items, never concatenate both wrappers.
        int[] physical = new int[half * 2];
        for (int i=0; i<physical.length; i++) physical[i] = i+1;
        View a = new View(physical), b = new View(physical);
        check(a != b, "Different wrappers required");
        var mode = ChestInventoryShape.resolve(half,half,a.size(),b.size(),false);
        check(mode == FIRST_COMBINED, "Shared view selected twice");
        View selected = mode == FIRST_COMBINED ? a : b;
        int expected = Arrays.stream(physical).sum(), extracted = 0;
        for (int i=0; i<selected.size(); i++) extracted += selected.extract(i);
        check(extracted == expected, "Duplicate or missing physical items");
        check(Arrays.stream(physical).sum() == 0, "Buffer did not empty physical storage");
        for (int i=0; i<selected.size(); i++) selected.backing[i] = i+1;
        check(Arrays.stream(physical).sum() == expected, "Refill changed item count");
    }

    private record View(int[] backing) {
        int size() { return backing.length; }
        int extract(int slot) { int item=backing[slot]; backing[slot]=0; return item; }
    }

    public static void main(String[] args) {
        shape(27,27,54,54,false,FIRST_COMBINED);
        shape(18,18,36,36,false,FIRST_COMBINED);
        shape(18,18,36,36,true,FIRST_COMBINED);
        shape(27,27,27,27,false,TWO_HALVES);
        shape(18,18,18,18,false,TWO_HALVES);
        shape(9,18,9,27,false,SECOND_COMBINED);
        shape(9,18,27,18,false,FIRST_COMBINED);
        shape(48,48,96,96,false,FIRST_COMBINED);
        shape(18,18,18,18,true,UNSAFE);
        shape(18,18,72,36,false,UNSAFE);
        shape(18,18,35,36,false,UNSAFE);
        shape(0,18,18,18,false,UNSAFE);
        sharedView(18);
        sharedView(27);
        sharedView(48);
        System.out.println("Chest inventory shape: shared, split, mixed and ambiguous wrappers; alias-backed physical conservation passed.");
    }
}
