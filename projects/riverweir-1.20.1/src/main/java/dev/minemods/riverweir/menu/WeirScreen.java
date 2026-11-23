package dev.minemods.riverweir.menu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public final class WeirScreen extends AbstractContainerScreen<WeirMenu> {
    private static final ResourceLocation GUI=new ResourceLocation("minecraft","textures/gui/container/generic_54.png");
    public WeirScreen(WeirMenu menu,Inventory inventory,Component title) {
        super(menu,inventory,title);
        imageWidth=176;imageHeight=168;inventoryLabelY=74;
    }
    @Override protected void renderBg(GuiGraphics graphics,float partial,int mouseX,int mouseY) {
        graphics.blit(GUI,leftPos,topPos,0,0,176,71);
        graphics.blit(GUI,leftPos,topPos+71,0,126,176,96);
    }
    @Override public void render(GuiGraphics graphics,int mouseX,int mouseY,float partial) {
        renderBackground(graphics);
        super.render(graphics,mouseX,mouseY,partial);
        renderTooltip(graphics,mouseX,mouseY);
    }
}
