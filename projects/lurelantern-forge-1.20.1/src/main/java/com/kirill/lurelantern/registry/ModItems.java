package com.kirill.lurelantern.registry;
import com.kirill.lurelantern.item.LureLanternBlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.*;
public final class ModItems {
 public static final DeferredRegister<Item> ITEMS=DeferredRegister.create(ForgeRegistries.ITEMS,"lurelantern");
 public static final RegistryObject<Item> LURE_LANTERN=ITEMS.register("lure_lantern",
   ()->new LureLanternBlockItem(ModBlocks.LURE_LANTERN.get(),new Item.Properties().stacksTo(1),false));
 public static final RegistryObject<Item> ADVANCED_LURE_LANTERN=ITEMS.register("advanced_lure_lantern",
   ()->new LureLanternBlockItem(ModBlocks.ADVANCED_LURE_LANTERN.get(),new Item.Properties().stacksTo(1),true));
 public static void register(IEventBus bus){ITEMS.register(bus);}
}
