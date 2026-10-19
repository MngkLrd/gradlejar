package dev.anvilborn;

import dev.anvilborn.registry.ModComponents;
import dev.anvilborn.registry.ModMenus;
import dev.anvilborn.event.ForgeEvents;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

@Mod(Anvilborn.MODID)
public final class Anvilborn {
    public static final String MODID = "anvilborn";

    public Anvilborn(IEventBus modBus) {
        ModComponents.REGISTER.register(modBus);
        ModMenus.REGISTER.register(modBus);
        NeoForge.EVENT_BUS.register(new ForgeEvents());
    }
}
