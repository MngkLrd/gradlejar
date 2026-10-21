package dev.minemods.traumacraft.client;

import dev.minemods.traumacraft.TraumaCraft;
import dev.minemods.traumacraft.health.BodyPart;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

@Mod.EventBusSubscriber(modid = TraumaCraft.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class MedicalHud {
    private MedicalHud() {}
    @SubscribeEvent
    public static void addLayer(RegisterGuiLayersEvent event) {
        event.registerAboveAll(ResourceLocation.fromNamespaceAndPath(TraumaCraft.MOD_ID, "body_hud"),
                (graphics, delta) -> draw(graphics));
    }

    private static void draw(GuiGraphics g) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) { ClientBodyCache.clear(); return; }
        if (mc.screen != null || mc.options.hideGui || !ClientBodyCache.ready()) return;
        int x = 6;
        int y = g.guiHeight() - (g.guiWidth() < 400 ? 150 : 92);
        if (y < 3) return;
        // Semi-transparent, smaller than vanilla health/food hotbar; no new hearts.
        g.fill(x, y, x + 83, y + 79, 0x88090F11);
        g.fill(x, y, x + 83, y + 1, 0xAA6C7777);
        g.fill(x, y + 78, x + 83, y + 79, 0xAA263336);
        g.drawString(mc.font, "BODY", x + 5, y + 4, 0xCCDCE4DC, false);
        g.pose().pushPose();
        g.pose().translate(x + 5, y + 18, 0);
        g.pose().scale(0.53f, 0.53f, 1f);
        MedicalPanelPainter.figure(g, 0, 0, null, true);
        g.pose().popPose();
        for (BodyPart part : BodyPart.values()) {
            int i = part.ordinal();
            int bx = x + 39;
            int by = y + 14 + 9 * i;
            g.fill(bx, by, bx + 37, by + 6, 0x88192022);
            int width = (int) (35 * ClientBodyCache.percent(part));
            if (width > 0) g.fill(bx + 1, by + 1, bx + 1 + width, by + 5, MedicalPanelPainter.condition(part));
            if (ClientBodyCache.bleed(part) > 0) g.fill(bx + 34, by, bx + 37, by + 3, 0xFFE64639);
            if (ClientBodyCache.broken(part)) g.fill(bx + 34, by + 3, bx + 37, by + 6, 0xFFE7B258);
        }
    }
}
