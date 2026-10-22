package dev.minemods.sleepdebt.client;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
/** Pure client display; does not spawn entities, modify physics or obscure menus. */
public final class ClientHallucinations {
 public static void render(GuiGraphics g){
  Minecraft mc=Minecraft.getInstance();
  if(!ClientSettings.HALLUCINATIONS.get()||mc.player==null||mc.level==null||mc.options.hideGui)return;
  if(ClientState.debt<620||ClientState.maskTicks>0)return;
  long t=mc.level.getGameTime();
  long period=t/180;
  // Only sparse two-second windows, deterministic for a specific player.
  long seed=(period*41371L+mc.player.getUUID().getLeastSignificantBits())&0x7fffffff;
  if(seed%9!=0||t%180>35)return;
  int w=g.guiWidth(),h=g.guiHeight();
  int x=(seed%2==0)?13:w-30;
  int y=h/3+(int)((seed/7)%50);
  // Dark distant eyes: intentionally tiny and peripheral, with no collision or world entity.
  g.fill(x,y,x+3,y+2,0x844A3F60);
  g.fill(x+10,y-1,x+13,y+1,0x845D476D);
  if(ClientState.debt>830 && t%180<16) {
   g.fill(0,0,w,4,0x1E100F16);
   g.fill(0,h-4,w,h,0x1E100F16);
  }
 }
 private ClientHallucinations(){}
}

