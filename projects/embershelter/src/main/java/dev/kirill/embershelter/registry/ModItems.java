package dev.kirill.embershelter.registry;

import dev.kirill.embershelter.EmberShelter;
import dev.kirill.embershelter.item.CampKitItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    private ModItems() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(EmberShelter.MOD_ID);
    public static final DeferredItem<CampKitItem> CAMP_KIT = ITEMS.registerItem("camp_kit", CampKitItem::new);

    public static void onCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(CAMP_KIT.get());
        }
    }
}
