package dev.minemods.worldlogic;
import java.util.*;
import java.util.function.Supplier;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
public final class MineralContent {
 private MineralContent(){}
 public static final List<String> IDS=List.of("hematite","magnetite","cassiterite","galena","sphalerite","pyrite","malachite","bauxite","fluorite","cinnabar");
 private record Family(Supplier<Block> stone,Supplier<Block> deep,Supplier<Item> sample){}
 private static final Map<GeologyModel.Ore,Family> FAMILIES=new EnumMap<>(GeologyModel.Ore.class);
 static{
  for(String id:IDS){
   var ore=GeologyModel.Ore.valueOf(id.toUpperCase(Locale.ROOT));
   float hardness=Set.of("galena","bauxite","cinnabar","malachite").contains(id)?2.2f:3f;
   var stone=Content.BLOCKS.register(id+"_ore",()->new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_ORE).strength(hardness,3).requiresCorrectToolForDrops()));
   var deep=Content.BLOCKS.register("deepslate_"+id+"_ore",()->new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE_IRON_ORE).strength(hardness+1.5f,6).requiresCorrectToolForDrops()));
   Content.ITEMS.register(id+"_ore",()->new BlockItem(stone.get(),new Item.Properties()));
   Content.ITEMS.register("deepslate_"+id+"_ore",()->new BlockItem(deep.get(),new Item.Properties()));
   var sample=Content.ITEMS.register(id,()->new Item(new Item.Properties()));
   FAMILIES.put(ore,new Family(stone,deep,sample));
  }
 }
 static void register(){}
 public static BlockState ore(GeologyModel.Ore ore,boolean deep){var f=FAMILIES.get(ore);return (deep?f.deep():f.stone()).get().defaultBlockState();}
 public static Item specimen(GeologyModel.Ore ore){return FAMILIES.get(ore).sample().get();}
 public static GeologyModel.Ore type(BlockState state){
  for(var e:FAMILIES.entrySet())if(state.is(e.getValue().stone().get())||state.is(e.getValue().deep().get()))return e.getKey();
  return null;
 }
 public static void creative(BuildCreativeModeTabContentsEvent e){
  if(e.getTabKey().equals(CreativeModeTabs.NATURAL_BLOCKS))for(var f:FAMILIES.values()){e.accept(f.stone().get());e.accept(f.deep().get());}
  if(e.getTabKey().equals(CreativeModeTabs.INGREDIENTS))for(var f:FAMILIES.values())e.accept(f.sample().get());
 }
}
