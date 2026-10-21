package dev.minemods.potiontimerbar;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/**
 * A single, unsegmented 81px strip aligned exactly with the vanilla food icons.
 * All values are GUI-scaled pixels, not physical display pixels.
 */
public final class PotionHudOverlay {
    // 10 vanilla food icons: 9px icon width + 9 gaps of 8px = 81px.
    private static final int FOOD_WIDTH = 81;
    private static final int FOOD_LEFT_OF_CENTER = 10;
    // Food icons start at guiHeight - 39; put a 2px strip above them, leaving a 3px gap.
    private static final int STRIP_TOP_FROM_BOTTOM = 44;
    private static final int STRIP_HEIGHT = 2;

    public static void render(GuiGraphics graphics, DeltaTracker partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!HudConfig.ENABLED.get() || minecraft.options.hideGui ||
            minecraft.player == null || minecraft.level == null || minecraft.player.isSpectator()) {
            return;
        }

        // A bar is drawn only while at least one actual status effect is active.
        EffectTracker.Selection selected = EffectTracker.select(minecraft.player, minecraft.level);
        if (selected == null) {
            return;
        }

        final int x = graphics.guiWidth() / 2 + FOOD_LEFT_OF_CENTER;
        int y = graphics.guiHeight() - STRIP_TOP_FROM_BOTTOM + HudConfig.VERTICAL_OFFSET.get();

        // Air icons occupy the area just above hunger. Clear them without moving X or changing width.
        if (HudConfig.AUTO_AVOID_AIR.get() &&
            (minecraft.player.isUnderWater() ||
             minecraft.player.getAirSupply() < minecraft.player.getMaxAirSupply())) {
            y -= 11;
        }

        final int color = 0xFF000000 | makeReadable(selected.effect().getEffect().value().getColor());
        final int filled = Math.max(0, Math.min(FOOD_WIDTH, Math.round(FOOD_WIDTH * selected.fraction())));

        // Both background and fill use the SAME two pixel rows: exactly one continuous strip.
        graphics.fill(x, y, x + FOOD_WIDTH, y + STRIP_HEIGHT, 0x991B1B22);
        if (filled > 0) {
            graphics.fill(x, y, x + filled, y + STRIP_HEIGHT, color);
        }
    }

    private static int makeReadable(int color) {
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        // Preserve the potion's hue while lifting very dark effect colors off the HUD.
        if (Math.max(r, Math.max(g, b)) < 130) {
            r = Math.min(255, r + 75);
            g = Math.min(255, g + 75);
            b = Math.min(255, b + 75);
        }
        return r << 16 | g << 8 | b;
    }

    private PotionHudOverlay() {}
}
