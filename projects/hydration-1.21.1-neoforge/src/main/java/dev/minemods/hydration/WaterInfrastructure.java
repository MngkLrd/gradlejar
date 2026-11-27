package dev.minemods.hydration;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

public final class WaterInfrastructure {
 private static final DeferredRegister.Blocks BLOCKS=DeferredRegister.createBlocks("hydration");
 private static final DeferredRegister.Items ITEMS=DeferredRegister.createItems("hydration");
 private static final DeferredRegister<BlockEntityType<?>> ENTITIES=DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE,"hydration");
 private static DeferredBlock<TankBlock> tank(String id,TankBlock.Kind kind,net.minecraft.world.level.block.Block copy){return BLOCKS.register(id,()->new TankBlock(kind,BlockBehaviour.Properties.ofFullCopy(copy).noOcclusion().pushReaction(PushReaction.BLOCK)));}
 public static final DeferredBlock<TankBlock> BASIN=tank("clay_basin",TankBlock.Kind.BASIN,Blocks.TERRACOTTA);
 public static final DeferredBlock<TankBlock> BARREL=tank("water_barrel",TankBlock.Kind.BARREL,Blocks.OAK_PLANKS);
 public static final DeferredBlock<TankBlock> COLLECTOR=tank("rain_collector",TankBlock.Kind.COLLECTOR,Blocks.OAK_PLANKS);
 public static final DeferredBlock<TankBlock> FILTER=tank("stationary_filter",TankBlock.Kind.FILTER,Blocks.TERRACOTTA);
 public static final DeferredBlock<TankBlock> INTAKE=tank("water_intake",TankBlock.Kind.INTAKE,Blocks.TERRACOTTA);
 public static final DeferredBlock<ClayPipeBlock> PIPE=BLOCKS.register("clay_pipe",()->new ClayPipeBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.TERRACOTTA).noOcclusion()));
 public static final Supplier<BlockEntityType<TankEntity>> TANK_ENTITY=ENTITIES.register("tank",()->BlockEntityType.Builder.of(TankEntity::new,BASIN.get(),BARREL.get(),COLLECTOR.get(),FILTER.get(),INTAKE.get()).build(null));
 private static DeferredItem<BlockItem> item(String id,DeferredBlock<?> b){return ITEMS.register(id,()->new BlockItem(b.get(),new Item.Properties()));}
 public static final DeferredItem<BlockItem> BASIN_ITEM=item("clay_basin",BASIN),BARREL_ITEM=item("water_barrel",BARREL),COLLECTOR_ITEM=item("rain_collector",COLLECTOR),FILTER_ITEM=item("stationary_filter",FILTER),INTAKE_ITEM=item("water_intake",INTAKE),PIPE_ITEM=item("clay_pipe",PIPE);
 public static List<DeferredItem<BlockItem>> items(){return List.of(BASIN_ITEM,BARREL_ITEM,COLLECTOR_ITEM,FILTER_ITEM,INTAKE_ITEM,PIPE_ITEM);}
 public static void register(IEventBus bus){BLOCKS.register(bus);ITEMS.register(bus);ENTITIES.register(bus);}
 public static void interact(PlayerInteractEvent.RightClickBlock e){
  if(e.getHand()!=InteractionHand.MAIN_HAND||e.getEntity().isSpectator())return;
  var state=e.getLevel().getBlockState(e.getPos());
  if(state.getBlock() instanceof ClayPipeBlock&&e.getItemStack().isEmpty()&&e.getEntity().getOffhandItem().isEmpty()){
   if(e.getEntity() instanceof ServerPlayer p)p.displayClientMessage(Component.translatable("hydration.pipe.direction",state.getValue(ClayPipeBlock.OUT).getName(),e.getLevel().hasNeighborSignal(e.getPos())?"OFF":"ON"),true);
   e.setCancellationResult(InteractionResult.SUCCESS);e.setCanceled(true);return;
  }
  if(!(state.getBlock() instanceof TankBlock))return;
  var stack=e.getItemStack();boolean empty=stack.isEmpty()&&e.getEntity().getOffhandItem().isEmpty();
  boolean vessel=stack.getItem() instanceof VesselItem, cartridge=stack.is(HydrationItems.FILTER.get());
  if(!empty&&!vessel&&!cartridge)return;
  e.setCancellationResult(InteractionResult.SUCCESS);e.setCanceled(true);
  if(!(e.getEntity() instanceof ServerPlayer p)||!(e.getLevel().getBlockEntity(e.getPos()) instanceof TankEntity tank))return;
  if(vessel&&stack.getItem() instanceof VesselItem v){
   var before=v.read(stack);
   var after=p.isShiftKeyDown()?tank.insert(before):tank.extract(before,v.capacity,v.capacity-before.ml());
   v.write(stack,after);p.getInventory().setChanged();
  }else if(cartridge&&tank.installFilter()){
   if(!p.isCreative())stack.shrink(1);p.getInventory().setChanged();
  }else if(empty&&p.isShiftKeyDown()&&Hydration.active(p)&&WaterBalance.canDrink(Hydration.state(p))){
   var b=tank.extract(WaterBatch.empty(),250,250);
   if(b.ml()>0)Hydration.applyDrink(p,b.quality(),b.ml(),b.risk());
  }
  var b=tank.water();
  p.displayClientMessage(Component.translatable("hydration.tank.status",b.ml(),tank.capacity(),Component.translatable("hydration.water.quality."+b.quality().name().toLowerCase(Locale.ROOT)),tank.charge()),true);
 }
}
