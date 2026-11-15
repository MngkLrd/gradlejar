package dev.minemods.lassotrident;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(LassoTrident.MOD_ID)
public final class LassoTrident {
    public static final String MOD_ID = "lassotrident";

    public LassoTrident(IEventBus modBus) {
        modBus.addListener(TetherNetwork::register);
    }
}
