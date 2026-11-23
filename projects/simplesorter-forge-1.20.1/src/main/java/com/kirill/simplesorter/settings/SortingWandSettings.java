package com.kirill.simplesorter.settings;

import com.kirill.simplesorter.config.SorterSettings;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

public record SortingWandSettings(
        SortingMode mode,
        int radius,
        boolean showRadius,
        boolean showContainerHighlights,
        boolean showTransferParticles,
        boolean autoOrganize,
        int labelMode
) {
    public static final int[] RADIUS_STEPS = new int[] {8, 12, 16, 24, 32, 48, 64};

    private static final String KEY_MODE = "sort_mode";
    private static final String KEY_MODE_INDEX = "sort_mode_index";
    private static final String KEY_RADIUS = "sort_radius";
    private static final String KEY_SHOW_RADIUS = "show_radius";
    private static final String KEY_SHOW_CONTAINER_HIGHLIGHTS = "show_container_highlights";
    private static final String KEY_SHOW_TRANSFER_PARTICLES = "show_transfer_particles";
    private static final String KEY_AUTO_ORGANIZE = "auto_organize";
    private static final String KEY_LABEL_MODE = "label_mode";

    public static SortingWandSettings defaults() {
        return new SortingWandSettings(
                SortingMode.SMART_CATEGORIES,
                SorterSettings.SORT_RADIUS_BLOCKS,
                true,
                true,
                true,
                false,
                0
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

        boolean auto = tag.contains(KEY_AUTO_ORGANIZE) ? tag.getBoolean(KEY_AUTO_ORGANIZE) : defaults.autoOrganize();
        int labels = tag.contains(KEY_LABEL_MODE) ? tag.getInt(KEY_LABEL_MODE) : defaults.labelMode();
        return new SortingWandSettings(mode, radius, showRadius, showContainerHighlights, showTransferParticles, auto, labels).sanitized();
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
        tag.putBoolean(KEY_AUTO_ORGANIZE, safe.autoOrganize());
        tag.putInt(KEY_LABEL_MODE, safe.labelMode());
        
    }

    public SortingWandSettings withMode(SortingMode newMode) {
        return new SortingWandSettings(newMode, radius, showRadius, showContainerHighlights, showTransferParticles, autoOrganize, labelMode).sanitized();
    }

    public SortingWandSettings nextMode() {
        return withMode(mode.next());
    }

    public SortingWandSettings withRadius(int newRadius) {
        return new SortingWandSettings(mode, newRadius, showRadius, showContainerHighlights, showTransferParticles, autoOrganize, labelMode).sanitized();
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
        return new SortingWandSettings(mode, radius, value, showContainerHighlights, showTransferParticles, autoOrganize, labelMode).sanitized();
    }

    public SortingWandSettings withShowContainerHighlights(boolean value) {
        return new SortingWandSettings(mode, radius, showRadius, value, showTransferParticles, autoOrganize, labelMode).sanitized();
    }

    public SortingWandSettings withShowTransferParticles(boolean value) {
        return new SortingWandSettings(mode, radius, showRadius, showContainerHighlights, value, autoOrganize, labelMode).sanitized();
    }

    public SortingWandSettings withAutoOrganize(boolean newValue) {
        return new SortingWandSettings(mode, radius, showRadius,
                showContainerHighlights, showTransferParticles, newValue, labelMode).sanitized();
    }

    /** 0 off, 1 text sign, 2 representative item icon. */
    public SortingWandSettings withLabelMode(int newMode) {
        return new SortingWandSettings(mode, radius, showRadius,
                showContainerHighlights, showTransferParticles, autoOrganize, newMode).sanitized();
    }

    public SortingWandSettings nextLabelMode() {
        return withLabelMode((labelMode + 1) % 3);
    }

    public SortingWandSettings sanitized() {
        SortingMode safeMode = mode == null || mode == SortingMode.COMPACT_STACKS
                ? SortingMode.SMART_CATEGORIES : mode;
        int safeRadius = nearestRadiusStep(radius);
        return new SortingWandSettings(safeMode, safeRadius, showRadius, showContainerHighlights,
                showTransferParticles, autoOrganize, Math.max(0, Math.min(2, labelMode)));
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
