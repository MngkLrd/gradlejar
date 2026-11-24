package dev.ataflore.catchtheblast.config;
import net.minecraftforge.common.ForgeConfigSpec;
public final class BlastConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue TERRAIN_DAMAGE;
    public static final ForgeConfigSpec.DoubleValue MAX_RESISTANCE;
    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        TERRAIN_DAMAGE = b.comment("Allow charged shockwaves to carve terrain. Normal block protection still applies.").define("terrainDamage", true);
        MAX_RESISTANCE = b.comment("Maximum explosion resistance a shockwave can break.").defineInRange("maxResistance", 8.0, 0.0, 100.0);
        SPEC = b.build();
    }
    private BlastConfig() {}
}
