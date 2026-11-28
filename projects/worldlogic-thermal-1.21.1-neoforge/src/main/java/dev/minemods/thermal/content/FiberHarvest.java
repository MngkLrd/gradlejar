package dev.minemods.thermal.content;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
/** Bare-handed fiber collection before metal tools. Only tagged plants, never general blocks. */
public final class FiberHarvest {
 public static final TagKey<Block> PLANTS=TagKey.create(Registries.BLOCK,ResourceLocation.fromNamespaceAndPath("worldlogic_thermal","fiber_plants"));
 private FiberHarvest(){}
 public static boolean canHarvest(BlockState s){return s.is(PLANTS);}
 public static void onClick(PlayerInteractEvent.RightClickBlock e){
  var p=e.getEntity();
  if(p.isCreative()||p.isSpectator()||e.getHand()!=InteractionHand.MAIN_HAND||!e.getItemStack().isEmpty()||!canHarvest(e.getLevel().getBlockState(e.getPos())))return;
  if(e.getLevel() instanceof ServerLevel level){
    if(!level.mayInteract(p,e.getPos())||!harvest(level,e.getPos(),p))return;
  }
  e.setCancellationResult(InteractionResult.SUCCESS);e.setCanceled(true);
 }
 public static boolean harvest(ServerLevel level,BlockPos pos,Player p){
  if(!level.hasChunkAt(pos))return false;
  var state=level.getBlockState(pos);
  if(!canHarvest(state)||!HarvestPermission.allowed(level,pos,p,state))return false;
  if(!level.destroyBlock(pos,false,p))return false;
  HarvestPermission.accepted(p,.15);
  Block.popResource(level,pos,new ItemStack(ThermalContent.RAW_FIBER.get(),1));return true;
 }
}
