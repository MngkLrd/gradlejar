package dev.kirill.embershelter.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class ServerConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue CONSUME_MATERIALS = BUILDER
            .comment("Require and consume vanilla building blocks before starting a camp.")
            .define("consumeMaterials", true);
    public static final ModConfigSpec.BooleanValue ALLOW_GRASS = BUILDER
            .comment("Allow camp blocks to replace grass, ferns and thin snow.")
            .define("allowReplacingGrass", true);
    public static final ModConfigSpec.BooleanValue ALLOW_FLOWERS = BUILDER
            .comment("Allow camp blocks to replace flowers.")
            .define("allowReplacingFlowers", false);
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
