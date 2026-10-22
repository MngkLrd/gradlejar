package dev.kirill.embershelter;

import dev.kirill.embershelter.config.ServerConfig;
import dev.kirill.embershelter.event.CampEvents;
import dev.kirill.embershelter.registry.ModItems;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;

@Mod(EmberShelter.MOD_ID)
public final class EmberShelter {
    public static final String MOD_ID = "embershelter";

    public EmberShelter(IEventBus modBus, ModContainer container) {
        ModItems.ITEMS.register(modBus);
        modBus.addListener(ModItems::onCreativeTab);
        container.registerConfig(ModConfig.Type.SERVER, ServerConfig.SPEC);
        NeoForge.EVENT_BUS.register(new CampEvents());
    }
}
