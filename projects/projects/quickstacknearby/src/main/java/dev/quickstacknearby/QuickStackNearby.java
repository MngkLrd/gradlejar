package dev.quickstacknearby;

import dev.quickstacknearby.config.QuickStackConfig;
import dev.quickstacknearby.network.QuickStackNetwork;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;

@Mod(QuickStackNearby.MOD_ID)
public final class QuickStackNearby {
    public static final String MOD_ID = "quickstacknearby";

    public QuickStackNearby() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, QuickStackConfig.SPEC);
        QuickStackNetwork.register();
    }
}
