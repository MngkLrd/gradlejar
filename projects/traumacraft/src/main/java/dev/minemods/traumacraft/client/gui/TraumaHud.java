package dev.minemods.traumacraft.client.gui;

import dev.minemods.traumacraft.client.ClientBodyState;
import dev.minemods.traumacraft.health.BodyPart;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public final class TraumaHud {
    private TraumaHud() {}
    public static void render(GuiGraphics gui, DeltaTracker ignored) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || mc.screen != null
                || !ClientBodyState.hasReceived()) return;
        int x = Math.max(3, gui.guiWidth() / 2 - 91 - 64);
        int y = gui.guiHeight() - 65;
        int totalBleeding = 0;
        boolean fractured = false;
        for (BodyPart part : BodyPart.values()) {
            totalBleeding += ClientBodyState.bleeding(part);
            fractured |= ClientBodyState.fractured(part);
        }
        boolean injured = ClientBodyState.hasAnyInjuries();
        gui.fill(x, y, x + 56, y + 61, injured ? 0xAC101A1E : 0x65101A1E);
        gui.fill(x, y, x + 56, y + 1, 0x77667479);
        gui.drawString(mc.font, "HP", x + 4, y + 3,
                injured ? 0xFFE4E4E0 : 0xAAB3B8B4, false);
        gui.pose().pushPose();
        gui.pose().translate(x + 16, y + 13, 0);
        gui.pose().scale(0.43f, 0.43f, 1f);
        BodyVisual.draw(gui, 0, 0, true);
        gui.pose().popPose();
        if (totalBleeding > 0) {
            gui.drawString(mc.font, "B" + totalBleeding, x + 38, y + 18, 0xFFF0A3A3, false);
        }
        if (fractured) {
            gui.drawString(mc.font, "FX", x + 38, y + 31, 0xFFE8CF95, false);
        }
        if (injured && totalBleeding == 0 && !fractured) {
            gui.drawString(mc.font, "!", x + 43, y + 3, 0xFFE9B96F, false);
        }
    }
}
