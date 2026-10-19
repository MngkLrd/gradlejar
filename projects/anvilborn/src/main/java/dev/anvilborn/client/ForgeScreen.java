package dev.anvilborn.client;

import dev.anvilborn.forge.ForgeData;
import dev.anvilborn.forge.ForgeMenu;
import dev.anvilborn.forge.ForgeRules;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public final class ForgeScreen extends AbstractContainerScreen<ForgeMenu> {
    private int previousSequence = -1;
    private int flashTicks;
    public ForgeScreen(ForgeMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 190;
        inventoryLabelY = 91;
        titleLabelX = 9;
        titleLabelY = 7;
    }
    @Override protected void init() {
        super.init();
        addRenderableWidget(Button.builder(Component.translatable("button.anvilborn.strike"), button -> {
            if (minecraft != null && minecraft.gameMode != null)
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0);
        }).bounds(leftPos + 106, topPos + 36, 61, 21).build());
    }
    @Override protected void containerTick() {
        super.containerTick();
        if (menu.sequence() != previousSequence) {
            if (previousSequence != -1) flashTicks = 9;
            previousSequence = menu.sequence();
        }
        if (flashTicks > 0) flashTicks--;
    }
    @Override protected void renderBg(GuiGraphics g, float partialTick, int mx, int my) {
        int x = leftPos, y = topPos;
        g.fill(x - 3, y - 3, x + 179, y + 193, 0xff171b1d);
        g.fill(x - 1, y - 1, x + 177, y + 191, 0xff727575);
        g.fill(x + 2, y + 2, x + 174, y + 188, 0xffbcb8ae);
        g.fill(x + 5, y + 19, x + 171, y + 88, 0xff4a4644);
        g.fill(x + 7, y + 21, x + 169, y + 86, 0xff2d3034);
        drawSlot(g, x + 79, y + 37);
        g.drawString(font, "ANVILBORN", x + 9, y + 23, 0xfff3cb86, false);

        ItemStack stack = menu.workpiece();
        ForgeData data = ForgeRules.data(stack);
        g.fill(x + 11, y + 71, x + 165, y + 79, 0xff111111);
        g.fill(x + 12, y + 72, x + 12 + Math.round(152 * data.hits() / (float)ForgeRules.TARGET_HITS), y + 78,
            data.frenzy() > 0 ? 0xffffb236 : 0xffda914c);
        int chance = data.frenzy() > 0 ? 0 : ForgeRules.breakChance(data.level());
        g.drawString(font, stack.isEmpty() ? "Insert equipment" : "Quality: " + data.level() + "   Break: " + chance + "%",
            x + 10, y + 61, chance >= 20 ? 0xffff9d9d : 0xffe5e5e5, false);
        if (data.frenzy() > 0) g.drawString(font, "FRENZY " + data.frenzy(), x + 107, y + 24, 0xffffc258, false);
        if (flashTicks > 0) {
            int c = switch (menu.resultCode()) {
                case 1, 2 -> 0x55f5c46b; case 3, 4 -> 0x66ef5555; default -> 0x33ffffff;
            };
            g.fill(x + 7, y + 21, x + 169, y + 86, c);
        }
        for (int row=0; row<3; row++) for (int col=0; col<9; col++)
            drawSlot(g, x + 7 + col * 18, y + 102 + row * 18);
        for (int col=0; col<9; col++) drawSlot(g, x + 7 + col*18, y + 160);
    }
    private void drawSlot(GuiGraphics g, int x, int y) {
        g.fill(x, y, x + 18, y + 18, 0xff353334);
        g.fill(x + 1, y + 1, x + 17, y + 17, 0xffded9ca);
        g.fill(x + 2, y + 2, x + 16, y + 16, 0xff777974);
    }
    @Override protected void renderLabels(GuiGraphics g, int mx, int my) {
        g.drawString(font, title, titleLabelX, titleLabelY, 0xff31312f, false);
        g.drawString(font, playerInventoryTitle, 9, inventoryLabelY, 0xff333333, false);
    }
    @Override public void render(GuiGraphics g, int mx, int my, float partialTick) {
        renderBackground(g, mx, my, partialTick);
        super.render(g, mx, my, partialTick);
        renderTooltip(g, mx, my);
    }
}
