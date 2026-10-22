package dev.minemods.cowardlymobs.hunger.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class ZombieHungerConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.IntValue HEAL_HP;
    public static final ModConfigSpec.IntValue ROTTEN_RADIUS;
    public static final ModConfigSpec.IntValue RAW_RADIUS;
    public static final ModConfigSpec.IntValue COOKED_RADIUS;
    public static final ModConfigSpec.IntValue EAT_TICKS;
    public static final ModConfigSpec.IntValue EAT_COOLDOWN;
    public static final ModConfigSpec.IntValue HIT_INTERRUPT_TICKS;
    public static final ModConfigSpec.IntValue SCAN_TICKS;
    public static final ModConfigSpec.IntValue ALERT_RADIUS;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("zombie_hunger");
        HEAL_HP = builder.comment("HP healed by consuming one item. 2 HP = one heart.")
                .defineInRange("healHp", 2, 0, 20);
        ROTTEN_RADIUS = builder.comment("Detection radius for rotten flesh.")
                .defineInRange("rottenRadius", 12, 2, 32);
        RAW_RADIUS = builder.comment("Detection radius for raw meat.")
                .defineInRange("rawRadius", 8, 2, 32);
        COOKED_RADIUS = builder.comment("Detection radius for cooked meat.")
                .defineInRange("cookedRadius", 6, 2, 32);
        EAT_TICKS = builder.comment("Ticks the zombie spends eating before consuming one item.")
                .defineInRange("eatTicks", 30, 5, 100);
        EAT_COOLDOWN = builder.comment("Ticks between meals (100 = 5 seconds).")
                .defineInRange("eatCooldownTicks", 100, 0, 1200);
        HIT_INTERRUPT_TICKS = builder.comment("Ticks the zombie ignores food after being attacked by a player.")
                .defineInRange("hitInterruptTicks", 100, 0, 1200);
        SCAN_TICKS = builder.comment("Idle ticks between scans. Bigger is less CPU-intensive.")
                .defineInRange("scanIntervalTicks", 15, 5, 100);
        ALERT_RADIUS = builder.comment("Radius of the one-off hunger signal to nearby zombies.")
                .defineInRange("alertRadius", 6, 0, 24);
        builder.pop();
        SPEC = builder.build();
    }

    private ZombieHungerConfig() {}
}
