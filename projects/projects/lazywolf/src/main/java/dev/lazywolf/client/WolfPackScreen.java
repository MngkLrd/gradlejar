package dev.lazywolf.client;

import dev.lazywolf.WolfPackMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public final class WolfPackScreen extends AbstractContainerScreen<WolfPackMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/gui/container/generic_54.png");

    public WolfPackScreen(WolfPackMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageHeight = 132;
        this.inventoryLabelY = 38;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos, y = this.topPos;
        graphics.blit(TEXTURE, x, y, 0, 0, 176, 35);
        graphics.blit(TEXTURE, x, y + 35, 0, 126, 176, 96);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
