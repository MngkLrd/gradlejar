package com.kirill.simplesorter.settings;

import com.kirill.simplesorter.config.SorterSettings;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

public record SortingWandSettings(
        SortingMode mode,
        int radius,
        boolean showRadius,
        boolean showContainerHighlights,
        boolean showTransferParticles
) {
    public static final int[] RADIUS_STEPS = new int[] {8, 12, 16, 24, 32};

    private static final String KEY_MODE = "sort_mode";
    private static final String KEY_MODE_INDEX = "sort_mode_index";
    private static final String KEY_RADIUS = "sort_radius";
    private static final String KEY_SHOW_RADIUS = "show_radius";
    private static final String KEY_SHOW_CONTAINER_HIGHLIGHTS = "show_container_highlights";
    private static final String KEY_SHOW_TRANSFER_PARTICLES = "show_transfer_particles";

    public static SortingWandSettings defaults() {
        return new SortingWandSettings(
                SortingMode.SMART_CATEGORIES,
                SorterSettings.SORT_RADIUS_BLOCKS,
                true,
                true,
                true
        );
    }

    public static SortingWandSettings readFrom(ItemStack stack) {
        if (stack.isEmpty()) {
            return defaults();
        }

        CompoundTag tag = stack.getOrCreateTag();
        SortingWandSettings defaults = defaults();

        SortingMode mode = defaults.mode();
        if (tag.contains(KEY_MODE_INDEX)) {
            mode = SortingMode.byIndex(tag.getInt(KEY_MODE_INDEX));
        } else if (tag.contains(KEY_MODE)) {
            mode = SortingMode.byId(tag.getString(KEY_MODE));
        }

        int radius = tag.contains(KEY_RADIUS) ? tag.getInt(KEY_RADIUS) : defaults.radius();
        boolean showRadius = tag.contains(KEY_SHOW_RADIUS) ? tag.getBoolean(KEY_SHOW_RADIUS) : defaults.showRadius();
        boolean showContainerHighlights = tag.contains(KEY_SHOW_CONTAINER_HIGHLIGHTS) ? tag.getBoolean(KEY_SHOW_CONTAINER_HIGHLIGHTS) : defaults.showContainerHighlights();
        boolean showTransferParticles = tag.contains(KEY_SHOW_TRANSFER_PARTICLES) ? tag.getBoolean(KEY_SHOW_TRANSFER_PARTICLES) : defaults.showTransferParticles();

        return new SortingWandSettings(mode, radius, showRadius, showContainerHighlights, showTransferParticles).sanitized();
    }

    public void writeTo(ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }

        SortingWandSettings safe = sanitized();
        CompoundTag tag = stack.getOrCreateTag();
        tag.putString(KEY_MODE, safe.mode().id());
        tag.putInt(KEY_MODE_INDEX, safe.mode().index());
        tag.putInt(KEY_RADIUS, safe.radius());
        tag.putBoolean(KEY_SHOW_RADIUS, safe.showRadius());
        tag.putBoolean(KEY_SHOW_CONTAINER_HIGHLIGHTS, safe.showContainerHighlights());
        tag.putBoolean(KEY_SHOW_TRANSFER_PARTICLES, safe.showTransferParticles());
        
    }

    public SortingWandSettings withMode(SortingMode newMode) {
        return new SortingWandSettings(newMode, radius, showRadius, showContainerHighlights, showTransferParticles).sanitized();
    }

    public SortingWandSettings nextMode() {
        return withMode(mode.next());
    }

    public SortingWandSettings withRadius(int newRadius) {
        return new SortingWandSettings(mode, newRadius, showRadius, showContainerHighlights, showTransferParticles).sanitized();
    }

    public SortingWandSettings previousRadius() {
        int currentIndex = radiusIndex(radius);
        int previousIndex = currentIndex <= 0 ? RADIUS_STEPS.length - 1 : currentIndex - 1;
        return withRadius(RADIUS_STEPS[previousIndex]);
    }

    public SortingWandSettings nextRadius() {
        int currentIndex = radiusIndex(radius);
        return withRadius(RADIUS_STEPS[(currentIndex + 1) % RADIUS_STEPS.length]);
    }

    public SortingWandSettings withShowRadius(boolean value) {
        return new SortingWandSettings(mode, radius, value, showContainerHighlights, showTransferParticles).sanitized();
    }

    public SortingWandSettings withShowContainerHighlights(boolean value) {
        return new SortingWandSettings(mode, radius, showRadius, value, showTransferParticles).sanitized();
    }

    public SortingWandSettings withShowTransferParticles(boolean value) {
        return new SortingWandSettings(mode, radius, showRadius, showContainerHighlights, value).sanitized();
    }

    public SortingWandSettings sanitized() {
        SortingMode safeMode = mode == null ? SortingMode.MATCH_EXISTING : mode;
        int safeRadius = nearestRadiusStep(radius);
        return new SortingWandSettings(safeMode, safeRadius, showRadius, showContainerHighlights, showTransferParticles);
    }

    private static int radiusIndex(int value) {
        int nearest = nearestRadiusStep(value);
        for (int i = 0; i < RADIUS_STEPS.length; i++) {
            if (RADIUS_STEPS[i] == nearest) {
                return i;
            }
        }
        return 0;
    }

    private static int nearestRadiusStep(int value) {
        int nearest = RADIUS_STEPS[0];
        int bestDistance = Math.abs(value - nearest);

        for (int step : RADIUS_STEPS) {
            int distance = Math.abs(value - step);
            if (distance < bestDistance) {
                nearest = step;
                bestDistance = distance;
            }
        }

        return nearest;
    }
}
