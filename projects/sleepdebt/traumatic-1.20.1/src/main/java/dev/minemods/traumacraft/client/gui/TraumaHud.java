package dev.minemods.traumacraft.client.gui;

import dev.minemods.traumacraft.client.ClientBodyState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public final class TraumaHud {
    private TraumaHud() {}
    public static void render(GuiGraphics gui) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || mc.screen != null || !ClientBodyState.hasReceived()) return;
        int x = gui.guiWidth()/2 - 91 - 66;
        int y = gui.guiHeight() - 67;
        boolean injured = ClientBodyState.hasAnyInjuries();
        int alpha = injured ? 0xA9000000 : 0x62000000;
        gui.fill(x, y, x+57, y+61, alpha | 0x0010161C);
        gui.fill(x, y, x+57, y+1, 0x775F7777);
        gui.fill(x, y, x+1, y+61, 0x775F7777);
        gui.fill(x+56, y, x+57, y+61, 0x55000000);
        gui.drawString(mc.font, "HP", x+4, y+4, injured ? 0xFFDBDDDB : 0xAAB3B8B4, false);
        gui.pose().pushPose();
        gui.pose().translate(x+16, y+14, 0);
        gui.pose().scale(0.42f, 0.42f, 1f);
        BodyVisual.draw(gui, 0, 0, true);
        gui.pose().popPose();
        if (injured) {
            gui.drawString(mc.font, "!", x+44, y+4, 0xFFF1AE60, true);
        }
    }
}
