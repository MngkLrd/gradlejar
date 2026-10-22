package dev.minemods.traumacraft.client.gui;

import dev.minemods.traumacraft.client.ClientBodyState;
import dev.minemods.traumacraft.health.BodyPart;
import dev.minemods.traumacraft.item.MedicalItem;
import dev.minemods.traumacraft.network.ApplyMedicalPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Keeps InventoryMenu and the vanilla 176x166 inventory artwork intact.
 * Only the screen's overall layout is wider: do not blit a 320+ pixel rectangle
 * from the vanilla inventory PNG (it is only 256 pixels wide).
 */
public final class TraumaInventoryScreen extends InventoryScreen {
    private static final int VANILLA_WIDTH = 176;
    private static final int VANILLA_HEIGHT = 166;
    private static final int DESIRED_PANEL_WIDTH = 208;
    private static final int PANEL_HEIGHT = 174;
    private static final int BODY_OFFSET_X = 10;
    private static final int BODY_OFFSET_Y = 34;

    private int panelWidth = DESIRED_PANEL_WIDTH;
    private BodyPart hovered;

    public TraumaInventoryScreen(Player player) {
        super(player);
        this.imageWidth = VANILLA_WIDTH + DESIRED_PANEL_WIDTH;
        this.imageHeight = PANEL_HEIGHT;
    }

    @Override
    protected void init() {
        // Narrow screens get a condensed version without a horizontally clipped panel.
        this.panelWidth = Math.max(158, Math.min(DESIRED_PANEL_WIDTH, this.width - VANILLA_WIDTH - 8));
        this.imageWidth = VANILLA_WIDTH + panelWidth;
        this.imageHeight = PANEL_HEIGHT;
        super.init();
    }

    private int panelX() { return leftPos + VANILLA_WIDTH; }
    private int rowX() { return panelX() + 65; }
    private int rowRight() { return panelX() + panelWidth - 8; }
    private int bodyX() { return panelX() + BODY_OFFSET_X; }
    private int bodyY() { return topPos + BODY_OFFSET_Y; }

    @Override
    protected void renderBg(GuiGraphics gui, float partialTick, int mouseX, int mouseY) {
        // Vanilla InventoryScreen.renderBg uses imageWidth/imageHeight as source
        // texture dimensions. Temporarily restore the vanilla bounds for this blit.
        final int savedWidth = imageWidth, savedHeight = imageHeight;
        imageWidth = VANILLA_WIDTH;
        imageHeight = VANILLA_HEIGHT;
        try {
            super.renderBg(gui, partialTick, mouseX, mouseY);
        } finally {
            imageWidth = savedWidth;
            imageHeight = savedHeight;
        }

        final int x = panelX(), y = topPos, r = x + panelWidth;
        gui.fill(x - 1, y, r + 1, y + PANEL_HEIGHT, 0xFF383838);
        gui.fill(x, y, r, y + PANEL_HEIGHT - 1, 0xFFE7E7E7);
        gui.fill(x + 1, y + 1, r - 1, y + PANEL_HEIGHT - 2, 0xFFC6C6C6);
        gui.fill(r - 2, y + 1, r - 1, y + PANEL_HEIGHT - 1, 0xFF565656);
        gui.fill(x + 3, y + 17, r - 4, y + 18, 0xFF777777);
        gui.drawString(font, Component.translatable("screen.traumacraft.body"), x + 7, y + 5, 0xFF343434, false);

        // Anatomical overview on the left. All coordinates are shared with the HUD.
        gui.fill(x + 6, y + 22, x + 60, y + 158, 0xFF5F6466);
        gui.fill(x + 8, y + 24, x + 58, y + 156, 0xFF252B2E);
        BodyVisual.draw(gui, bodyX(), bodyY(), false);
        drawSummary(gui, x + 10, y + 135);

        hovered = BodyVisual.hit(mouseX, mouseY, bodyX(), bodyY());
        for (int i = 0; i < BodyPart.values().length; i++) {
            final BodyPart part = BodyPart.values()[i];
            final int rowY = y + 20 + i * 20;
            if (mouseX >= rowX() - 2 && mouseX < rowRight() + 3
                    && mouseY >= rowY && mouseY < rowY + 20) hovered = part;
        }
        for (int i = 0; i < BodyPart.values().length; i++) {
            drawPartRow(gui, BodyPart.values()[i], rowX(), y + 20 + i * 20,
                    rowRight(), hovered == BodyPart.values()[i]);
        }
        gui.drawString(font, Component.translatable("screen.traumacraft.drag_to_treat"),
                x + 8, y + PANEL_HEIGHT - 11, 0xFF505050, false);
    }

