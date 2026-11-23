package com.kirill.simplesorter.network;
import com.kirill.simplesorter.item.SortingWandItem;
import com.kirill.simplesorter.registry.ModItems;
import com.kirill.simplesorter.settings.SortingMode;
import com.kirill.simplesorter.settings.SortingWandSettings;
import com.kirill.simplesorter.sorting.SortingManager;
import com.kirill.simplesorter.sorting.SortingResult;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
public record WandActionPayload(int handIndex,int storedModeIndex,int actionModeIndex,int radius,boolean showRadius,boolean showContainerHighlights,boolean showTransferParticles){
 public static WandActionPayload run(InteractionHand hand,SortingWandSettings s,SortingMode mode){s=s.sanitized();return new WandActionPayload(hand==InteractionHand.OFF_HAND?1:0,s.mode().index(),mode==null?s.mode().index():mode.index(),s.radius(),s.showRadius(),s.showContainerHighlights(),s.showTransferParticles());}
 public void write(FriendlyByteBuf b){b.writeVarInt(handIndex);b.writeVarInt(storedModeIndex);b.writeVarInt(actionModeIndex);b.writeVarInt(radius);b.writeBoolean(showRadius);b.writeBoolean(showContainerHighlights);b.writeBoolean(showTransferParticles);}
 public static WandActionPayload read(FriendlyByteBuf b){return new WandActionPayload(b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readBoolean(),b.readBoolean(),b.readBoolean());}
 public void handle(ServerPlayer p){
  ItemStack stack=p.getItemInHand(handIndex==1?InteractionHand.OFF_HAND:InteractionHand.MAIN_HAND);
  if(stack.isEmpty()||stack.getItem()!=ModItems.SORTING_WAND.get())return;
  if(p.getCooldowns().isOnCooldown(stack.getItem()))return;
  SortingWandSettings s=new SortingWandSettings(SortingMode.byIndex(storedModeIndex),radius,showRadius,showContainerHighlights,showTransferParticles).sanitized();
  s.writeTo(stack); SortingResult result=SortingManager.sortAroundPlayer(p.serverLevel(),p,s.withMode(SortingMode.byIndex(actionModeIndex)));
  p.getCooldowns().addCooldown(stack.getItem(),SortingWandItem.COOLDOWN_TICKS);if(result.used())SortingWandItem.consumeUse(stack,p);
 }
}
