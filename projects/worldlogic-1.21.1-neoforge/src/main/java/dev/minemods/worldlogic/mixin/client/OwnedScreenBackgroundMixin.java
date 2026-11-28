package dev.minemods.worldlogic.mixin.client;
import dev.minemods.worldlogic.api.client.WorldLogicTabs;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Screen.class)
abstract class OwnedScreenBackgroundMixin {
 @Inject(method="renderBackground",at=@At("HEAD"),cancellable=true)
 private void worldlogic$keepContentSharp(GuiGraphics g,int mx,int my,float delta,CallbackInfo ci){
  if(WorldLogicTabs.drawsOwnBackground((Screen)(Object)this))ci.cancel();
 }
}
