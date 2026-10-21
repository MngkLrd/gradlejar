package dev.minemods.potiontimerbar;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.DeltaTracker;
import net.minecraft.world.entity.LivingEntity;

public final class PotionHudOverlay {
    private static final int BAR_WIDTH = 67;

    public static void render(GuiGraphics graphics, DeltaTracker partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!HudConfig.ENABLED.get() || minecraft.options.hideGui ||
            minecraft.player == null || minecraft.level == null || minecraft.player.isSpectator()) {
            return;
        }

        EffectTracker.Selection selected = EffectTracker.select(
            minecraft.player, minecraft.level, HudConfig.PREFER_HARMFUL.get()
        );
        if (selected == null) return;

        int x = graphics.guiWidth() / 2 + 10;
        int y = graphics.guiHeight() - 54 + HudConfig.VERTICAL_OFFSET.get();
        // Vanilla renders air bubbles above food. Shift up so they remain readable.
        if (HudConfig.AUTO_AVOID_AIR.get() &&
            (minecraft.player.isUnderWater() ||
             minecraft.player.getAirSupply() < minecraft.player.getMaxAirSupply())) {
            y -= 11;
        }
        if (minecraft.player.getVehicle() instanceof LivingEntity) {
            y -= 10;
        }

        int color = selected.effect().getEffect().value().getColor() & 0xFFFFFF;
        color = readableColor(color);
        int opaque = 0xFF000000 | color;
        drawBottle(graphics, x, y, opaque);

        final int barX = x + 14;
        final int barY = y + 5;
        final int fillWidth;
        if (HudConfig.SHOW_COUNTDOWN.get()) {
            fillWidth = 40;
            String time = durationText(selected.effect().getDuration());
            graphics.drawString(minecraft.font, time,
                barX + BAR_WIDTH - minecraft.font.width(time),
                y + 1, 0xFFE6E6EA, true);
        } else {
            fillWidth = BAR_WIDTH;
        }

        // Fine pixel-straight trough with a translucent outer outline.
        graphics.fill(barX - 1, barY - 2, barX + fillWidth + 1, barY + 5, 0x990B0C16);
        graphics.fill(barX, barY, barX + fillWidth, barY + 3, 0xFF252531);
        int length = Math.round(fillWidth * selected.fraction());
        if (length > 0) {
            graphics.fill(barX, barY, barX + length, barY + 2, opaque);
            graphics.fill(barX, barY + 2, barX + length, barY + 3, shade(color));
            // Final 3 seconds: a very small, vanilla-like pixel blink.
            if (selected.effect().getDuration() <= 60 &&
                (minecraft.level.getGameTime() / 5) % 2 == 0) {
                graphics.fill(barX, barY, barX + length, barY + 1, 0xFFECE0FC);
            }
        }
    }

    private static String durationText(int ticks) {
        int seconds = (ticks + 19) / 20;
        return (seconds / 60) + ":" + String.format("%02d", seconds % 60);
    }

    private static int readableColor(int color) {
        int r = (color >> 16) & 255;
        int g = (color >> 8) & 255;
        int b = color & 255;
        // Dark potion colors need a small boost against Minecraft's HUD.
        if (Math.max(r, Math.max(g, b)) < 130) {
            r = Math.min(255, r + 75);
            g = Math.min(255, g + 75);
            b = Math.min(255, b + 75);
        }
        return r << 16 | g << 8 | b;
    }

    private static int shade(int color) {
        int r = ((color >> 16) & 255) * 3 / 5;
        int g = ((color >> 8) & 255) * 3 / 5;
        int b = (color & 255) * 3 / 5;
        return 0xFF000000 | r << 16 | g << 8 | b;
    }

    private static void drawBottle(GuiGraphics g, int x, int y, int liquid) {
        final int outline = 0xFF20202B;
        final int glass = 0xFFB4BAC7;
        // The entire 11x12 silhouette is composed of crisp, unfiltered GUI pixels.
        g.fill(x + 4, y, x + 8, y + 2, outline);
        g.fill(x + 5, y, x + 7, y + 2, 0xFFCBB790);
        g.fill(x + 4, y + 2, x + 8, y + 4, glass);
        g.fill(x + 3, y + 4, x + 9, y + 6, outline);
        g.fill(x + 2, y + 6, x + 10, y + 10, outline);
        g.fill(x + 3, y + 6, x + 9, y + 10, glass);
        g.fill(x + 3, y + 7, x + 9, y + 10, liquid);
        g.fill(x + 4, y + 7, x + 5, y + 9, 0xFFECF5FF);
        g.fill(x + 3, y + 10, x + 9, y + 12, outline);
        g.fill(x + 4, y + 10, x + 8, y + 11, shade(liquid & 0xFFFFFF));
    }

    private PotionHudOverlay() {}
}
