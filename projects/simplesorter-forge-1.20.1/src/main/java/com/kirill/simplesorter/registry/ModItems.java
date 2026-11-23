package com.kirill.simplesorter.registry;
import com.kirill.simplesorter.item.SortingWandItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
public final class ModItems {
 private ModItems(){}
 public static final DeferredRegister<Item> ITEMS=DeferredRegister.create(ForgeRegistries.ITEMS,"simplesorter");
 public static final RegistryObject<Item> SORTING_WAND=ITEMS.register("sorting_wand",()->new SortingWandItem(new Item.Properties().stacksTo(1).durability(128)));
}
