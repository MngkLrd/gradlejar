package com.kirill.lurelantern.registry;
import com.kirill.lurelantern.item.LureLanternBlockItem;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import java.util.function.Supplier;
public final class ModItems {
 private static Item BASIC, ADVANCED;
 public static final Supplier<Item> LURE_LANTERN=()->BASIC, ADVANCED_LURE_LANTERN=()->ADVANCED;
 public static void register() {
   BASIC=Registry.register(BuiltInRegistries.ITEM,new ResourceLocation("lurelantern","lure_lantern"),
     new LureLanternBlockItem(ModBlocks.LURE_LANTERN.get(),new Item.Properties().stacksTo(1),false));
   ADVANCED=Registry.register(BuiltInRegistries.ITEM,new ResourceLocation("lurelantern","advanced_lure_lantern"),
     new LureLanternBlockItem(ModBlocks.ADVANCED_LURE_LANTERN.get(),new Item.Properties().stacksTo(1),true));
 }
}
