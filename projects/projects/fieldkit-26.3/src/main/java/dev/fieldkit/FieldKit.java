package dev.fieldkit;

import dev.fieldkit.client.ClientInput;
import dev.fieldkit.client.ClientRegistration;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

/** FieldKit is installed only on the client. */
@Mod(value = FieldKit.MOD_ID, dist = Dist.CLIENT)
public final class FieldKit {
    public static final String MOD_ID = "fieldkit";
    
    public FieldKit(IEventBus modBus) {
        modBus.addListener(ClientRegistration::registerKeys);
        modBus.addListener(ClientRegistration::registerOverlay);
        NeoForge.EVENT_BUS.addListener(ClientInput::onClientTick);
        NeoForge.EVENT_BUS.addListener(ClientInput::onDisconnect);
    }
}
