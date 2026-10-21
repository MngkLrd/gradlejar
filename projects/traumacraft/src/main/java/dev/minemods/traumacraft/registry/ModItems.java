package dev.minemods.traumacraft.registry;

import dev.minemods.traumacraft.TraumaCraft;
import dev.minemods.traumacraft.item.MedicalItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, TraumaCraft.MOD_ID);
    public static final DeferredHolder<Item, Item> BANDAGE = ITEMS.register("bandage",
            () -> new MedicalItem(new Item.Properties().stacksTo(16), MedicalItem.Type.BANDAGE));
    public static final DeferredHolder<Item, Item> SPLINT = ITEMS.register("splint",
            () -> new MedicalItem(new Item.Properties().stacksTo(16), MedicalItem.Type.SPLINT));
    public static final DeferredHolder<Item, Item> MEDKIT = ITEMS.register("medkit",
            () -> new MedicalItem(new Item.Properties().stacksTo(8), MedicalItem.Type.MEDKIT));

    private ModItems() {}
    public static void register(IEventBus bus) {
        ITEMS.register(bus);
        bus.addListener(ModItems::addToCreativeTab);
    }
    private static void addToCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(BANDAGE.get());
            event.accept(SPLINT.get());
            event.accept(MEDKIT.get());
        }
    }
}
