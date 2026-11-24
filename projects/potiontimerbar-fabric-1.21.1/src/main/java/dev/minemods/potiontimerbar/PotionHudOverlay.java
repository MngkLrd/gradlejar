package dev.minemods.potiontimerbar;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import java.util.List;

/** Native XP track, recolored pixel fill, one timer per status effect. */
public final class PotionHudOverlay {
    private static final int WIDTH=81, HEIGHT=5, INNER=79, STEP=6;
    private static final ResourceLocation XP_BACKGROUND =
        ResourceLocation.withDefaultNamespace("hud/experience_bar_background");
    private PotionHudOverlay() {}

    public static void render(GuiGraphics g) {
        Minecraft mc=Minecraft.getInstance();
        if (!HudConfig.enabled() || mc.options.hideGui || mc.player==null ||
                mc.level==null || !mc.player.isAlive() || mc.player.isSpectator()) return;
        List<EffectTracker.Selection> effects=EffectTracker.collect(mc.player,mc.level);
        if(effects.isEmpty()) return;
        int x=g.guiWidth()/2+10;
        int y=g.guiHeight()-46+HudConfig.verticalOffset();
        if(HudConfig.autoAvoidAir() &&
                (mc.player.isUnderWater() || mc.player.getAirSupply()<mc.player.getMaxAirSupply())) y-=11;
        for(int i=0;i<effects.size();i++){
            var selection=effects.get(i);
            int row=y-i*STEP;
            if(row+HEIGHT<=0) break;
            drawBar(g,x,row,selection.fraction(),PotionPalette.forEffect(selection.effect().getEffect()));
        }
    }

    private static void drawBar(GuiGraphics g,int x,int y,float fraction,PotionPalette.Gradient palette) {
        int n=Math.min(INNER,Math.max(0,Math.round(INNER*fraction)));
        if(fraction>0f) n=Math.max(1,n);
        // Use the actual vanilla XP background sprite; no custom thick rectangular border.
        g.blitSprite(XP_BACKGROUND,x,y,WIDTH,HEIGHT);
        for(int i=0;i<n;i++){
            // Always show the full light-to-dark gradient, even on a short remaining fill.
            float p=n==1?0.5f:i/(float)(n-1);
            int c=palette.at(p);
            int px=x+1+i;
            // Small XP-style divisions, retaining the continuous readable fill.
            boolean seam=(i+1)%10==0 && i+1<n;
            int top=PotionPalette.mix(c,0xFFFFFF,.15f);
            int mid=c;
            int bottom=PotionPalette.mix(c,0x000000,.28f);
            if(seam){
                top=PotionPalette.mix(top,0x000000,.30f);
                mid=PotionPalette.mix(mid,0x000000,.38f);
                bottom=PotionPalette.mix(bottom,0x000000,.38f);
            }
            g.fill(px,y+1,px+1,y+2,0xFF000000|top);
            g.fill(px,y+2,px+1,y+3,0xFF000000|mid);
            g.fill(px,y+3,px+1,y+4,0xFF000000|bottom);
        }
    }
}
