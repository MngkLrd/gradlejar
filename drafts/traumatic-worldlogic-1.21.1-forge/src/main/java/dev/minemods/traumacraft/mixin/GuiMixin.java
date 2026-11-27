package dev.minemods.traumacraft.mixin;
import dev.minemods.traumacraft.client.gui.TraumaHud;
import dev.minemods.traumacraft.client.gui.TraumaticScreenEffects;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=Gui.class,remap=false)
public abstract class GuiMixin {
    @Inject(method="render",at=@At("TAIL"))
    private void traumatic$render(GuiGraphics gui,DeltaTracker tick,CallbackInfo ci) {
        TraumaticScreenEffects.render(gui,tick);
        TraumaHud.render(gui,tick);
    }
}
