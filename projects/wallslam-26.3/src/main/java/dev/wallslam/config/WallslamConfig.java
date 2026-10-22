package dev.wallslam.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Server-synced gameplay options; defaults preserve WallSlam 0.3.3's balance. */
public final class WallslamConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.DoubleValue DAMAGE_MULTIPLIER = BUILDER
        .comment("Collision damage multiplier. 1.0 preserves classic WallSlam damage.")
        .defineInRange("damageMultiplier", 1.0, 0.0, 3.0);

    public static final ModConfigSpec.IntValue STUN_TICKS = BUILDER
        .comment("How many ticks a mob pauses after a wall impact; 0 disables stun.")
        .defineInRange("stunTicks", 10, 0, 40);

    public static final ModConfigSpec.DoubleValue PARTICLE_MULTIPLIER = BUILDER
        .comment("Visual intensity of wall impacts; 0 hides particles, sounds remain.")
        .defineInRange("particleMultiplier", 1.0, 0.0, 2.0);

    public static final ModConfigSpec.BooleanValue DEBUG = BUILDER
        .comment("Write wall-impact state transitions to latest.log; disabled for releases.")
        .define("debug", false);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private WallslamConfig() {}
}
