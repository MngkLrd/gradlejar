package dev.minemods.openmotion;

import dev.minemods.openmotion.registry.ModBlocks;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
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
        modBus.addListener(OpenMotion::registerCapabilities);
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.VACUUM_HOPPER.get(), (entity, side) -> entity.inventory());
    }

    private static void addCreativeItems(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.REDSTONE_BLOCKS) {
            event.accept(ModBlocks.AIR_FAN.get());
            event.accept(ModBlocks.ITEM_CANNON.get());
            event.accept(ModBlocks.VACUUM_HOPPER.get());
            event.accept(ModBlocks.SPRINKLER.get());
        }
    }
}
