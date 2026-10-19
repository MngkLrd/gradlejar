package dev.fieldkit;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;

/** Client-only mod: server installation is not required. */
@Mod(value = FieldKit.MOD_ID, dist = {Dist.CLIENT})
public final class FieldKit {
    public static final String MOD_ID = "fieldkit";

    public FieldKit() {
    }
}
