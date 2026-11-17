package com.kirill.lurelantern.item;
import com.kirill.lurelantern.util.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;
public final class LureLanternBlockItem extends BlockItem {
 private final boolean advanced;
 public LureLanternBlockItem(Block block,Properties properties,boolean advanced){super(block,properties);this.advanced=advanced;}
 @Override public InteractionResult use(Level level,Player player,InteractionHand hand){
  ItemStack stack=player.getItemInHand(hand);
  if(level instanceof ServerLevel server){
    if(player.isShiftKeyDown()&&CapturedMobData.hasMob(stack))
      LureLanternLogic.release(server,BlockPos.containing(player.position().add(player.getLookAngle().scale(2))),stack);
    else if(advanced&&!CapturedMobData.hasMob(stack)){
      if(!LureLanternLogic.captureNearest(server,player.position(),stack))
        CapturedMobData.setActive(stack,!CapturedMobData.isActive(stack));
    }else CapturedMobData.setActive(stack,!CapturedMobData.isActive(stack));
    return InteractionResult.SUCCESS_SERVER;
  }
  return InteractionResult.SUCCESS;
 }
 @Override public void inventoryTick(ItemStack stack,ServerLevel level,Entity owner,@Nullable EquipmentSlot slot){
  if(slot==EquipmentSlot.MAINHAND&&owner.tickCount%20==0&&CapturedMobData.isActive(stack))
     LureLanternLogic.lureMobs(level,owner.position());
 }
}
