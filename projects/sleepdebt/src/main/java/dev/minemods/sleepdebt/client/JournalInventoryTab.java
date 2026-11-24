package dev.minemods.sleepdebt.client;
import dev.minemods.sleepdebt.network.JournalRequest;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.network.PacketDistributor;
public final class JournalInventoryTab {
 public static void init(ScreenEvent.Init.Post e){
  if(!(e.getScreen() instanceof InventoryScreen screen))return;
  // Above the inventory, not over equipment slots or the recipe book.
  int x=Math.max(2,Math.min(screen.width-82,screen.getGuiLeft()+screen.getXSize()-80));
  int y=Math.max(2,screen.getGuiTop()-23);
  e.addListener(Button.builder(Component.translatable("journal.sleepdebt.tab"),b->
    PacketDistributor.sendToServer(new JournalRequest())).bounds(x,y,80,20).build());
 }
 private JournalInventoryTab(){}
}
