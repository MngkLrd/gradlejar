package dev.mngklrd.primitivewater;

import dev.mngklrd.primitivewater.registry.ModBlockEntities;
import dev.mngklrd.primitivewater.registry.ModBlocks;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(PrimitiveWater.MOD_ID)
public final class PrimitiveWater {
    public static final String MOD_ID = "primitivewater";

    public PrimitiveWater() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ModBlocks.BLOCKS.register(bus);
        ModBlocks.ITEMS.register(bus);
        ModBlockEntities.TYPES.register(bus);
        bus.addListener(this::onCreativeTab);
    }

    private void onCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(ModBlocks.COLLECTOR_ITEM.get());
            event.accept(ModBlocks.GUTTER_ITEM.get());
            event.accept(ModBlocks.DOWNSPOUT_ITEM.get());
        }
    }
}
