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

public final class SleepJournalItem extends Item {
 public SleepJournalItem(Properties properties){super(properties);}
 @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
  ItemStack held=player.getItemInHand(hand);
  if(!level.isClientSide &&player instanceof ServerPlayer server){
   server.sendSystemMessage(Component.translatable("message.sleepdebt.journal_heading"));
   server.sendSystemMessage(Component.translatable("message.sleepdebt.journal_stats",
     SleepEngine.getDebt(server),SleepEngine.getFatigue(server),SleepEngine.getStress(server)));
   server.sendSystemMessage(Component.translatable("message.sleepdebt.journal_last",
     SleepEngine.getLastQuality(server),SleepEngine.getLastRecovery(server)));
   server.sendSystemMessage(Component.translatable("message.sleepdebt.journal_advice",
     SleepEngine.getDebt(server)>=620?"§c":"§a",
     SleepEngine.getDebt(server)>=620?Component.translatable("message.sleepdebt.advice_sleep"):
       Component.translatable("message.sleepdebt.advice_rest")));
  }
  return InteractionResultHolder.sidedSuccess(held,level.isClientSide);
 }
}

