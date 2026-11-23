package dev.minemods.riverweir.menu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public final class WeirScreen extends AbstractContainerScreen<WeirMenu> {
    private static final ResourceLocation GUI=new ResourceLocation("minecraft","textures/gui/container/generic_54.png");
    public WeirScreen(WeirMenu menu,Inventory inventory,Component title){
        super(menu,inventory,title);
        imageWidth=176;
        imageHeight=132+menu.getRows()*18;
        inventoryLabelY=imageHeight-94;
    }
    @Override protected void renderBg(GuiGraphics g,float partial,int mouseX,int mouseY){
        int netRows=menu.getRows(),upper=17+netRows*18;
        g.blit(GUI,leftPos,topPos,0,0,176,upper);
        // Extra hand-maintenance readout, outside slots. No tooltips hiding the catch.
        g.fill(leftPos,topPos+upper,leftPos+176,topPos+upper+18,0xFFC6C6C6);
        g.blit(GUI,leftPos,topPos+upper+18,0,126,176,96);
    }
    @Override protected void renderLabels(GuiGraphics g,int mouseX,int mouseY){
        g.drawString(font,Component.translatable("gui.riverweir.header"),8,6,0x404040,false);
        g.drawString(font,Component.translatable("gui.riverweir.condition_short",menu.getCondition()),101,6,0x404040,false);
        int infoY=17+menu.getRows()*18+5;
        g.drawString(font,Component.translatable("gui.riverweir.clog",menu.getClog()),8,infoY,0x494338,false);
        g.drawString(font,Component.translatable("gui.riverweir.bait",menu.getBaitCharges()),101,infoY,0x494338,false);
        g.drawString(font,playerInventoryTitle,8,inventoryLabelY,0x404040,false);
    }
    @Override public void render(GuiGraphics g,int mouseX,int mouseY,float partial){
        renderBackground(g);
        super.render(g,mouseX,mouseY,partial);
        renderTooltip(g,mouseX,mouseY);
    }
}
