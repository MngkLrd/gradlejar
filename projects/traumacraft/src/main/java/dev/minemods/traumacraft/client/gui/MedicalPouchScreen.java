package dev.minemods.traumacraft.client.gui;

import dev.minemods.traumacraft.menu.MedicalPouchMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Small vanilla-style medical case with a visible 1x2 reserved cell. */
public final class MedicalPouchScreen extends AbstractContainerScreen<MedicalPouchMenu> {
    public MedicalPouchScreen(MedicalPouchMenu menu,Inventory playerInventory,Component title) {
        super(menu,playerInventory,title);
        imageWidth=176;
        imageHeight=154;
        inventoryLabelY=60;
        titleLabelY=6;
    }
    @Override protected void renderBg(GuiGraphics gui,float partialTick,int mx,int my) {
        int x=leftPos,y=topPos;
        gui.fill(x+2,y,x+174,y+154,0xFF383838);
        gui.fill(x,y+2,x+176,y+152,0xFF383838);
        gui.fill(x+3,y+3,x+173,y+151,0xFFC6C6C6);
        gui.fill(x+4,y+4,x+172,y+5,0xFFFFFFFF);
        for(int row=0;row<2;row++)for(int col=0;col<2;col++){
            int i=row*2+col;
            int sx=x+69+col*18,sy=y+24+row*18;
            gui.fill(sx,sy,sx+18,sy+18,0xFF434343);
            gui.fill(sx+1,sy+1,sx+17,sy+17,menu.reserved(i)?0xFF566368:0xFF8B8B8B);
            if(menu.reserved(i)) {
                gui.fill(sx+8,sy+3,sx+9,sy+14,0xFF9FB8AA);
                gui.fill(sx+3,sy+8,sx+14,sy+9,0xFF9FB8AA);
            }
        }
        for(int row=0;row<3;row++)for(int col=0;col<9;col++)slot(gui,x+7+col*18,y+71+row*18);
        for(int col=0;col<9;col++)slot(gui,x+7+col*18,y+129);
    }
    private void slot(GuiGraphics gui,int x,int y) {
        gui.fill(x,y,x+18,y+18,0xFF565656);
        gui.fill(x+1,y+1,x+17,y+17,0xFF8B8B8B);
    }
    @Override public void render(GuiGraphics gui,int mx,int my,float delta) {
        super.render(gui,mx,my,delta);
        renderTooltip(gui,mx,my);
    }
}
