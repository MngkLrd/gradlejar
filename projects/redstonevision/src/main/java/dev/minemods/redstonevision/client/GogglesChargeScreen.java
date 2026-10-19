package dev.minemods.redstonevision.client;

import dev.minemods.redstonevision.item.GogglesPower;
import dev.minemods.redstonevision.menu.GogglesChargeMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class GogglesChargeScreen extends AbstractContainerScreen<GogglesChargeMenu> {
    public GogglesChargeScreen(GogglesChargeMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176; imageHeight = 166; inventoryLabelY = 72;
    }
    @Override protected void init() {
        super.init();
        addRenderableWidget(Button.builder(Component.translatable("screen.redstonevision.load"), button -> {
            if (minecraft != null && minecraft.gameMode != null)
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0);
        }).bounds(leftPos+106,topPos+35,62,20).build());
    }
    @Override protected void renderBg(GuiGraphics g, float partial, int mouseX, int mouseY) {
        g.fill(leftPos,topPos,leftPos+imageWidth,topPos+imageHeight,0xF0202630);
        g.fill(leftPos+3,topPos+3,leftPos+imageWidth-3,topPos+imageHeight-3,0xFF363D48);
        g.fill(leftPos+6,topPos+18,leftPos+imageWidth-6,topPos+66,0xFF242B34);
        g.fill(leftPos+79,topPos+35,leftPos+97,topPos+53,0xFF0E141B);
        g.fill(leftPos+7,topPos+83,leftPos+169,topPos+138,0xFF262D36);
        g.fill(leftPos+7,topPos+141,leftPos+169,topPos+160,0xFF262D36);
        for (int row=0;row<4;row++) for (int col=0;col<9;col++) {
            int x=leftPos+7+col*18,y=topPos+(row<3?83+row*18:141);
            g.fill(x,y,x+18,y+18,0xFF161D25);
            g.fill(x+1,y+1,x+17,y+17,0xFF303A45);
        }
        int filled=Math.round(156f*menu.chargeTicks()/GogglesPower.MAX_TICKS);
        g.fill(leftPos+10,topPos+58,leftPos+166,topPos+63,0xFF111923);
        if (filled>0) g.fill(leftPos+10,topPos+58,leftPos+10+filled,topPos+63,0xFFDA4545);
    }
    @Override protected void renderLabels(GuiGraphics g, int x, int y) {
        g.drawString(font,title,titleLabelX,6,0xFFEEEEEE,false);
        int s=(menu.chargeTicks()+19)/20;
        g.drawString(font,Component.translatable("screen.redstonevision.remaining",
            String.format(java.util.Locale.ROOT,"%d:%02d",s/60,s%60)),8,22,0xFFFFAAAA,false);
        g.drawString(font,Component.translatable("screen.redstonevision.insert"),8,45,0xFFCCCCCC,false);
        g.drawString(font,playerInventoryTitle,8,inventoryLabelY,0xFFDDDDDD,false);
    }
    @Override public void render(GuiGraphics g, int x,int y,float partial) {
        super.render(g,x,y,partial); renderTooltip(g,x,y);
    }
}
