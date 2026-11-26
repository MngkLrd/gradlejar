package dev.christmasui.mixin;
import dev.christmasui.client.FestiveArt;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.LoadingOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(LoadingOverlay.class)
public abstract class LoadingOverlayMixin {
    @Shadow private float currentProgress;
    @Inject(method="drawProgressBar",at=@At("HEAD"),cancellable=true)
    private void christmasui$progress(GuiGraphics g,int x,int y,int endX,int endY,float alpha,CallbackInfo ci){
        FestiveArt.progress(g,x,y,endX-x,endY-y,currentProgress,alpha);
        ci.cancel();
    }
}
