package com.kirill.simplesorter.config;

public final class SorterSettings {
    private SorterSettings() {}

    /**
     * Main sorting radius around the player, measured in blocks.
     * The scanner uses a real 3D radius, not the current chunk.
     */
    public static final int SORT_RADIUS_BLOCKS = 16;

    /**
     * Client-side particle ring quality while the wand is held.
     * Higher value means more particles around the radius circle.
     */
    public static final int RADIUS_RING_PARTICLES = 48;

    /**
     * Hard cap for item flight particle traces after one sorting action.
     * Prevents massive storage walls from dumping thousands of particles.
     */
    public static final int MAX_MOVE_PARTICLE_TRACES = 96;

    /**
     * Hard cap for highlighted changed containers after one sorting action.
     */
    public static final int MAX_HIGHLIGHTED_CONTAINERS = 10;
}
