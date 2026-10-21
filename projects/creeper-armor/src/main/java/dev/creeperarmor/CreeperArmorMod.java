package dev.creeperarmor;

import dev.creeperarmor.gameplay.CreeperGameplayEvents;
import dev.creeperarmor.registry.ModArmorMaterials;
import dev.creeperarmor.registry.ModItems;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

@Mod(CreeperArmorMod.MOD_ID)
public final class CreeperArmorMod {
    public static final String MOD_ID = "creeperarmor";

    public CreeperArmorMod(IEventBus modBus) {
        ModArmorMaterials.MATERIALS.register(modBus);
        ModItems.ITEMS.register(modBus);
        modBus.addListener(ModItems::addToCreativeTabs);
        NeoForge.EVENT_BUS.register(new CreeperGameplayEvents());
    }
}
