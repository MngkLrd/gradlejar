package dev.minemods.traumacraft.client;

import dev.minemods.traumacraft.health.BodyPart;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import java.util.ArrayList;
import java.util.List;

/** GUI-scaled coordinates only; deliberately uses sharp vanilla-like pixel blocks. */
public final class MedicalPanelPainter {
    public static final int WIDTH = 128;
    public static final int HEIGHT = 166;
    public static final int EXTENDED_WIDTH = 176 + WIDTH;
    private static final String[] SHORT = {"HEAD","CHEST","ABDOM","L.ARM","R.ARM","L.LEG","R.LEG"};
    private static final int[] R = {14, 10, 10, 0, 34, 12, 24};
    private static final int[] Y = {0, 18, 39, 19, 19, 51, 51};
    private static final int[] W = {18, 26, 26, 10, 10, 12, 12};
    private static final int[] H = {17, 21, 13, 39, 39, 40, 40};
    private MedicalPanelPainter() {}

    public static int inventoryLeft(int screenWidth) {
        return (screenWidth - EXTENDED_WIDTH) / 2;
    }

    public static int panelX(int inventoryLeft) {
        return inventoryLeft + 176;
    }

    public static int panelY(int screenHeight) {
        return (screenHeight - HEIGHT) / 2;
    }

    public static void render(GuiGraphics gfx, int x, int y, int mouseX, int mouseY) {
        // Seam joins the normal inventory instead of pretending to be a separate menu.
        gfx.fill(x - 1, y - 1, x + WIDTH + 2, y + HEIGHT + 2, 0xFF373737);
        gfx.fill(x, y, x + WIDTH + 1, y + HEIGHT + 1, 0xFFFFFFFF);
        gfx.fill(x + 1, y + 1, x + WIDTH, y + HEIGHT, 0xFFC6C6C6);
        gfx.fill(x + WIDTH - 1, y + 2, x + WIDTH, y + HEIGHT, 0xFF555555);
        gfx.fill(x + 4, y + 16, x + WIDTH - 5, y + 147, 0xFF555555);
        gfx.fill(x + 5, y + 17, x + WIDTH - 6, y + 146, 0xFF20262A);
        gfx.fill(x + 7, y + 19, x + WIDTH - 8, y + 145, 0xFF293036);
        gfx.drawString(Minecraft.getInstance().font, "BODY STATUS", x + 8, y + 5, 0xFF353535, false);

        int figureX = x + 11;
        int figureY = y + 33;
        BodyPart hover = hovered(x, y, mouseX, mouseY);
        figure(gfx, figureX, figureY, hover, false);
        int rowX = x + 60;
        for (BodyPart zone : BodyPart.values()) {
            int i = zone.ordinal();
            int row = y + 23 + i * 17;
            boolean selected = zone == hover;
            if (selected) gfx.fill(rowX - 3, row - 2, x + WIDTH - 9, row + 15, 0xAA64747B);
            int textColor = selected ? 0xFFFFFFC7 : 0xFFE1E5E6;
            gfx.drawString(Minecraft.getInstance().font, SHORT[i], rowX, row, textColor, false);
            String hp = Integer.toString(Math.round(ClientBodyCache.hp(zone)));
            gfx.drawString(Minecraft.getInstance().font, hp, x + WIDTH - 12 - Minecraft.getInstance().font.width(hp), row,
                    0xFFD0D2D3, false);
            int barWidth = 55;
            int barX = rowX;
            int barY = row + 10;
            gfx.fill(barX, barY, barX + barWidth, barY + 4, 0xFF0C1012);
            int filled = Math.round((barWidth - 2) * ClientBodyCache.percent(zone));
            gfx.fill(barX + 1, barY + 1, barX + 1 + filled, barY + 3, condition(zone));
            if (ClientBodyCache.bleed(zone) > 0) gfx.fill(x + WIDTH - 10, row + 1, x + WIDTH - 6, row + 7, 0xFFD5483C);
            if (ClientBodyCache.broken(zone)) gfx.fill(x + WIDTH - 10, row + 7, x + WIDTH - 6, row + 10, 0xFFE6BD68);
        }
        gfx.fill(x + 5, y + 150, x + WIDTH - 5, y + 151, 0xFF979797);
        gfx.drawString(Minecraft.getInstance().font, "PICK MEDICINE + CLICK", x + 7, y + 155, 0xFF4A4A4A, false);

        if (hover != null) {
            List<Component> tip = new ArrayList<>();
            tip.add(Component.literal(hover.label + ": " + Math.round(ClientBodyCache.hp(hover)) + "/" + hover.maximum + " HP"));
            if (ClientBodyCache.bleed(hover) > 0)
                tip.add(Component.literal("Bleeding: level " + ClientBodyCache.bleed(hover)));
            if (ClientBodyCache.broken(hover))
                tip.add(Component.literal("Fracture"));
            tip.add(Component.literal("Click with bandage, splint or medkit on cursor"));
            gfx.renderTooltip(Minecraft.getInstance().font, tip, java.util.Optional.empty(), mouseX, mouseY);
        }
    }

