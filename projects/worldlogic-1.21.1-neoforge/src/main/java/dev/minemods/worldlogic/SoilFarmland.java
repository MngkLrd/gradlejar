package dev.minemods.worldlogic;
import java.util.*;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
/** Seven tillable soil identities use the real vanilla FarmBlock lifecycle. */
public final class SoilFarmland{
 private SoilFarmland(){}
 public static final List<String> IDS=List.of("humus","loam","peat","alluvial_silt","chernozem","sandy_loam","laterite");
 private static final Map<String,Supplier<Block>> FARMS=new LinkedHashMap<>();
 public static final class Tilled extends FarmBlock{
  private final String soil;
  Tilled(String soil){super(BlockBehaviour.Properties.ofFullCopy(Blocks.FARMLAND));this.soil=soil;}
  public String soil(){return soil;}
  public BlockState parent(){return Content.SOILS.get(soil).get().defaultBlockState();}
 }
 static{
  for(String id:IDS){
   var b=Content.BLOCKS.register(id+"_farmland",()->new Tilled(id));FARMS.put(id,b::get);
   Content.ITEMS.register(id+"_farmland",()->new BlockItem(b.get(),new Item.Properties()));
  }
 }
 static void register(){}
 static Block block(String id){return FARMS.get(id).get();}
 public static BlockState original(BlockState s){return s.getBlock() instanceof Tilled t?t.parent():s;}
 static BlockState tilled(BlockState ground,BlockState under){
  BlockState candidate=ground;
  if(ground.is(Blocks.GRASS_BLOCK)||ground.is(Blocks.DIRT)||ground.is(Blocks.PODZOL))candidate=under;
  for(String id:IDS)if(candidate.is(Content.SOILS.get(id).get()))return block(id).defaultBlockState();
  return null;
 }
 public static void till(BlockEvent.BlockToolModificationEvent e){
  if(e.isCanceled()||e.getItemAbility()!=ItemAbilities.HOE_TILL||!HoeItem.onlyIfAirAbove(e.getContext()))return;
  if(!e.getContext().getItemInHand().canPerformAction(ItemAbilities.HOE_TILL))return;
  var level=e.getContext().getLevel();var p=e.getContext().getClickedPos();
  if(p.getY()<=level.getMinBuildHeight())return;
  var result=tilled(e.getState(),level.getBlockState(p.below()));
  if(result!=null)e.setFinalState(result);
 }
 public static void creative(BuildCreativeModeTabContentsEvent e){
  if(e.getTabKey().equals(CreativeModeTabs.NATURAL_BLOCKS))for(var b:FARMS.values())e.accept(b.get());
 }
}
