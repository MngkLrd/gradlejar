package dev.minemods.lassotrident;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;

@Mod(LassoTrident.MOD_ID)
public final class LassoTrident {
    public static final String MOD_ID = "lassotrident";

    public LassoTrident() {
        TetherNetwork.init();
        MinecraftForge.EVENT_BUS.register(TetherEvents.class);
    }
}
