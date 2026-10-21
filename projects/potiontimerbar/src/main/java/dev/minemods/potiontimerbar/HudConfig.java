package dev.minemods.potiontimerbar;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class HudConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED;
    public static final ModConfigSpec.BooleanValue SHOW_COUNTDOWN;
    public static final ModConfigSpec.BooleanValue PREFER_HARMFUL;
    public static final ModConfigSpec.BooleanValue AUTO_AVOID_AIR;
    public static final ModConfigSpec.IntValue VERTICAL_OFFSET;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.comment("Potion Timer Bar client HUD settings").push("hud");
        ENABLED = builder.comment("Enable the effect bar").define("enabled", true);
        SHOW_COUNTDOWN = builder.comment("Display remaining time as mm:ss instead of a full-length bar")
            .define("showCountdown", false);
        PREFER_HARMFUL = builder.comment("Harmful effects take priority over beneficial effects")
            .define("preferHarmful", true);
        AUTO_AVOID_AIR = builder.comment("Move the bar away from air bubbles while underwater")
            .define("autoAvoidAir", true);
        VERTICAL_OFFSET = builder.comment("Vertical adjustment in GUI-scaled pixels (negative moves up)")
            .defineInRange("verticalOffset", 0, -60, 60);
        builder.pop();
        SPEC = builder.build();
    }

    private HudConfig() {}
}
