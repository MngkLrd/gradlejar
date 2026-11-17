package com.kirill.lurelantern.item;
import com.kirill.lurelantern.client.AdvancedLanternItemRenderer;
import com.kirill.lurelantern.util.*;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
public final class LureLanternBlockItem extends BlockItem {
 private final boolean advanced;
 public LureLanternBlockItem(Block block,Properties props,boolean advanced){super(block,props);this.advanced=advanced;}
 @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
  ItemStack stack=player.getItemInHand(hand);
  if(!level.isClientSide&&level instanceof net.minecraft.server.level.ServerLevel server){
   if(player.isShiftKeyDown()&&CapturedMobData.hasCapturedMob(stack))
     LureLanternLogic.releaseCapturedMob(server,player.position().add(player.getLookAngle().scale(2)),stack);
   else if(advanced&&!CapturedMobData.hasCapturedMob(stack)){
    if(!LureLanternLogic.captureNearestMob(server,player.position(),stack))
      CapturedMobData.setLureActive(stack,!CapturedMobData.isLureActive(stack));
   }else CapturedMobData.setLureActive(stack,!CapturedMobData.isLureActive(stack));
  }
  return InteractionResultHolder.sidedSuccess(stack,level.isClientSide);
 }
 @Override public void inventoryTick(ItemStack stack,Level level,Entity holder,int slot,boolean selected){
  if(!level.isClientSide&&selected&&holder.tickCount%20==0&&CapturedMobData.isLureActive(stack))
   LureLanternLogic.lureMobs(level,holder.position());
 }
 @Override public void appendHoverText(ItemStack stack,Level level,List<Component> list,TooltipFlag flag){
  CapturedMobData.getCapturedMobTag(stack).ifPresent(t->
    list.add(Component.translatable("message.lurelantern.mob_inside",CapturedMobData.getCapturedMobName(t))));
 }
 @Override public void initializeClient(Consumer<IClientItemExtensions> consumer){
  if(!advanced)return;
  consumer.accept(new IClientItemExtensions(){
   private AdvancedLanternItemRenderer renderer;
   @Override public BlockEntityWithoutLevelRenderer getCustomRenderer(){
    if(renderer==null)renderer=new AdvancedLanternItemRenderer();
    return renderer;
   }
  });
 }
}
