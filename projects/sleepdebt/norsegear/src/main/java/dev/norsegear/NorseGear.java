package dev.norsegear;

import dev.norsegear.registry.ModArmorMaterials;
import dev.norsegear.registry.ModEntities;
import dev.norsegear.registry.ModItems;
import dev.norsegear.registry.ModBlocks;
import dev.norsegear.registry.ModRecipes;
import dev.norsegear.registry.ModTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(NorseGear.MODID)
public final class NorseGear {
    public static final String MODID = "norsegear";

    public NorseGear(IEventBus modBus) {
        ModArmorMaterials.ARMOR_MATERIALS.register(modBus);
        ModBlocks.BLOCKS.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModRecipes.SERIALIZERS.register(modBus);
        ModEntities.ENTITIES.register(modBus);
        ModTabs.TABS.register(modBus);
    }
}
