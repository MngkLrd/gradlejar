package dev.quickstacknearby.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class QuickStackConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec.IntValue RADIUS = BUILDER
        .comment("Chest search radius in blocks (server controlled, 1 to 16).")
        .defineInRange("radius", 10, 1, 16);
    public static final ModConfigSpec SPEC = BUILDER.build();

    private QuickStackConfig() {}
}
