package dev.minemods.traumacraft.client.gui;

import dev.minemods.traumacraft.client.ClientBodyState;
import dev.minemods.traumacraft.health.BodyPart;
import net.minecraft.client.gui.GuiGraphics;

/**
 * One coherent 46 x 98 anatomy layout, shared by the survival inventory and HUD.
 * No anatomy region (including its one-pixel border) intersects another.
 */
public final class BodyVisual {
    public record Zone(BodyPart part, int x, int y, int width, int height) {
        public boolean contains(int mouseX, int mouseY, int offsetX, int offsetY) {
            return mouseX >= offsetX + x && mouseX < offsetX + x + width
                    && mouseY >= offsetY + y && mouseY < offsetY + y + height;
        }
    }

    private static final Zone[] ZONES = {
            new Zone(BodyPart.HEAD,       17,  1, 12, 13),
            new Zone(BodyPart.CHEST,      12, 18, 22, 23),
            new Zone(BodyPart.STOMACH,    12, 44, 22, 16),
            new Zone(BodyPart.LEFT_ARM,    2, 19,  8, 40),
            new Zone(BodyPart.RIGHT_ARM,  36, 19,  8, 40),
            new Zone(BodyPart.LEFT_LEG,   14, 64,  9, 32),
            new Zone(BodyPart.RIGHT_LEG,  25, 64,  9, 32)
    };

    private BodyVisual() {}

    public static BodyPart hit(int mouseX, int mouseY, int ox, int oy) {
        for (Zone zone : ZONES) if (zone.contains(mouseX, mouseY, ox, oy)) return zone.part();
        return null;
    }

    public static void draw(GuiGraphics gui, int ox, int oy, boolean translucent) {
        for (Zone zone : ZONES) {
            final BodyPart part = zone.part();
            final int x = ox + zone.x(), y = oy + zone.y(), w = zone.width(), h = zone.height();
            final int frame = translucent ? 0x9C14191C : 0xFF171D1F;
            gui.fill(x - 1, y - 1, x + w + 1, y + h + 1, frame);
            gui.fill(x, y, x + w, y + h, hpColor(part, translucent));
            gui.fill(x + 1, y + 1, x + w - 1, y + 3,
                    translucent ? 0x30FFFFFF : 0x44FFFFFF);
            gui.fill(x + 1, y + 3, x + 3, y + h - 2,
                    translucent ? 0x24FFFFFF : 0x30FFFFFF);

            if (ClientBodyState.fractured(part)) {
                int middle = x + w / 2;
                gui.fill(middle - 1, y + h / 2 - 3, middle + 1, y + h / 2 + 2, 0xFFEDE2C7);
                gui.fill(middle + 1, y + h / 2, middle + 3, y + h / 2 + 3, 0xFFEDE2C7);
            }
            if (ClientBodyState.bleeding(part) > 0) {
                gui.fill(x + w - 5, y + h - 6, x + w - 2, y + h - 2, 0xFFEE444A);
            }
        }
    }

    public static int hpColor(BodyPart part, boolean translucent) {
        float fraction = ClientBodyState.fraction(part);
        int rgb;
        if (fraction <= 0f) rgb = 0x38343D;
        else if (ClientBodyState.bleeding(part) == 2) rgb = 0xB83B47;
        else if (fraction <= 0.15f) rgb = 0xB83B47;
        else if (ClientBodyState.bleeding(part) == 1) rgb = 0xC96C50;
        else if (fraction <= 0.40f) rgb = 0xD1844D;
        else if (ClientBodyState.fractured(part)) rgb = 0xD0AA5C;
        else if (fraction <= 0.75f) rgb = 0xC7B56A;
        else rgb = 0x75A98A;
        return (translucent ? 0xB0000000 : 0xFF000000) | rgb;
    }
}
