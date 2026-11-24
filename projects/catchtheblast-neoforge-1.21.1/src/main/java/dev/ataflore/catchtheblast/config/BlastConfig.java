package dev.ataflore.catchtheblast.config;
import net.neoforged.neoforge.common.ModConfigSpec;
public final class BlastConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue TERRAIN_DAMAGE;
    public static final ModConfigSpec.DoubleValue MAX_RESISTANCE;
    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        TERRAIN_DAMAGE = b.comment("Allow charged shockwaves to carve terrain. Normal block protection still applies.").define("terrainDamage", true);
        MAX_RESISTANCE = b.comment("Maximum explosion resistance a shockwave can break.").defineInRange("maxResistance", 8.0, 0.0, 100.0);
        SPEC = b.build();
    }
    private BlastConfig() {}
}
