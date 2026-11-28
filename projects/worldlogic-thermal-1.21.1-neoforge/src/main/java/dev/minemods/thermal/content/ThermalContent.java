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
import net.minecraft.network.chat.Component;
import java.util.List;
import java.util.function.Supplier;
public final class ThermalContent {
 private static final DeferredRegister.Items ITEMS=DeferredRegister.createItems(ThermalMod.ID);
 private static final DeferredRegister.Blocks BLOCKS=DeferredRegister.createBlocks(ThermalMod.ID);
 private static final DeferredRegister<CreativeModeTab> TABS=DeferredRegister.create(Registries.CREATIVE_MODE_TAB,ThermalMod.ID);
 private static final DeferredRegister<BlockEntityType<?>> ENTITIES=DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE,ThermalMod.ID);
 public static final DeferredItem<Item> RAW_FIBER=ITEMS.register("raw_fiber_bundle",()->new ThermalInfoItem(new Item.Properties().stacksTo(16)));
 public static final DeferredItem<Item> THREAD=ITEMS.register("plant_thread",()->new ThermalInfoItem(new Item.Properties()));
 public static final DeferredItem<Item> MAT=ITEMS.register("woven_fiber",()->new ThermalInfoItem(new Item.Properties()));
 public static final DeferredItem<Item> SCRAPER=ITEMS.register("stone_scraper",()->new ThermalInfoItem(new Item.Properties().durability(96)));
 public static final DeferredBlock<FiberWorkpieceBlock> WORKPIECE=BLOCKS.register("fiber_workpiece",()->new FiberWorkpieceBlock(BlockBehaviour.Properties.of().strength(-1,6).noCollission().noOcclusion().noLootTable().pushReaction(PushReaction.BLOCK).sound(SoundType.GRASS)));
 public static final Supplier<BlockEntityType<FiberWorkpieceEntity>> WORKPIECE_ENTITY=ENTITIES.register("fiber_workpiece",()->BlockEntityType.Builder.of(FiberWorkpieceEntity::new,WORKPIECE.get()).build(null));
 private static DeferredItem<ArmorItem> clothing(String name,ArmorItem.Type type,int life){
  boolean summer=name.equals("sun_hat")||name.equals("light_tunic");
  return ITEMS.register(name,()->new ClimateGarment(type,life,0xffffff,new ClimateGarment.Profile(summer?.02:.25,summer?.45:0,0,0,0)));
 }
 public static final DeferredItem<Item> FIBER=ITEMS.register("dry_fiber",()->new ThermalInfoItem(new Item.Properties()));
 public static final DeferredItem<ArmorItem> GRASS_HOOD=clothing("grass_hood",ArmorItem.Type.HELMET,3);
 public static final DeferredItem<ArmorItem> GRASS_COAT=clothing("grass_coat",ArmorItem.Type.CHESTPLATE,3);
 public static final DeferredItem<ArmorItem> GRASS_LEGS=clothing("grass_leggings",ArmorItem.Type.LEGGINGS,3);
 public static final DeferredItem<ArmorItem> GRASS_BOOTS=clothing("grass_boots",ArmorItem.Type.BOOTS,3);
 public static final DeferredItem<ArmorItem> SUN_HAT=clothing("sun_hat",ArmorItem.Type.HELMET,4);
 public static final DeferredItem<ArmorItem> LIGHT_TUNIC=clothing("light_tunic",ArmorItem.Type.CHESTPLATE,4);
 public static final DeferredBlock<FixtureBlock> HEARTH=BLOCKS.register("clay_hearth",()->new FixtureBlock(false,BlockBehaviour.Properties.ofFullCopy(Blocks.BRICKS).lightLevel(s->s.getValue(FixtureBlock.ACTIVE)?12:0)));
 public static final DeferredBlock<FixtureBlock> COOLER=BLOCKS.register("evaporative_cooler",()->new FixtureBlock(true,BlockBehaviour.Properties.ofFullCopy(Blocks.TERRACOTTA)));
 public static final DeferredItem<BlockItem> HEARTH_ITEM=ITEMS.register("clay_hearth",()->new ThermalBlockItem(HEARTH.get(),new Item.Properties()));
 public static final DeferredItem<BlockItem> COOLER_ITEM=ITEMS.register("evaporative_cooler",()->new ThermalBlockItem(COOLER.get(),new Item.Properties()));
 public static final DeferredItem<ClimateGarment> FUR_HAT=ITEMS.register("fur_hat",()->new ClimateGarment(ArmorItem.Type.HELMET,9,0x76513b,new ClimateGarment.Profile(0.34,0,0.12,0.2,0)));
 public static final DeferredItem<ClimateGarment> FUR_COAT=ITEMS.register("fur_coat",()->new ClimateGarment(ArmorItem.Type.CHESTPLATE,11,0x795b43,new ClimateGarment.Profile(0.42,0,0.18,0.24,0)));
 public static final DeferredItem<ClimateGarment> FUR_BOOTS=ITEMS.register("fur_boots",()->new ClimateGarment(ArmorItem.Type.BOOTS,10,0x67513a,new ClimateGarment.Profile(0.36,0,0.15,0.12,0)));
 public static final DeferredItem<ClimateGarment> WOOL_UNDERWEAR=ITEMS.register("wool_underwear",()->new ClimateGarment(ArmorItem.Type.LEGGINGS,6,0xc3b9a2,new ClimateGarment.Profile(0.3,0,0,0.05,0)));
 public static final DeferredItem<ClimateGarment> WAXED_CLOAK=ITEMS.register("waxed_cloak",()->new ClimateGarment(ArmorItem.Type.CHESTPLATE,9,0x827a4c,new ClimateGarment.Profile(0.18,0,0.8,0.25,0.12)));
 public static final DeferredItem<ClimateGarment> FACE_MASK=ITEMS.register("leather_face_mask",()->new ClimateGarment(ArmorItem.Type.HELMET,7,0x855b39,new ClimateGarment.Profile(0.16,0,0.12,0.55,0)));
 public static final DeferredItem<ClimateGarment> LINEN_CLOTHES=ITEMS.register("linen_clothes",()->new ClimateGarment(ArmorItem.Type.CHESTPLATE,6,0xd7cbb0,new ClimateGarment.Profile(0.03,0.62,0,0,0.12)));
 public static final DeferredItem<ClimateGarment> DESERT_WRAP=ITEMS.register("desert_headwear",()->new ClimateGarment(ArmorItem.Type.HELMET,6,0xe5d6b1,new ClimateGarment.Profile(0.04,0.45,0,0.1,0.65)));
 public static final DeferredItem<ClimateGarment> STRAW_CAPE=ITEMS.register("straw_cape",()->new ClimateGarment(ArmorItem.Type.CHESTPLATE,4,0xb5a05c,new ClimateGarment.Profile(0.09,0.38,0.35,0.08,0.28)));
 public static final DeferredItem<ThermalPackItem> HOT_STONE=ITEMS.register("hot_stone",()->new ThermalPackItem(ThermalPackItem.Kind.STONE));
 public static final DeferredItem<ThermalPackItem> LEATHER_POUCH=ITEMS.register("leather_warmer",()->new ThermalPackItem(ThermalPackItem.Kind.POUCH));
 public static final DeferredItem<ThermalPackItem> CHARCOAL_WARMER=ITEMS.register("charcoal_warmer",()->new ThermalPackItem(ThermalPackItem.Kind.CHARCOAL));
 public static final DeferredItem<ThermalPackItem> HAND_FAN=ITEMS.register("hand_fan",()->new ThermalPackItem(ThermalPackItem.Kind.FAN));
 public static final DeferredItem<ThermalPackItem> WET_CLOTH=ITEMS.register("wet_cloth",()->new ThermalPackItem(ThermalPackItem.Kind.CLOTH));
 public static final DeferredItem<ThermalPackItem> INSULATED_FLASK=ITEMS.register("insulated_flask",()->new ThermalPackItem(ThermalPackItem.Kind.FLASK));
 public static final DeferredItem<Item> OIL=ITEMS.register("plant_oil",()->new ThermalInfoItem(new Item.Properties()));
 public static final DeferredItem<Item> COMBED_WOOL=ITEMS.register("combed_wool",()->new ThermalInfoItem(new Item.Properties()));
 public static final DeferredItem<Item> PREPARED_HIDE=ITEMS.register("prepared_hide",()->new ThermalInfoItem(new Item.Properties()));
 public static final DeferredItem<Item> FELT=ITEMS.register("felt",()->new ThermalInfoItem(new Item.Properties()));
 public static final DeferredItem<TextileToolItem> SPINDLE=ITEMS.register("spindle",()->new TextileToolItem(TextileToolItem.Kind.SPINDLE));
 public static final DeferredItem<TextileToolItem> BONE_NEEDLE=ITEMS.register("bone_needle",()->new TextileToolItem(TextileToolItem.Kind.NEEDLE));
 public static final DeferredItem<TextileToolItem> WOOL_COMB=ITEMS.register("wool_comb",()->new TextileToolItem(TextileToolItem.Kind.COMB));
 public static final DeferredBlock<ClimateDeviceBlock> OIL_LAMP=BLOCKS.register("oil_lamp",()->new ClimateDeviceBlock(ClimateDeviceBlock.Kind.LAMP,BlockBehaviour.Properties.ofFullCopy(Blocks.TERRACOTTA).noOcclusion().lightLevel(s->s.getValue(ClimateDeviceBlock.PHASE)==1?8:s.getValue(ClimateDeviceBlock.PHASE)==2?2:0)));
 public static final DeferredBlock<ClimateDeviceBlock> BRAZIER=BLOCKS.register("clay_brazier",()->new ClimateDeviceBlock(ClimateDeviceBlock.Kind.BRAZIER,BlockBehaviour.Properties.ofFullCopy(Blocks.TERRACOTTA).noOcclusion().lightLevel(s->s.getValue(ClimateDeviceBlock.PHASE)==1?12:s.getValue(ClimateDeviceBlock.PHASE)==2?4:0)));
 public static final DeferredBlock<ClimateDeviceBlock> STONE_STOVE=BLOCKS.register("stone_stove",()->new ClimateDeviceBlock(ClimateDeviceBlock.Kind.STOVE,BlockBehaviour.Properties.ofFullCopy(Blocks.STONE_BRICKS).noOcclusion().lightLevel(s->s.getValue(ClimateDeviceBlock.PHASE)==1?12:s.getValue(ClimateDeviceBlock.PHASE)==2?4:0)));
 public static final DeferredBlock<ClimateDeviceBlock> FIREPLACE=BLOCKS.register("brick_fireplace",()->new ClimateDeviceBlock(ClimateDeviceBlock.Kind.FIREPLACE,BlockBehaviour.Properties.ofFullCopy(Blocks.BRICKS).noOcclusion().lightLevel(s->s.getValue(ClimateDeviceBlock.PHASE)==1?14:s.getValue(ClimateDeviceBlock.PHASE)==2?4:0)));
 public static final DeferredBlock<ClimateDeviceBlock> EVAP_WALL=BLOCKS.register("evaporative_wall",()->new ClimateDeviceBlock(ClimateDeviceBlock.Kind.EVAP_WALL,BlockBehaviour.Properties.ofFullCopy(Blocks.TERRACOTTA).noOcclusion().lightLevel(s->s.getValue(ClimateDeviceBlock.PHASE)==1?0:s.getValue(ClimateDeviceBlock.PHASE)==2?0:0)));
 public static final DeferredBlock<ClimateDeviceBlock> ICE_BOX=BLOCKS.register("ice_cooler",()->new ClimateDeviceBlock(ClimateDeviceBlock.Kind.ICE_BOX,BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS).noOcclusion().lightLevel(s->s.getValue(ClimateDeviceBlock.PHASE)==1?0:s.getValue(ClimateDeviceBlock.PHASE)==2?0:0)));
 public static final DeferredBlock<ClimateDeviceBlock> WIND_TOWER=BLOCKS.register("wind_tower",()->new ClimateDeviceBlock(ClimateDeviceBlock.Kind.WIND_TOWER,BlockBehaviour.Properties.ofFullCopy(Blocks.SANDSTONE).noOcclusion().lightLevel(s->s.getValue(ClimateDeviceBlock.PHASE)==1?0:s.getValue(ClimateDeviceBlock.PHASE)==2?0:0)));
 public static final DeferredBlock<TextileStationBlock> LOOM=BLOCKS.register("textile_loom",()->new TextileStationBlock(TextileStationBlock.Kind.LOOM,BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS).noOcclusion()));
 public static final DeferredBlock<TextileStationBlock> RACK=BLOCKS.register("drying_rack",()->new TextileStationBlock(TextileStationBlock.Kind.RACK,BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS).noOcclusion()));
 public static final DeferredBlock<TextileStationBlock> HIDE_FRAME=BLOCKS.register("hide_frame",()->new TextileStationBlock(TextileStationBlock.Kind.HIDE,BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS).noOcclusion()));
 public static final DeferredBlock<TextileStationBlock> FELT_BOARD=BLOCKS.register("felting_board",()->new TextileStationBlock(TextileStationBlock.Kind.FELT,BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS).noOcclusion()));
 public static final DeferredBlock<VentBlock> CHIMNEY=BLOCKS.register("chimney",()->new VentBlock(VentBlock.Kind.CHIMNEY,BlockBehaviour.Properties.ofFullCopy(Blocks.BRICKS).noOcclusion()));
 public static final DeferredBlock<VentBlock> DAMPER=BLOCKS.register("thermal_damper",()->new VentBlock(VentBlock.Kind.DAMPER,BlockBehaviour.Properties.ofFullCopy(Blocks.STONE_BRICKS).noOcclusion()));
 public static final DeferredBlock<VentBlock> CANOPY=BLOCKS.register("woven_canopy",()->new VentBlock(VentBlock.Kind.CANOPY,BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS).noOcclusion()));
 public static final DeferredItem<BlockItem> OIL_LAMP_ITEM=ITEMS.register("oil_lamp",()->new ThermalBlockItem(OIL_LAMP.get(),new Item.Properties()));
 public static final DeferredItem<BlockItem> BRAZIER_ITEM=ITEMS.register("clay_brazier",()->new ThermalBlockItem(BRAZIER.get(),new Item.Properties()));
 public static final DeferredItem<BlockItem> STONE_STOVE_ITEM=ITEMS.register("stone_stove",()->new ThermalBlockItem(STONE_STOVE.get(),new Item.Properties()));
 public static final DeferredItem<BlockItem> FIREPLACE_ITEM=ITEMS.register("brick_fireplace",()->new ThermalBlockItem(FIREPLACE.get(),new Item.Properties()));
 public static final DeferredItem<BlockItem> EVAP_WALL_ITEM=ITEMS.register("evaporative_wall",()->new ThermalBlockItem(EVAP_WALL.get(),new Item.Properties()));
 public static final DeferredItem<BlockItem> ICE_BOX_ITEM=ITEMS.register("ice_cooler",()->new ThermalBlockItem(ICE_BOX.get(),new Item.Properties()));
 public static final DeferredItem<BlockItem> WIND_TOWER_ITEM=ITEMS.register("wind_tower",()->new ThermalBlockItem(WIND_TOWER.get(),new Item.Properties()));
 public static final DeferredItem<BlockItem> LOOM_ITEM=ITEMS.register("textile_loom",()->new ThermalBlockItem(LOOM.get(),new Item.Properties()));
 public static final DeferredItem<BlockItem> RACK_ITEM=ITEMS.register("drying_rack",()->new ThermalBlockItem(RACK.get(),new Item.Properties()));
 public static final DeferredItem<BlockItem> HIDE_FRAME_ITEM=ITEMS.register("hide_frame",()->new ThermalBlockItem(HIDE_FRAME.get(),new Item.Properties()));
 public static final DeferredItem<BlockItem> FELT_BOARD_ITEM=ITEMS.register("felting_board",()->new ThermalBlockItem(FELT_BOARD.get(),new Item.Properties()));
 public static final DeferredItem<BlockItem> CHIMNEY_ITEM=ITEMS.register("chimney",()->new ThermalBlockItem(CHIMNEY.get(),new Item.Properties()));
 public static final DeferredItem<BlockItem> DAMPER_ITEM=ITEMS.register("thermal_damper",()->new ThermalBlockItem(DAMPER.get(),new Item.Properties()));
 public static final DeferredItem<BlockItem> CANOPY_ITEM=ITEMS.register("woven_canopy",()->new ThermalBlockItem(CANOPY.get(),new Item.Properties()));
 public static final DeferredBlock<WallInsulationBlock> CLAY_INSULATION=BLOCKS.register("clay_insulation",()->new WallInsulationBlock(WallInsulationBlock.Kind.CLAY,BlockBehaviour.Properties.ofFullCopy(Blocks.PACKED_MUD).strength(.6f).noOcclusion().pushReaction(PushReaction.DESTROY)));
 public static final DeferredBlock<WallInsulationBlock> STRAW_INSULATION=BLOCKS.register("straw_insulation",()->new WallInsulationBlock(WallInsulationBlock.Kind.STRAW,BlockBehaviour.Properties.ofFullCopy(Blocks.HAY_BLOCK).strength(.35f).noOcclusion().pushReaction(PushReaction.DESTROY)));
 public static final DeferredItem<BlockItem> CLAY_INSULATION_ITEM=ITEMS.register("clay_insulation",()->new ThermalBlockItem(CLAY_INSULATION.get(),new Item.Properties()));
 public static final DeferredItem<BlockItem> STRAW_INSULATION_ITEM=ITEMS.register("straw_insulation",()->new ThermalBlockItem(STRAW_INSULATION.get(),new Item.Properties()));
 public static List<Item> creativeItems(){
  return List.<Supplier<? extends Item>>of(RAW_FIBER,FIBER,THREAD,MAT,SCRAPER,GRASS_HOOD,GRASS_COAT,GRASS_LEGS,GRASS_BOOTS,SUN_HAT,LIGHT_TUNIC,HEARTH_ITEM,COOLER_ITEM,FUR_HAT,FUR_COAT,FUR_BOOTS,WOOL_UNDERWEAR,WAXED_CLOAK,FACE_MASK,LINEN_CLOTHES,DESERT_WRAP,STRAW_CAPE,HOT_STONE,LEATHER_POUCH,CHARCOAL_WARMER,HAND_FAN,WET_CLOTH,INSULATED_FLASK,OIL,COMBED_WOOL,PREPARED_HIDE,FELT,SPINDLE,BONE_NEEDLE,WOOL_COMB,OIL_LAMP_ITEM,BRAZIER_ITEM,STONE_STOVE_ITEM,FIREPLACE_ITEM,EVAP_WALL_ITEM,ICE_BOX_ITEM,WIND_TOWER_ITEM,LOOM_ITEM,RACK_ITEM,HIDE_FRAME_ITEM,FELT_BOARD_ITEM,CHIMNEY_ITEM,DAMPER_ITEM,CANOPY_ITEM,CLAY_INSULATION_ITEM,STRAW_INSULATION_ITEM).stream().map(s->(Item)s.get()).toList();
 }
 public static final Supplier<CreativeModeTab> TAB=TABS.register("temperature",()->CreativeModeTab.builder()
  .title(Component.translatable("itemGroup.worldlogic_thermal.temperature"))
  .icon(()->new ItemStack(HEARTH_ITEM.get()))
  .displayItems((parameters,output)->creativeItems().forEach(output::accept)).build());
 public static void register(IEventBus bus){BLOCKS.register(bus);ITEMS.register(bus);ENTITIES.register(bus);TABS.register(bus);}
}
