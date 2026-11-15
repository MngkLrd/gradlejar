package dev.minemods.potiontimerbar;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Continuous, 81 GUI pixel wide timer. Same x-coordinate as the first food icon.
 * Drawn only with at least one active effect.
 */
public final class PotionHudOverlay {
    private static final int WIDTH = 81;
    private static final int HEIGHT = 2;

    public static void render(GuiGraphics graphics) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.player.isSpectator()) return;

        EffectTracker.Selection selected = EffectTracker.select(mc.player, mc.level);
        if (selected == null) return;

        // The food icons start at x = guiWidth / 2 + 10, y = guiHeight - 39.
        // A two-pixel strip beginning at y - 44 leaves three pixels above food.
        int x = graphics.guiWidth() / 2 + 10;
        int y = graphics.guiHeight() - 44;
        if (mc.player.getAirSupply() < mc.player.getMaxAirSupply()) y -= 11;

        int rgb = selected.effect().getEffect().getColor() & 0xFFFFFF;
        int r = (rgb >>> 16) & 255, g = (rgb >>> 8) & 255, b = rgb & 255;
        if (Math.max(r, Math.max(g, b)) < 130) {
            r = Math.min(r + 75, 255);
            g = Math.min(g + 75, 255);
            b = Math.min(b + 75, 255);
        }
        int color = 0xFF000000 | r << 16 | g << 8 | b;
        int filled = Math.max(0, Math.min(WIDTH, Math.round(WIDTH * selected.fraction())));
        graphics.fill(x, y, x + WIDTH, y + HEIGHT, 0x991B1B22);
        if (filled > 0) graphics.fill(x, y, x + filled, y + HEIGHT, color);
    }

    private PotionHudOverlay() {}
}
