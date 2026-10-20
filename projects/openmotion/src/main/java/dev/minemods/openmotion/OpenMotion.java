package dev.minemods.openmotion;

import dev.minemods.openmotion.registry.ModBlocks;
import dev.minemods.openmotion.registry.ModBlockEntities;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

@Mod(OpenMotion.MOD_ID)
public final class OpenMotion {
    public static final String MOD_ID = "openmotion";

    public OpenMotion(IEventBus modBus) {
        ModBlocks.register(modBus);
        ModBlockEntities.register(modBus);
        modBus.addListener(OpenMotion::addCreativeItems);
    }

    private static void addCreativeItems(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.REDSTONE_BLOCKS) {
            event.accept(ModBlocks.AIR_FAN.get());
            event.accept(ModBlocks.ITEM_CANNON.get());
        }
    }
}
