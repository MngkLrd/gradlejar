package dev.minemods.phantomlure;
import dev.minemods.phantomlure.registry.ModBlocks;
import dev.minemods.phantomlure.registry.ModBlockEntities;
import dev.minemods.phantomlure.gameplay.PhantomCleanup;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.world.item.CreativeModeTabs;
public final class PhantomLure implements ModInitializer {
 public static final String MOD_ID="phantomlure";
 @Override public void onInitialize() {
   ModBlocks.initialize();
   ModBlockEntities.initialize();
   ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS)
       .register(entries -> entries.accept(ModBlocks.PHANTOM_LURE));
   ServerTickEvents.END_WORLD_TICK.register(PhantomCleanup::onWorldTick);
 }
}
