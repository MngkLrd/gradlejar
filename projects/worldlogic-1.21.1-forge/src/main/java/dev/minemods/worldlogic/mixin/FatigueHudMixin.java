package dev.minemods.worldlogic.mixin;
import dev.minemods.worldlogic.client.FatigueHud;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=Gui.class,remap=false)
public abstract class FatigueHudMixin {
 @Inject(method="render",at=@At("TAIL"),remap=false)
 private void worldlogic$fatigueHud(GuiGraphics graphics,DeltaTracker delta,CallbackInfo ci){FatigueHud.render(graphics);}
}
