package dev.minemods.worldlogic;
import java.util.*;
import java.util.function.Supplier;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
/** Core supplies ore and raw mineral IDs, not downstream processing. */
public final class MineralContent{
 private MineralContent(){}
 public record Family(Supplier<Block> ore,Supplier<Item> raw){}
 private static final Map<MineralDeposits.Mineral,Family> FAMILIES=new EnumMap<>(MineralDeposits.Mineral.class);
 static{
  for(var m:MineralDeposits.Mineral.values()){
   var ore=Content.BLOCKS.register(m.id()+"_ore",()->new MineralOreBlock(m,BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_ORE).strength(3,3).requiresCorrectToolForDrops()));
   var raw=Content.ITEMS.register("raw_"+m.id(),()->new Item(new Item.Properties()));
   Content.ITEMS.register(m.id()+"_ore",()->new BlockItem(ore.get(),new Item.Properties()));
   FAMILIES.put(m,new Family(ore,raw));
  }
 }
 public static void register(){}
 public static Family get(MineralDeposits.Mineral mineral){return FAMILIES.get(mineral);}
 public static BlockState state(MineralDeposits.Mineral mineral,int grade){return get(mineral).ore().get().defaultBlockState().setValue(MineralOreBlock.RICHNESS,grade);}
}
