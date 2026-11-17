package com.kirill.lurelantern.item;
import com.kirill.lurelantern.util.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import java.util.List;
public class LureLanternBlockItem extends BlockItem {
 private final boolean advanced;
 public LureLanternBlockItem(Block block,Properties properties,boolean advanced){super(block,properties);this.advanced=advanced;}
 @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
   ItemStack stack=player.getItemInHand(hand);
   if(!level.isClientSide && level instanceof net.minecraft.server.level.ServerLevel server){
     if(player.isShiftKeyDown()&&CapturedMobData.hasCapturedMob(stack)){
       LureLanternLogic.releaseCapturedMob(server,player.position().add(player.getLookAngle().scale(2)),stack);
     } else if(advanced&&!CapturedMobData.hasCapturedMob(stack)){
       if(!LureLanternLogic.captureNearestMob(server,player.position(),stack)){
         CapturedMobData.setLureActive(stack,!CapturedMobData.isLureActive(stack));
       }
     }else CapturedMobData.setLureActive(stack,!CapturedMobData.isLureActive(stack));
   }
   return InteractionResultHolder.sidedSuccess(stack,level.isClientSide);
 }
 @Override public void inventoryTick(ItemStack stack,Level level,Entity holder,int slot,boolean selected){
   if(!level.isClientSide&&selected&&holder.tickCount%20==0&&CapturedMobData.isLureActive(stack))
     LureLanternLogic.lureMobs(level,holder.position());
 }
 @Override public void appendHoverText(ItemStack stack,Item.TooltipContext context,List<Component> list,TooltipFlag flag){
   CapturedMobData.getCapturedMobTag(stack).ifPresent(tag->
     list.add(Component.translatable("message.lurelantern.mob_inside",CapturedMobData.getCapturedMobName(tag))));
 }
}
