package dev.quickstacknearby.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class QuickStackConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec.IntValue RADIUS = BUILDER
        .comment("Radius for nearby chests in blocks. Server controlled, 1..16.")
        .defineInRange("radius", 10, 1, 16);
    public static final ForgeConfigSpec SPEC = BUILDER.build();

    private QuickStackConfig() {}
}
