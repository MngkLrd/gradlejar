package com.kirill.simplesorter.network;
import com.kirill.simplesorter.registry.ModItems;
import com.kirill.simplesorter.settings.SortingMode;
import com.kirill.simplesorter.settings.SortingWandSettings;
import com.kirill.simplesorter.sorting.SortCategoryRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

public record WandSettingsPayload(int handIndex,int modeIndex,int radius,boolean showRadius,boolean showContainerHighlights,boolean showTransferParticles,boolean reloadRules){
 public static WandSettingsPayload save(InteractionHand hand, SortingWandSettings s, boolean reload){
  s=s.sanitized();return new WandSettingsPayload(hand==InteractionHand.OFF_HAND?1:0,s.mode().index(),s.radius(),s.showRadius(),s.showContainerHighlights(),s.showTransferParticles(),reload);
 }
 public void write(FriendlyByteBuf b){b.writeVarInt(handIndex);b.writeVarInt(modeIndex);b.writeVarInt(radius);b.writeBoolean(showRadius);b.writeBoolean(showContainerHighlights);b.writeBoolean(showTransferParticles);b.writeBoolean(reloadRules);}
 public static WandSettingsPayload read(FriendlyByteBuf b){return new WandSettingsPayload(b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readBoolean(),b.readBoolean(),b.readBoolean(),b.readBoolean());}
 public void handle(ServerPlayer p){
  ItemStack stack=p.getItemInHand(handIndex==1?InteractionHand.OFF_HAND:InteractionHand.MAIN_HAND);
  if(stack.isEmpty()||stack.getItem()!=ModItems.SORTING_WAND.get())return;
  SortingWandSettings s=new SortingWandSettings(SortingMode.byIndex(modeIndex),radius,showRadius,showContainerHighlights,showTransferParticles).sanitized();
  s.writeTo(stack);
  if(reloadRules){if(!p.hasPermissions(2)){p.displayClientMessage(Component.translatable("message.simplesorter.reload_denied"),true);return;}
   SortCategoryRegistry.reload();p.displayClientMessage(Component.translatable("message.simplesorter.rules_reloaded"),true);}
 }
}
