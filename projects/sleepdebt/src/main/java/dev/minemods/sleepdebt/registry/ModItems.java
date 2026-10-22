package dev.minemods.sleepdebt.registry;
import dev.minemods.sleepdebt.SleepDebtMod;
import dev.minemods.sleepdebt.item.RestDrinkItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.BlockItem;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
public final class ModItems {
 public static final DeferredRegister.Items ITEMS=DeferredRegister.createItems(SleepDebtMod.MOD_ID);
 public static final DeferredItem<Item> ROASTED=ITEMS.registerSimpleItem("roasted_cocoa_beans",new Item.Properties());
 public static final DeferredItem<Item> GROUNDS=ITEMS.registerSimpleItem("coffee_grounds",new Item.Properties());
 public static final DeferredItem<RestDrinkItem> COFFEE=ITEMS.register("coffee",()->new RestDrinkItem(new Item.Properties().stacksTo(16),6000,28,0,0));
 public static final DeferredItem<RestDrinkItem> STRONG=ITEMS.register("strong_coffee",()->new RestDrinkItem(new Item.Properties().stacksTo(16),7400,55,0,0));
 public static final DeferredItem<RestDrinkItem> LONG=ITEMS.register("long_coffee",()->new RestDrinkItem(new Item.Properties().stacksTo(16),10500,38,0,0));
 public static final DeferredItem<RestDrinkItem> ENERGY=ITEMS.register("energy_drink",()->new RestDrinkItem(new Item.Properties().stacksTo(16),12000,70,0,0));
 public static final DeferredItem<RestDrinkItem> WARM_MILK=ITEMS.register("warm_milk",()->new RestDrinkItem(new Item.Properties().stacksTo(16),0,0,75,1));
 public static final DeferredItem<Item> PILLOW=ITEMS.registerSimpleItem("pillow",new Item.Properties().stacksTo(16));
 public static final DeferredItem<Item> MASK=ITEMS.registerSimpleItem("sleep_mask",new Item.Properties().stacksTo(1));
 public static final DeferredItem<BlockItem> COFFEE_MACHINE=ITEMS.register("coffee_machine",()->new BlockItem(ModBlocks.COFFEE_MACHINE.get(),new Item.Properties()));
 public static final DeferredItem<BlockItem> BEDROLL=ITEMS.register("camp_bedroll",()->new BlockItem(ModBlocks.CAMP_BEDROLL.get(),new Item.Properties()));
 public static final DeferredItem<BlockItem> AWNING=ITEMS.register("camp_awning",()->new BlockItem(ModBlocks.CAMP_AWNING.get(),new Item.Properties()));
 private ModItems(){}
}

