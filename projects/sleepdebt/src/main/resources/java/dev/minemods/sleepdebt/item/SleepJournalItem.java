package dev.minemods.sleepdebt.item;

import dev.minemods.sleepdebt.gameplay.SleepEngine;
import dev.minemods.sleepdebt.gameplay.RestEnvironment;
import dev.minemods.sleepdebt.network.SleepReportPayload;
import net.neoforged.neoforge.network.PacketDistributor;
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
   var quality=RestEnvironment.evaluate(server,server.blockPosition());
   PacketDistributor.sendToPlayer(server,new SleepReportPayload(
    SleepEngine.getDebt(server),SleepEngine.getFatigue(server),
    SleepEngine.getStress(server),SleepEngine.getLastQuality(server),
    SleepEngine.getLastRecovery(server),SleepEngine.getLastReasons(server),
    quality.quality(),quality.reasons()));
  }
  return InteractionResultHolder.sidedSuccess(held,level.isClientSide);
 }
}

