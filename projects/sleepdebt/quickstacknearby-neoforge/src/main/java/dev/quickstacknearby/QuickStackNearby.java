package dev.quickstacknearby;

import dev.quickstacknearby.config.QuickStackConfig;
import dev.quickstacknearby.network.QuickStackNetwork;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

@Mod(QuickStackNearby.MOD_ID)
public final class QuickStackNearby {
    public static final String MOD_ID = "quickstacknearby";

    public QuickStackNearby(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.SERVER, QuickStackConfig.SPEC);
        modBus.addListener(QuickStackNetwork::register);
    }
}
