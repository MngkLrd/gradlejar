package dev.minemods.sleepdebt.item;
import dev.minemods.sleepdebt.gameplay.SleepEngine;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import java.util.List;
public final class RestDrinkItem extends Item {
 private final int mask, load, recovery, sleepBoost;
 public RestDrinkItem(Properties p,int mask,int load,int recovery,int sleepBoost) {
  super(p); this.mask=mask;this.load=load;this.recovery=recovery;this.sleepBoost=sleepBoost;
 }
 @Override public int getUseDuration(ItemStack stack,LivingEntity e){return 32;}
 @Override public UseAnim getUseAnimation(ItemStack stack){return UseAnim.DRINK;}
 @Override public InteractionResultHolder<ItemStack> use(Level level,Player p,InteractionHand hand){
  return ItemUtils.startUsingInstantly(level,p,hand);
 }
 @Override public ItemStack finishUsingItem(ItemStack stack,Level level,LivingEntity e){
  if(!level.isClientSide() && e instanceof ServerPlayer p) {
   SleepEngine.drink(p,mask,load,recovery,sleepBoost);
   if (!p.getAbilities().instabuild) stack.shrink(1);
  }
  return stack;
 }
 @Override public void appendHoverText(ItemStack stack,TooltipContext ctx,List<Component> list,TooltipFlag flag){
  list.add(Component.translatable(getDescriptionId()+".tooltip"));
  super.appendHoverText(stack,ctx,list,flag);
 }
}
