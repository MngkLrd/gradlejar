package dev.minemods.potiontimerbar;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/** The Catch the Blast track, with a potion-specific countdown palette. */
public final class PotionHudOverlay {
    private static final int WIDTH = 81;
    private PotionHudOverlay() {}

    public static void render(GuiGraphics g) {
        Minecraft mc = Minecraft.getInstance();
        if (!HudConfig.enabled() || mc.options.hideGui || mc.player == null ||
                mc.level == null || !mc.player.isAlive() || mc.player.isSpectator()) return;
        EffectTracker.Selection selected = EffectTracker.select(mc.player, mc.level);
        if (selected == null) return;

        int x = g.guiWidth()/2 + 10;
        int y = g.guiHeight() - 47 + HudConfig.verticalOffset();
        if (HudConfig.autoAvoidAir() &&
                (mc.player.isUnderWater() || mc.player.getAirSupply() < mc.player.getMaxAirSupply())) y -= 11;

        bar(g, x, y, selected.fraction(), PotionPalette.forEffect(selected.effect().getEffect()));
    }

    private static void bar(GuiGraphics g, int x, int y, float amount, PotionPalette.Gradient palette) {
        amount = Math.max(0f, Math.min(1f, amount));
        int n = Math.max(0, Math.min(WIDTH-2, Math.round(amount*(WIDTH-2))));
        if (amount > 0) n = Math.max(1, n);
        g.fill(x+1,y,x+WIDTH-1,y+5,0xFF171512);
        g.fill(x,y+1,x+WIDTH,y+4,0xFF171512);
        g.fill(x+1,y+1,x+WIDTH-1,y+4,0xFF49423B);
        for (int i=0; i<n; i++) {
            // Retain the countdown's darkening behavior, but never shift to an unrelated hue.
            float p = n == 1 ? 1f : 1f-amount + i/(float)(n-1)*amount;
            int c = palette.at(p);
            g.fill(x+1+i,y+1,x+2+i,y+2,0xFF000000 | PotionPalette.mix(c,0xFFFFFF,.24f));
            g.fill(x+1+i,y+2,x+2+i,y+3,0xFF000000 | c);
            g.fill(x+1+i,y+3,x+2+i,y+4,0xFF000000 | PotionPalette.mix(c,0x000000,.34f));
        }
    }
}
