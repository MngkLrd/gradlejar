package dev.minemods.sleepdebt.item;

import dev.minemods.sleepdebt.gameplay.SleepEngine;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class HerbalPoulticeItem extends Item {
 public HerbalPoulticeItem(Properties properties){super(properties);}
 @Override public InteractionResultHolder<ItemStack> use(Level world,Player player,InteractionHand hand){
  ItemStack held=player.getItemInHand(hand);
  if(!world.isClientSide&&player instanceof ServerPlayer server){
   if(SleepEngine.usePoultice(server)){
    if(!server.getAbilities().instabuild)held.shrink(1);
    server.displayClientMessage(Component.translatable("message.sleepdebt.poultice_effect"),true);
   }else{
    server.displayClientMessage(Component.translatable("message.sleepdebt.poultice_cooldown"),true);
   }
  }
  return InteractionResultHolder.sidedSuccess(held,world.isClientSide);
 }
}

