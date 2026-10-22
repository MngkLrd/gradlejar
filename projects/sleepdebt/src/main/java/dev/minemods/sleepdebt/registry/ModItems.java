package dev.minemods.sleepdebt.registry;
import dev.minemods.sleepdebt.SleepDebtMod;
import dev.minemods.sleepdebt.item.RestDrinkItem;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import dev.minemods.sleepdebt.item.SleepJournalItem;
import dev.minemods.sleepdebt.item.HerbalPoulticeItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.SpawnEggItem;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
public final class ModItems {
 public static final TagKey<Item> INCENSE_FUELS=TagKey.create(Registries.ITEM,
  ResourceLocation.fromNamespaceAndPath(SleepDebtMod.MOD_ID,"incense_fuels"));
 public static final DeferredRegister.Items ITEMS=DeferredRegister.createItems(SleepDebtMod.MOD_ID);
 public static final DeferredItem<Item> ROASTED=ITEMS.registerSimpleItem("roasted_cocoa_beans",new Item.Properties());
 public static final DeferredItem<Item> GROUNDS=ITEMS.registerSimpleItem("coffee_grounds",new Item.Properties());
 public static final DeferredItem<Item> DRIED_HERBS=ITEMS.registerSimpleItem("dried_herbs",new Item.Properties());
 public static final DeferredItem<Item> DREAM_SHARD=ITEMS.registerSimpleItem("dream_shard",new Item.Properties());
 public static final DeferredItem<RestDrinkItem> COFFEE=ITEMS.register("coffee",()->new RestDrinkItem(new Item.Properties().stacksTo(16),6000,28,0,0));
 public static final DeferredItem<RestDrinkItem> STRONG=ITEMS.register("strong_coffee",()->new RestDrinkItem(new Item.Properties().stacksTo(16),7400,55,0,0));
 public static final DeferredItem<RestDrinkItem> LONG=ITEMS.register("long_coffee",()->new RestDrinkItem(new Item.Properties().stacksTo(16),10500,38,0,0));
 public static final DeferredItem<RestDrinkItem> ENERGY=ITEMS.register("energy_drink",()->new RestDrinkItem(new Item.Properties().stacksTo(16),12000,70,0,0));
 public static final DeferredItem<RestDrinkItem> WARM_MILK=ITEMS.register("warm_milk",()->new RestDrinkItem(new Item.Properties().stacksTo(16),0,0,75,1));
 public static final DeferredItem<RestDrinkItem> HERBAL_TEA=ITEMS.register("herbal_tea",()->new RestDrinkItem(new Item.Properties().stacksTo(16),0,0,55,2));
 public static final DeferredItem<RestDrinkItem> DREAM_TEA=ITEMS.register("dream_tea",()->new RestDrinkItem(new Item.Properties().stacksTo(16),0,0,160,3));
 public static final DeferredItem<RestDrinkItem> CARAMEL_COFFEE=ITEMS.register("caramel_coffee",()->new RestDrinkItem(new Item.Properties().stacksTo(16),8500,35,0,0));
 public static final DeferredItem<RestDrinkItem> BITTER_TONIC=ITEMS.register("bitter_tonic",()->new RestDrinkItem(new Item.Properties().stacksTo(16),4000,13,0,2));
 public static final DeferredItem<Item> COCOA_BISCUIT=ITEMS.registerSimpleItem("cocoa_biscuit",new Item.Properties()
  .food(new FoodProperties.Builder().nutrition(3).saturationModifier(.35f).build()));
 public static final DeferredItem<Item> INCENSE_BUNDLE=
   ITEMS.registerSimpleItem("incense_bundle",new Item.Properties().stacksTo(32));
 public static final DeferredItem<SleepJournalItem> SLEEP_JOURNAL=
   ITEMS.register("sleep_journal",()->new SleepJournalItem(new Item.Properties().stacksTo(1)));
 public static final DeferredItem<HerbalPoulticeItem> HERBAL_POULTICE=
   ITEMS.register("herbal_poultice",()->new HerbalPoulticeItem(new Item.Properties().stacksTo(16)));
 public static final DeferredItem<Item> PILLOW=ITEMS.registerSimpleItem("pillow",new Item.Properties().stacksTo(16));
 public static final DeferredItem<Item> MASK=ITEMS.registerSimpleItem("sleep_mask",new Item.Properties().stacksTo(1));
 public static final DeferredItem<BlockItem> INCENSE_BURNER=ITEMS.register(
  "incense_burner",()->new BlockItem(ModBlocks.INCENSE_BURNER.get(),new Item.Properties()));
 public static final DeferredItem<BlockItem> COFFEE_MACHINE=ITEMS.register("coffee_machine",()->new BlockItem(ModBlocks.COFFEE_MACHINE.get(),new Item.Properties()));
 public static final DeferredItem<BlockItem> BEDROLL=ITEMS.register("camp_bedroll",()->new BlockItem(ModBlocks.CAMP_BEDROLL.get(),new Item.Properties()));
 public static final DeferredItem<BlockItem> AWNING=ITEMS.register("camp_awning",()->new BlockItem(ModBlocks.CAMP_AWNING.get(),new Item.Properties()));
 public static final DeferredItem<BlockItem> CAMP_LANTERN=ITEMS.register("camp_lantern",()->new BlockItem(ModBlocks.CAMP_LANTERN.get(),new Item.Properties()));
 public static final DeferredItem<BlockItem> DREAMCATCHER=ITEMS.register("dreamcatcher",()->new BlockItem(ModBlocks.DREAMCATCHER.get(),new Item.Properties()));
 public static final DeferredItem<SpawnEggItem> SLEEPLESS_EGG=ITEMS.register("sleepless_spawn_egg",
  ()->new SpawnEggItem(ModEntities.SLEEPLESS.get(),0x26232F,0xBB8498,new Item.Properties()));
 private ModItems(){}
}

