package dev.minemods.cowardlymobs.jockey;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Gameplay settings; evaluated only on the logical server. */
public final class SpiderJockeyConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED;
    public static final ModConfigSpec.DoubleValue CHANCE;
    public static final ModConfigSpec.DoubleValue FEAR_CHANCE;
    public static final ModConfigSpec.IntValue SEARCH_RADIUS;
    public static final ModConfigSpec.IntValue RETRY_COOLDOWN;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("spider_jockey");
        ENABLED = builder.comment("Let ordinary skeletons mount nearby free spiders.")
                .define("enabled", true);
        CHANCE = builder.comment("Chance per eligible meeting (not every tick).")
                .defineInRange("mountChance", 0.15D, 0.0D, 1.0D);
        FEAR_CHANCE = builder.comment("Mount chance if the skeleton is wounded and fears its target.")
                .defineInRange("frightenedMountChance", 0.35D, 0.0D, 1.0D);
        SEARCH_RADIUS = builder.comment("Max local radius of searches, in blocks.")
                .defineInRange("searchRadius", 6, 2, 16);
        RETRY_COOLDOWN = builder.comment("Ticks before another roll after a spider encounter; 600 = 30 seconds.")
                .defineInRange("retryCooldownTicks", 600, 60, 3600);
        builder.pop();
        SPEC = builder.build();
    }

    private SpiderJockeyConfig() {}
}
