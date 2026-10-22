package dev.minemods.traumacraft.client.gui;

import dev.minemods.traumacraft.client.ClientTreatmentState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import java.util.Locale;

/** Quiet, pixel-crisp radial timer around the crosshair. */
public final class TreatmentRing {
    private static final int SEGMENTS = 64;
    private TreatmentRing() {}

    public static void render(GuiGraphics gui) {
        if (!ClientTreatmentState.active() || ClientTreatmentState.remaining() <= 0) return;
        Minecraft mc = Minecraft.getInstance();
        int cx = gui.guiWidth() / 2, cy = gui.guiHeight() / 2;
        float progress = ClientTreatmentState.fraction();
        // Mask the vanilla crosshair while treatment is active.
        gui.fill(cx - 8, cy - 8, cx + 9, cy + 9, 0xF01C2429);
        for (int i = 0; i < SEGMENTS; i++) {
            double angle = Math.PI * 2 * i / SEGMENTS - Math.PI / 2;
            int x = cx + (int)Math.round(Math.cos(angle) * 17);
            int y = cy + (int)Math.round(Math.sin(angle) * 17);
            gui.fill(x, y, x + 2, y + 2, 0xAA52616A);
            if (i / (float)SEGMENTS <= progress) {
                gui.fill(x, y, x + 2, y + 2, 0xFFE0E6DE);
            }
        }
        String time = String.format(Locale.ROOT, "%.1fs", ClientTreatmentState.remaining() / 20f);
        gui.drawString(mc.font, time, cx - mc.font.width(time)/2, cy - 4, 0xFFEAE8DC, true);
    }
}
