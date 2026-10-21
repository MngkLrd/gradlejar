package dev.minemods.openmotion.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Server-side gameplay values. Defaults preserve vanilla-size automation. */
public final class OpenMotionConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue FAN_BREAK_SAPLINGS;
    public static final ModConfigSpec.BooleanValue FAN_HARVEST_CROPS;
    public static final ModConfigSpec.IntValue FAN_HARVEST_INTERVAL;
    public static final ModConfigSpec.IntValue FAN_HARVEST_RANGE;
    public static final ModConfigSpec.IntValue WATER_MAX_NODES;
    public static final ModConfigSpec.IntValue SPRINKLER_INTERVAL;
    public static final ModConfigSpec.IntValue SPRINKLER_RADIUS;
    public static final ModConfigSpec.IntValue HOPPER_INTERVAL;
    public static final ModConfigSpec.DoubleValue HOPPER_RADIUS;
    public static final ModConfigSpec.DoubleValue CANNON_SPEED;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("fan");
        FAN_BREAK_SAPLINGS = b.comment("Allow an active Air Fan to break saplings in its wind lane.")
                .define("breakSaplings", true);
        FAN_HARVEST_CROPS = b.comment("Harvest mature vanilla/tagged crops, never immature crops.")
                .define("harvestMatureCrops", true);
        FAN_HARVEST_INTERVAL = b.comment("Ticks between farm-cutting attempts; 20 ticks = 1 second.")
                .defineInRange("harvestInterval", 12, 3, 200);
        FAN_HARVEST_RANGE = b.comment("Number of blocks scanned along the fan's facing direction.")
                .defineInRange("harvestRange", 5, 1, 8);
        b.pop();
        b.push("water");
        WATER_MAX_NODES = b.comment("Maximum number of loaded machines in one water network. Oversized networks fail dry.")
                .defineInRange("maxNetworkNodes", 4096, 128, 16384);
        SPRINKLER_INTERVAL = b.comment("Ticks per growth attempt.")
                .defineInRange("sprinklerInterval", 100, 20, 1200);
        SPRINKLER_RADIUS = b.comment("Sprinkler crop search radius (blocks).")
                .defineInRange("sprinklerRadius", 4, 1, 8);
        b.pop();
        b.push("items");
        HOPPER_INTERVAL = b.comment("Ticks between vacuum collection checks.")
                .defineInRange("vacuumInterval", 6, 2, 60);
        HOPPER_RADIUS = b.comment("Maximum pickup radius in blocks.")
                .defineInRange("vacuumRadius", 3.5, 1.0, 8.0);
        CANNON_SPEED = b.comment("Item Cannon launch speed in blocks/tick (before redstone boost).")
                .defineInRange("cannonSpeed", 1.15, 0.3, 3.0);
        b.pop();
        SPEC = b.build();
    }

    private OpenMotionConfig() { }
}
