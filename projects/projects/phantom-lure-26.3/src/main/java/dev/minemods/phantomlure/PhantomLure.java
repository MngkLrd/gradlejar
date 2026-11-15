package dev.minemods.phantomlure;

import dev.minemods.phantomlure.gameplay.PhantomCleanup;
import dev.minemods.phantomlure.registry.ModBlockEntities;
import dev.minemods.phantomlure.registry.ModBlocks;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

@Mod(PhantomLure.MOD_ID)
public final class PhantomLure {
    public static final String MOD_ID = "phantomlure";

    public PhantomLure(IEventBus modBus) {
        ModBlocks.register(modBus);
        ModBlockEntities.register(modBus);
        modBus.addListener(PhantomLure::addCreativeTab);
        NeoForge.EVENT_BUS.addListener(PhantomCleanup::onTick);
    }

    private static void addCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(ModBlocks.PHANTOM_LURE.get());
        }
    }
}
