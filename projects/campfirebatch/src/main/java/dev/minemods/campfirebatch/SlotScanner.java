package dev.minemods.campfirebatch;

import net.minecraft.world.level.block.entity.CampfireBlockEntity;

final class SlotScanner {
    private SlotScanner() {}
    static int freeSlots(CampfireBlockEntity fire) {
        int free = 0;
        for (var stack : fire.getItems()) {
            if (stack.isEmpty()) free++;
        }
        return Math.min(4, free);
    }
}
