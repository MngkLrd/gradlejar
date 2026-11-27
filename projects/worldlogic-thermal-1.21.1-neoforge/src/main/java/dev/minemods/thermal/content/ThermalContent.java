package dev.minemods.thermal.content;
import dev.minemods.thermal.ThermalMod;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.*;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import java.util.function.Supplier;
public final class ThermalContent {
 private static final DeferredRegister.Items ITEMS=DeferredRegister.createItems(ThermalMod.ID);
 private static final DeferredRegister.Blocks BLOCKS=DeferredRegister.createBlocks(ThermalMod.ID);
 private static final DeferredRegister<BlockEntityType<?>> ENTITIES=DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE,ThermalMod.ID);
 public static final DeferredItem<Item> RAW_FIBER=ITEMS.register("raw_fiber_bundle",()->new Item(new Item.Properties().stacksTo(16)));
 public static final DeferredItem<Item> THREAD=ITEMS.register("plant_thread",()->new Item(new Item.Properties()));
 public static final DeferredItem<Item> MAT=ITEMS.register("woven_fiber",()->new Item(new Item.Properties()));
 public static final DeferredItem<Item> SCRAPER=ITEMS.register("stone_scraper",()->new Item(new Item.Properties().durability(96)));
 public static final DeferredBlock<FiberWorkpieceBlock> WORKPIECE=BLOCKS.register("fiber_workpiece",()->new FiberWorkpieceBlock(BlockBehaviour.Properties.of().strength(-1,6).noCollission().noOcclusion().noLootTable().pushReaction(PushReaction.BLOCK).sound(SoundType.GRASS)));
 public static final Supplier<BlockEntityType<FiberWorkpieceEntity>> WORKPIECE_ENTITY=ENTITIES.register("fiber_workpiece",()->BlockEntityType.Builder.of(FiberWorkpieceEntity::new,WORKPIECE.get()).build(null));
 private static DeferredItem<ArmorItem> clothing(String name,ArmorItem.Type type,int life){
  return ITEMS.register(name,()->new ArmorItem(ArmorMaterials.LEATHER,type,new Item.Properties().durability(type.getDurability(life))));
 }
 public static final DeferredItem<Item> FIBER=ITEMS.register("dry_fiber",()->new Item(new Item.Properties()));
 public static final DeferredItem<ArmorItem> GRASS_HOOD=clothing("grass_hood",ArmorItem.Type.HELMET,3);
 public static final DeferredItem<ArmorItem> GRASS_COAT=clothing("grass_coat",ArmorItem.Type.CHESTPLATE,3);
 public static final DeferredItem<ArmorItem> GRASS_LEGS=clothing("grass_leggings",ArmorItem.Type.LEGGINGS,3);
 public static final DeferredItem<ArmorItem> GRASS_BOOTS=clothing("grass_boots",ArmorItem.Type.BOOTS,3);
 public static final DeferredItem<ArmorItem> SUN_HAT=clothing("sun_hat",ArmorItem.Type.HELMET,4);
 public static final DeferredItem<ArmorItem> LIGHT_TUNIC=clothing("light_tunic",ArmorItem.Type.CHESTPLATE,4);
 public static final DeferredBlock<FixtureBlock> HEARTH=BLOCKS.register("clay_hearth",()->new FixtureBlock(false,BlockBehaviour.Properties.ofFullCopy(Blocks.BRICKS).lightLevel(s->s.getValue(FixtureBlock.ACTIVE)?12:0)));
 public static final DeferredBlock<FixtureBlock> COOLER=BLOCKS.register("evaporative_cooler",()->new FixtureBlock(true,BlockBehaviour.Properties.ofFullCopy(Blocks.TERRACOTTA)));
 public static final DeferredItem<BlockItem> HEARTH_ITEM=ITEMS.register("clay_hearth",()->new BlockItem(HEARTH.get(),new Item.Properties()));
 public static final DeferredItem<BlockItem> COOLER_ITEM=ITEMS.register("evaporative_cooler",()->new BlockItem(COOLER.get(),new Item.Properties()));
 public static void register(IEventBus bus){BLOCKS.register(bus);ITEMS.register(bus);ENTITIES.register(bus);bus.addListener(ThermalContent::creative);}
 private static void creative(BuildCreativeModeTabContentsEvent e){
  if(e.getTabKey()==CreativeModeTabs.TOOLS_AND_UTILITIES)for(Supplier<? extends Item> i:java.util.List.of(RAW_FIBER,FIBER,THREAD,MAT,SCRAPER,GRASS_HOOD,GRASS_COAT,GRASS_LEGS,GRASS_BOOTS,SUN_HAT,LIGHT_TUNIC,HEARTH_ITEM,COOLER_ITEM))e.accept(i.get());
 }
}
