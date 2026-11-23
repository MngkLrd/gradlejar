package dev.minemods.riverweir.menu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public final class WeirScreen extends AbstractContainerScreen<WeirMenu> {
    private static final ResourceLocation GUI = new ResourceLocation("minecraft", "textures/gui/container/generic_54.png");

    public WeirScreen(WeirMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = WeirLayout.imageHeight(menu.getNetSlots());
        inventoryLabelY = WeirLayout.inventoryLabelY(menu.getNetSlots());
    }

    @Override protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int upper = WeirLayout.upperHeight(menu.getNetSlots());
        graphics.blit(GUI, leftPos, topPos, 0, 0, 176, upper);
        if(menu.getNetSlots() == 2) {
            // Erase unused chest slots, then redraw exactly two centered slots
            // using the original 18x18 pixel Vanilla slot art (no fake slots).
            graphics.fill(leftPos + 7, topPos + 17, leftPos + 169, topPos + 35, 0xFFC6C6C6);
            graphics.blit(GUI, leftPos + 69, topPos + 17, 7, 17, 18, 18);
            graphics.blit(GUI, leftPos + 87, topPos + 17, 7, 17, 18, 18);
        }
        graphics.fill(leftPos, topPos + upper, leftPos + 176, topPos + upper + WeirLayout.INFORMATION_HEIGHT, 0xFFC6C6C6);
        graphics.blit(GUI, leftPos, topPos + upper + WeirLayout.INFORMATION_HEIGHT, 0, 126, 176, 96);
    }

    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, Component.translatable("gui.riverweir.header"), 8, 6, 0x404040, false);
        graphics.drawString(font, Component.translatable("gui.riverweir.condition_short", menu.getCondition()), 101, 6, 0x404040, false);
        int infoY = WeirLayout.upperHeight(menu.getNetSlots()) + 5;
        graphics.drawString(font, Component.translatable("gui.riverweir.clog", menu.getClog()), 8, infoY, 0x494338, false);
        graphics.drawString(font, Component.translatable("gui.riverweir.bait", menu.getBaitCharges()), 101, infoY, 0x494338, false);
        graphics.drawString(font, playerInventoryTitle, 8, inventoryLabelY, 0x404040, false);
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
