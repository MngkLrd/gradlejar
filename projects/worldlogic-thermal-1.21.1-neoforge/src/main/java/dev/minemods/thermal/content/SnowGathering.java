package dev.minemods.thermal.content;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
/** Emergency snow packing: no tools, no invented resource, no powder snow conversion. */
public final class SnowGathering {
 private SnowGathering(){}
 public static void onClick(PlayerInteractEvent.RightClickBlock e){
  var p=e.getEntity();var s=e.getLevel().getBlockState(e.getPos());
  if(e.getHand()!=InteractionHand.MAIN_HAND||!e.getItemStack().isEmpty()||!p.isShiftKeyDown()||p.isCreative()||p.isSpectator()||!(s.is(Blocks.SNOW)||s.is(Blocks.SNOW_BLOCK)))return;
  if(e.getLevel() instanceof ServerLevel l&&!gather(l,e.getPos(),p))return;
  e.setCancellationResult(InteractionResult.SUCCESS);e.setCanceled(true);
 }
 public static boolean gather(ServerLevel l,BlockPos pos,Player p){
  if(!l.mayInteract(p,pos)||!p.mayBuild()||p.isCreative()||p.isSpectator())return false;
  var s=l.getBlockState(pos);
  if(!s.is(Blocks.SNOW)&&!s.is(Blocks.SNOW_BLOCK))return false;
  var drop=s.is(Blocks.SNOW_BLOCK)?new ItemStack(Items.SNOW_BLOCK):new ItemStack(Items.SNOWBALL,Math.max(1,s.getValue(SnowLayerBlock.LAYERS)/2));
  if(!l.destroyBlock(pos,false,p))return false;
  Block.popResource(l,pos,drop);
  return true;
 }
}