    private void drawSummary(GuiGraphics gui, int x, int y) {
        int bleed = 0, fractures = 0;
        for (BodyPart part : BodyPart.values()) {
            bleed += ClientBodyState.bleeding(part);
            if (ClientBodyState.fractured(part)) fractures++;
        }
        gui.drawString(font, "BLD " + bleed, x, y, bleed > 0 ? 0xFFFF8C8C : 0xFF969FA0, false);
        gui.drawString(font, "FX " + fractures, x, y + 10, fractures > 0 ? 0xFFF2D28C : 0xFF969FA0, false);
    }

    private void drawPartRow(GuiGraphics gui, BodyPart part, int x, int y, int right, boolean focused) {
        if (focused) gui.fill(x - 3, y - 1, right + 3, y + 20, 0x5598B49B);
        final String title = switch (part) {
            case HEAD -> "HEAD"; case CHEST -> "CHEST"; case STOMACH -> "ABDOMEN";
            case LEFT_ARM -> "L.ARM"; case RIGHT_ARM -> "R.ARM";
            case LEFT_LEG -> "L.LEG"; case RIGHT_LEG -> "R.LEG";
        };
        String hp = Math.round(ClientBodyState.hp(part)) + "/" + part.maximum;
        gui.drawString(font, title, x, y, 0xFF343434, false);
        gui.drawString(font, hp, right - font.width(hp), y, 0xFF343434, false);

        // Explicit injury text prevents bleeding/fracture from being hidden by the HP gauge.
        String status;
        int statusColor = 0xFF6B7475;
        final int bleeding = ClientBodyState.bleeding(part);
        final boolean fracture = ClientBodyState.fractured(part);
        if (ClientBodyState.hp(part) <= 0) {
            status = "BLACKED"; statusColor = 0xFF933A42;
        } else if (bleeding > 0 && fracture) {
            status = "BLEED " + bleeding + " + FX"; statusColor = 0xFF9A353A;
        } else if (bleeding == 2) {
            status = "HEAVY BLEED"; statusColor = 0xFF9A353A;
        } else if (bleeding == 1) {
            status = "BLEEDING"; statusColor = 0xFFA2513C;
        } else if (fracture) {
            status = "FRACTURE"; statusColor = 0xFF956F2F;
        } else if (ClientBodyState.hp(part) < part.maximum) {
            status = "INJURED"; statusColor = 0xFF84652C;
        } else {
            status = "STABLE";
        }
        gui.drawString(font, status, x, y + 9, statusColor, false);

        // A slim HP gauge on each line, entirely inside its allocated 20px row.
        final int bw = right - x;
        gui.fill(x, y + 18, right, y + 20, 0xFF424748);
        int filled = Math.round(bw * ClientBodyState.fraction(part));
        if (filled > 0)
            gui.fill(x, y + 18, x + filled, y + 20, BodyVisual.hpColor(part, false));
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        super.render(gui, mouseX, mouseY, partialTick);
        TreatmentRing.render(gui);
        if (hovered == null) return;
        List<Component> tooltip = new ArrayList<>();
        tooltip.add(Component.literal(hovered.label));
        tooltip.add(Component.literal(String.format(Locale.ROOT,
                "HP: %.0f / %d", ClientBodyState.hp(hovered), hovered.maximum)));
        if (ClientBodyState.hp(hovered) <= 0) tooltip.add(Component.literal("Blackened / destroyed"));
        if (ClientBodyState.bleeding(hovered) > 0)
            tooltip.add(Component.literal(ClientBodyState.bleeding(hovered) == 2 ? "Heavy bleeding" : "Light bleeding"));
        if (ClientBodyState.fractured(hovered)) tooltip.add(Component.literal("Fracture"));
        if (this.menu.getCarried().getItem() instanceof MedicalItem)
            tooltip.add(Component.translatable("screen.traumacraft.click_to_treat"));
        // Keep long tooltips on-screen instead of clipping their right half.
        int widest = 0;
        for (Component component : tooltip) widest = Math.max(widest, font.width(component));
        int tipX = Math.max(8, Math.min(mouseX, this.width - widest - 20));
        int tipY = Math.max(8, Math.min(mouseY, this.height - tooltip.size() * 11 - 16));
        gui.renderComponentTooltip(font, tooltip, tipX, tipY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 || button == 1) {
            BodyPart part = findPart((int) mouseX, (int) mouseY);
            if (part != null) {
                if (this.menu.getCarried().getItem() instanceof MedicalItem) {
                    PacketDistributor.sendToServer(new ApplyMedicalPacket(part.ordinal()));
                }
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private BodyPart findPart(int mouseX, int mouseY) {
        BodyPart part = BodyVisual.hit(mouseX, mouseY, bodyX(), bodyY());
        if (part != null) return part;
        if (mouseX < rowX() - 2 || mouseX >= rowRight() + 3) return null;
        for (int i = 0; i < BodyPart.values().length; i++) {
            int y = topPos + 20 + i * 20;
            if (mouseY >= y && mouseY < y + 20) return BodyPart.values()[i];
        }
        return null;
    }
}
