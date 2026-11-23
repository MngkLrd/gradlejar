package com.kirill.simplesorter.sorting;

import com.kirill.simplesorter.settings.SortingMode;
import net.minecraft.network.chat.Component;

public final class SortingResult {
    private final int scannedContainers;
    private final int movedItems;
    private final int movedStacks;
    private final int roleCount;
    private final int noTargetStacks;
    private final int radius;
    private final String modeId;

    public SortingResult(int scannedContainers, int movedItems, int movedStacks, int roleCount, int noTargetStacks, int radius) {
        this(scannedContainers, movedItems, movedStacks, roleCount, noTargetStacks, radius, "sort");
    }

    public SortingResult(int scannedContainers, int movedItems, int movedStacks, int roleCount, int noTargetStacks, int radius, String modeId) {
        this.scannedContainers = scannedContainers;
        this.movedItems = movedItems;
        this.movedStacks = movedStacks;
        this.roleCount = roleCount;
        this.noTargetStacks = noTargetStacks;
        this.radius = radius;
        this.modeId = modeId == null || modeId.isBlank() ? "sort" : modeId;
    }

    public int scannedContainers() {
        return scannedContainers;
    }

    public int movedItems() {
        return movedItems;
    }

    public boolean used() {
        return movedItems > 0;
    }

    public Component toPlayerMessageComponent() {
        Component modeName = Component.translatable(SortingMode.byId(modeId).translationKey());

        if (scannedContainers == 0) {
            return Component.translatable("message.simplesorter.no_containers", radius);
        }

        if (movedItems == 0) {
            return Component.translatable("message.simplesorter.nothing_moved", modeName, scannedContainers, radius);
        }

        if (noTargetStacks > 0) {
            return Component.translatable("message.simplesorter.moved_with_misses", modeName, movedItems, movedStacks, radius, scannedContainers, roleCount, noTargetStacks);
        }

        return Component.translatable("message.simplesorter.moved", modeName, movedItems, movedStacks, radius, scannedContainers, roleCount);
    }

    public String toPlayerMessage() {
        String modeName = SortingMode.byId(modeId).englishName();
        String area = radius + " block radius";
        if (scannedContainers == 0) {
            return "Simple Sorter: no containers found in " + area + ".";
        }

        if (movedItems == 0) {
            return "Simple Sorter: " + modeName + " scanned " + scannedContainers + " containers in " + area + ", nothing moved.";
        }

        return "Simple Sorter: " + modeName + " moved " + movedItems + " items from " + movedStacks
                + " stacks. Radius: " + radius + ", containers: " + scannedContainers + ", changed: " + roleCount
                + (noTargetStacks > 0 ? ", no target: " + noTargetStacks + " stacks." : ".");
    }
}
