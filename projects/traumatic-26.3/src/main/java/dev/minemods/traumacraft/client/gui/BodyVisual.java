package dev.minemods.traumacraft.client.gui;

import dev.minemods.traumacraft.client.ClientBodyState;
import dev.minemods.traumacraft.health.BodyPart;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Deliberately crisp pixel clusters, no image scaling or smoothing. */
public final class BodyVisual {
    public record Zone(BodyPart part, int x, int y, int width, int height) {
        public boolean contains(int mouseX, int mouseY, int offsetX, int offsetY) {
            return mouseX >= offsetX + x && mouseX < offsetX + x + width
                    && mouseY >= offsetY + y && mouseY < offsetY + y + height;
        }
    }
    // Schematic front-facing human with separated torso, arms and legs; 29x91 design units.
    private static final Zone[] ZONES = {
            new Zone(BodyPart.HEAD, 11, 0, 14, 15),
            new Zone(BodyPart.CHEST, 9, 18, 18, 23),
            new Zone(BodyPart.STOMACH, 9, 42, 18, 14),
            new Zone(BodyPart.LEFT_ARM, 0, 18, 8, 38),
            new Zone(BodyPart.RIGHT_ARM, 28, 18, 8, 38),
            new Zone(BodyPart.LEFT_LEG, 9, 59, 8, 32),
            new Zone(BodyPart.RIGHT_LEG, 19, 59, 8, 32)
    };
    private BodyVisual() {}
    public static BodyPart hit(int mouseX, int mouseY, int ox, int oy) {
        for (Zone zone : ZONES) if (zone.contains(mouseX, mouseY, ox, oy)) return zone.part();
        return null;
    }
    public static void draw(GuiGraphicsExtractor gui, int ox, int oy, boolean translucent) {
        for (Zone zone : ZONES) {
            int x = ox + zone.x(), y = oy + zone.y(), w = zone.width(), h = zone.height();
            int fill = hpColor(zone.part(), translucent);
            int shadow = translucent ? 0xA00A0F12 : 0xFF161B20;
            gui.fill(x-1, y-1, x+w+1, y+h+1, shadow);
            gui.fill(x, y, x+w, y+h, fill);
            int shine = translucent ? 0x40FFFFFF : 0x55FFFFFF;
            gui.fill(x, y, x+w, y+2, shine);
            gui.fill(x, y+2, x+2, y+h-2, shine);
            if (ClientBodyState.fractured(zone.part())) {
                gui.fill(x + w/2-1, y+h/2-2, x+w/2+1, y+h/2+2, 0xFFDDD6C9);
                gui.fill(x + w/2+1, y+h/2+1, x+w/2+3, y+h/2+3, 0xFFE5E4E0);
            }
            if (ClientBodyState.bleeding(zone.part()) > 0) {
                gui.fill(x+w-4, y+h-5, x+w-2, y+h-2, 0xFFF12B35);
            }
        }
    }
    public static int hpColor(BodyPart part, boolean translucent) {
        float fraction = ClientBodyState.fraction(part);
        int rgb = fraction <= 0 ? 0x323138 : fraction <= 0.15 ? 0xB73D42
                : fraction <= 0.40 ? 0xD47A3C : fraction <= 0.75 ? 0xC7AE52 : 0x70AA80;
        return (translucent ? 0xB0000000 : 0xFF000000) | rgb;
    }
}