    public static BodyPart hovered(int x, int y, double mouseX, double mouseY) {
        int rowX = x + 57;
        if (mouseX >= rowX && mouseX < x + WIDTH - 5 && mouseY >= y + 20 && mouseY < y + 142) {
            int index = ((int) mouseY - (y + 23)) / 17;
            if (index >= 0 && index < 7) return BodyPart.values()[index];
        }
        int px = x + 11;
        int py = y + 33;
        for (BodyPart zone : BodyPart.values()) {
            int i = zone.ordinal();
            if (mouseX >= px + R[i] && mouseX < px + R[i] + W[i] &&
                    mouseY >= py + Y[i] && mouseY < py + Y[i] + H[i]) return zone;
        }
        return null;
    }

    public static boolean insidePanel(int x, int y, double mx, double my) {
        return mx >= x && mx < x + WIDTH && my >= y && my < y + HEIGHT;
    }

    public static void figure(GuiGraphics gfx, int x, int y, BodyPart selected, boolean dim) {
        // Four pixel shades per anatomical block; no antialiasing and no scaling artifacts.
        for (BodyPart zone : BodyPart.values()) {
            int i = zone.ordinal();
            int xx = x + R[i], yy = y + Y[i], w = W[i], h = H[i];
            int main = condition(zone);
            if (dim) main = translucent(main, 0xC0);
            boolean active = selected == zone;
            gfx.fill(xx - 1, yy - 1, xx + w + 1, yy + h + 1,
                    active ? 0xFFEFE6AC : dim ? 0x8A080B0C : 0xFF111719);
            gfx.fill(xx, yy, xx + w, yy + h, darken(main, 0.67f));
            gfx.fill(xx + 1, yy + 1, xx + w - 1, yy + h - 1, main);
            gfx.fill(xx + 2, yy + 1, xx + w - 2, yy + 3, lighten(main, 0.13f));
            gfx.fill(xx + 1, yy + h - 3, xx + w - 1, yy + h - 1, darken(main, 0.80f));
            gfx.fill(xx + w - 3, yy + 4, xx + w - 1, yy + h - 3, darken(main, 0.83f));
            if (ClientBodyCache.broken(zone)) {
                gfx.fill(xx + w/2 - 2, yy + h/2, xx + w/2 + 2, yy + h/2 + 2, 0xFFFFDD8B);
                gfx.fill(xx + w/2, yy + h/2 - 3, xx + w/2 + 2, yy + h/2, 0xFFFFDD8B);
            }
            if (ClientBodyCache.bleed(zone) > 0) {
                gfx.fill(xx + 2, yy + h - 5, xx + 5, yy + h - 2, 0xFFC83731);
                if (ClientBodyCache.bleed(zone) == 2)
                    gfx.fill(xx + 5, yy + h - 4, xx + 7, yy + h - 1, 0xFFF04D3A);
            }
        }
    }

    public static int condition(BodyPart part) {
        if (ClientBodyCache.hp(part) <= 0) return 0xFF6D2728;
        float pct = ClientBodyCache.percent(part);
        if (pct < 0.25f) return 0xFFB24C3C;
        if (pct < 0.55f) return 0xFFC99650;
        if (pct < 0.80f) return 0xFFB9C17B;
        return 0xFF7DAF8E;
    }

    private static int darken(int color, float factor) {
        int a = color & 0xFF000000;
        int r = (int) (((color >>> 16) & 255) * factor);
        int g = (int) (((color >>> 8) & 255) * factor);
        int b = (int) ((color & 255) * factor);
        return a | r << 16 | g << 8 | b;
    }
    private static int lighten(int color, float amount) {
        int a = color & 0xFF000000;
        int r = (int) (((color >>> 16) & 255) * (1 - amount) + 255 * amount);
        int g = (int) (((color >>> 8) & 255) * (1 - amount) + 255 * amount);
        int b = (int) ((color & 255) * (1 - amount) + 255 * amount);
        return a | r << 16 | g << 8 | b;
    }
    private static int translucent(int color, int alpha) {
        return (color & 0x00FFFFFF) | alpha << 24;
    }
}
