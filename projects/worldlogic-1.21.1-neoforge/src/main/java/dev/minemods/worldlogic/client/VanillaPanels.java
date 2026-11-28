package dev.minemods.worldlogic.client;
import net.minecraft.client.gui.GuiGraphics;
/** Shared pixel-aligned vanilla inventory bevel; content is drawn afterwards. */
final class VanillaPanels{
 static final int TEXT=0xff383838,MUTED=0xff555555,PANEL=0xffc6c6c6,LINE=0xffaaaaaa;
 private VanillaPanels(){}
 static void backdrop(GuiGraphics g,int w,int h){g.fill(0,0,w,h,0x99000000);}
 static void panel(GuiGraphics g,int x,int y,int r,int b){
  if(r-x<8||b-y<8)return;
  g.fill(x+2,y,r-2,b,0xff373737);g.fill(x,y+2,r,b-2,0xff373737);
  g.fill(x+2,y+1,r-2,b-1,0xffeeeeee);g.fill(x+1,y+3,r-1,b-3,0xffeeeeee);
  g.fill(x+3,y+3,r-3,b-3,PANEL);
  g.fill(r-3,y+3,r-2,b-2,0xff555555);g.fill(x+3,b-3,r-2,b-2,0xff555555);
 }
 static void bar(GuiGraphics g,int x,int y,int w,double v){
  g.fill(x,y,x+w,y+5,0xff666666);
  if(w>2)g.fill(x+1,y+1,x+1+(int)((w-2)*v),y+4,0xff72965a);
 }
}
