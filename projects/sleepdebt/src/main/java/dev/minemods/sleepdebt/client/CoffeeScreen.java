package dev.minemods.sleepdebt.client;

import dev.minemods.sleepdebt.menu.CoffeeMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class CoffeeScreen extends AbstractContainerScreen<CoffeeMenu> {
 public CoffeeScreen(CoffeeMenu menu,Inventory playerInventory,Component title){
  super(menu,playerInventory,title);
  imageWidth=176;imageHeight=166;inventoryLabelY=72;titleLabelX=8;titleLabelY=7;
 }
 @Override protected void renderBg(GuiGraphics g,float partial,int mx,int my){
  int x=leftPos,y=topPos;
  // Vanilla-style stone gray backing and pixel-hard copper ornamentation.
  g.fill(x,y,x+176,y+166,0xFF2A2627);
  g.fill(x+3,y+3,x+173,y+163,0xFFC6C6C6);
  g.fill(x+5,y+5,x+171,y+77,0xFF88847D);
  g.fill(x+6,y+6,x+170,y+26,0xFF655145);
  g.fill(x+6,y+70,x+170,y+76,0xFF645A51);
  g.fill(x+6,y+78,x+170,y+160,0xFF9D9C96);
  // Labels atop slots are drawn with the native font in renderLabels.
  for(int sx:new int[]{27,54,81,135}){
   int sy=43;
   g.fill(x+sx-2,y+sy-2,x+sx+18,y+sy+18,0xFF49403B);
   g.fill(x+sx-1,y+sy-1,x+sx+17,y+sy+17,0xFFDFDBCE);
   g.fill(x+sx,y+sy,x+sx+16,y+sy+16,0xFF77716A);
  }
  g.fill(x+108,y+46,x+130,y+58,0xFF4B3830);
  int width=menu.progress()*20/menu.total();
  if(width>0)g.fill(x+109,y+47,x+109+width,y+57,0xFFC18B60);
  // Vanilla 9x4 slot grid background.
  for(int r=0;r<4;r++){
   int baseY=r==3?141:83+r*18;
   for(int c=0;c<9;c++){
    int sx=x+7+c*18;
    g.fill(sx, y+baseY, sx+18,y+baseY+18,0xFF514E4A);
    g.fill(sx+1,y+baseY+1,sx+17,y+baseY+17,0xFFCBC7BB);
    g.fill(sx+2,y+baseY+2,sx+16,y+baseY+16,0xFF89847E);
   }
  }
 }
 @Override protected void renderLabels(GuiGraphics g,int mx,int my){
  g.drawString(font,title,8,9,0xFFF1E5CD,false);
  g.drawString(font,"H2O",25,32,0xFFF5EEDB,false);
  g.drawString(font,"RAW",51,32,0xFFF5EEDB,false);
  g.drawString(font,"FUEL",77,32,0xFFF5EEDB,false);
  g.drawString(font,"OUT",133,32,0xFFF5EEDB,false);
  g.drawString(font,playerInventoryTitle,8,71,0xFF2D261E,false);
 }
 @Override public void render(GuiGraphics g,int mx,int my,float partial){
  renderBackground(g,mx,my,partial);
  super.render(g,mx,my,partial);
  renderTooltip(g,mx,my);
 }
}

