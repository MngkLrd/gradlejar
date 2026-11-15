package dev.minemods.cowardlymobs.spider;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Server-controlled tuning for ordinary spider hunting and mounted jockeys. */
public final class SpiderInstinctConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED;
    public static final ModConfigSpec.BooleanValue FLANKING;
    public static final ModConfigSpec.BooleanValue GAZE_AVOIDANCE;
    public static final ModConfigSpec.BooleanValue WALL_AMBUSH;
    public static final ModConfigSpec.BooleanValue JOCKEY_TACTICS;
    public static final ModConfigSpec.IntValue GAZE_RANGE;
    public static final ModConfigSpec.DoubleValue GAZE_CONE;
    public static final ModConfigSpec.IntValue WALL_CHANCE;
    public static final ModConfigSpec.IntValue DIVE_COOLDOWN;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("spider_instinct");
        ENABLED = builder.comment("Enable the new spider hunting behavior.")
                .define("enabled", true);
        FLANKING = builder.comment("Try to approach players from behind or the side.")
                .define("flanking", true);
        GAZE_AVOIDANCE = builder.comment("Retreat when the player looks directly at an ordinary spider.")
                .define("gazeAvoidance", true);
        WALL_AMBUSH = builder.comment("Use a nearby wall for an airborne ambush.")
                .define("wallAmbush", true);
        JOCKEY_TACTICS = builder.comment("A spider with a skeleton rider favors distance and lateral approaches.")
                .define("jockeyTactics", true);
        GAZE_RANGE = builder.comment("Range in blocks at which a spider notices that it is being watched.")
                .defineInRange("gazeRange", 12, 4, 24);
        GAZE_CONE = builder.comment("Total angle in degrees of the player's viewing cone.")
                .defineInRange("gazeConeDegrees", 100.0D, 25.0D, 160.0D);
        WALL_CHANCE = builder.comment("Percent chance to attempt a wall ambush when suitable terrain is found.")
                .defineInRange("wallAmbushChancePercent", 50, 0, 100);
        DIVE_COOLDOWN = builder.comment("Minimum ticks between wall-jump attempts (200 = 10 seconds).")
                .defineInRange("diveCooldownTicks", 200, 40, 1200);
        builder.pop();
        SPEC = builder.build();
    }

    private SpiderInstinctConfig() {}
}
