package dev.minemods.potiontimerbar;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class HudConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED;
    public static final ModConfigSpec.BooleanValue AUTO_AVOID_AIR;
    public static final ModConfigSpec.IntValue VERTICAL_OFFSET;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.comment("Potion Timer Bar - one continuous 81px strip, longest remaining effect").push("hud");
        ENABLED = builder.comment("Show a bar only when an active effect exists").define("enabled", true);
        AUTO_AVOID_AIR = builder.comment("Keep the line clear of the vanilla air bubbles while underwater")
            .define("autoAvoidAir", true);
        VERTICAL_OFFSET = builder.comment("Move the strip vertically in GUI pixels; X and width always follow hunger")
            .defineInRange("verticalOffset", 0, -60, 60);
        builder.pop();
        SPEC = builder.build();
    }

    private HudConfig() {}
}
