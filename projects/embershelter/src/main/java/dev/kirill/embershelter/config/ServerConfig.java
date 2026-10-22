package dev.kirill.embershelter.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class ServerConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue ALLOW_GRASS = BUILDER
            .comment("Allow camp blocks to replace grass, ferns and thin snow.")
            .define("allowReplacingGrass", true);
    public static final ModConfigSpec.BooleanValue ALLOW_FLOWERS = BUILDER
            .comment("Allow camp blocks to replace flowers.")
            .define("allowReplacingFlowers", true);
    public static final ModConfigSpec.BooleanValue CLEAR_LOGS = BUILDER
            .comment("Clear logs in the 7x7x4 airspace; never modify floor blocks.")
            .define("clearLogs", true);
    public static final ModConfigSpec.BooleanValue CLEAR_LEAVES = BUILDER
            .comment("Clear tree leaves in the campsite airspace.")
            .define("clearLeaves", true);
    public static final ModConfigSpec.IntValue BUILD_DELAY = BUILDER
            .comment("Ticks between visible steps. 20 ticks is about one second.")
            .defineInRange("buildDelayTicks", 5, 1, 40);
    public static final ModConfigSpec.IntValue MAX_ACTIVE = BUILDER
            .defineInRange("maxActiveCampsPerPlayer", 1, 1, 10);
    public static final ModConfigSpec.BooleanValue PROTECTION = BUILDER
            .define("protectCampFromWeakMobs", true);
    public static final ModConfigSpec.IntValue PROTECTION_RADIUS = BUILDER
            .defineInRange("protectionRadius", 7, 3, 16);
    public static final ModConfigSpec.BooleanValue OTHER_DIMENSIONS = BUILDER
            .define("allowProtectionOutsideOverworld", false);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private ServerConfig() {}
}
