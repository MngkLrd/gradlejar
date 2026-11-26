package dev.minemods.sleepdebt.client;
import dev.minemods.sleepdebt.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.EquipmentSlot;
public final class SleepMaskOverlay {
 public static void render(GuiGraphics graphics, DeltaTracker delta){
  Minecraft mc=Minecraft.getInstance();
  if(mc.player==null||mc.player.isSpectator()||!mc.options.getCameraType().isFirstPerson())return;
  if(mc.player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.MASK.get()))
   graphics.fill(0,0,graphics.guiWidth(),graphics.guiHeight(),0xFF000000);
 }
 private SleepMaskOverlay(){}
}
