package dev.minemods.potiontimerbar;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/** Catch the Blast countdown style, aligned with the vanilla food row. */
public final class PotionHudOverlay {
    private static final int WIDTH=81;
    private static final int GREEN=0x61C647, GOLD=0xF5C74A, RED=0xEC4938;

    private PotionHudOverlay() {}

    public static void render(GuiGraphics g, DeltaTracker partialTick) {
        Minecraft mc=Minecraft.getInstance();
        if (!HudConfig.ENABLED.get() || mc.options.hideGui || mc.player==null ||
                mc.level==null || !mc.player.isAlive() || mc.player.isSpectator()) return;
        EffectTracker.Selection selected=EffectTracker.select(mc.player,mc.level);
        if(selected==null) return;
        int x=g.guiWidth()/2+10;
        // Preserve the previous lower edge and the three-pixel gap above food.
        int y=g.guiHeight()-47+HudConfig.VERTICAL_OFFSET.get();
        if(HudConfig.AUTO_AVOID_AIR.get() &&
                (mc.player.isUnderWater() || mc.player.getAirSupply()<mc.player.getMaxAirSupply())) y-=11;
        bar(g,x,y,selected.fraction(),true);
    }

    // Pixel-sharp five-pixel XP-style track; both states use the same palette.
    private static void bar(GuiGraphics g,int x,int y,float amount,boolean countdown) {
        int n=Math.max(0,Math.min(WIDTH-2,Math.round(amount*(WIDTH-2))));
        if(amount>0) n=Math.max(1,n);
        g.fill(x+1,y,x+WIDTH-1,y+5,0xFF171512);
        g.fill(x,y+1,x+WIDTH,y+4,0xFF171512);
        g.fill(x+1,y+1,x+WIDTH-1,y+4,0xFF49423B);
        for(int i=0;i<n;i++) {
            float p=i/(float)(WIDTH-3);
            // Countdown heats up as it empties: its visible end always reaches red.
            if(countdown) p=1f-amount+i/(float)(Math.max(1,n-1))*amount;
            int c=p<.5f?mix(GREEN,GOLD,p*2):mix(GOLD,RED,(p-.5f)*2);
            g.fill(x+1+i,y+1,x+2+i,y+2,0xFF000000|mix(c,0xFFFFFF,.24f));
            g.fill(x+1+i,y+2,x+2+i,y+3,0xFF000000|c);
            g.fill(x+1+i,y+3,x+2+i,y+4,0xFF000000|mix(c,0x000000,.34f));
        }
    }
    private static int mix(int a,int b,float t) {
        t=Math.max(0,Math.min(1,t));
        int r=Math.round(((a>>16)&255)*(1-t)+((b>>16)&255)*t);
        int g=Math.round(((a>>8)&255)*(1-t)+((b>>8)&255)*t);
        int bl=Math.round((a&255)*(1-t)+(b&255)*t);
        return (r<<16)|(g<<8)|bl;
    }
}
