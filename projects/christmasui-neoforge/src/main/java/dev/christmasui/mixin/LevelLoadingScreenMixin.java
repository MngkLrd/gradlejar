package dev.christmasui.mixin;
import dev.christmasui.client.FestiveArt;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.server.level.progress.StoringChunkProgressListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(LevelLoadingScreen.class)
public abstract class LevelLoadingScreenMixin {
    @Shadow @Final private StoringChunkProgressListener progressListener;
    @Inject(method="render",at=@At("TAIL"))
    private void christmasui$progress(GuiGraphics g,int mx,int my,float pt,CallbackInfo ci){
        int w=Math.min(180,g.guiWidth()-24);
        if(w<16||g.guiHeight()<100)return;
        int x=(g.guiWidth()-w)/2,y=g.guiHeight()-24;
        FestiveArt.progress(g,x,y,w,12,progressListener.getProgress()/100f,1f);
        FestiveArt.garland(g,x,y-9,w,1f);
    }
}
