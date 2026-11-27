package dev.minemods.worldlogic.client;
import dev.minemods.worldlogic.api.WorldLogicApi;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
public final class FatigueHud {
 private FatigueHud(){}
 public static void render(GuiGraphics g){
  var mc=Minecraft.getInstance();var p=mc.player;
  if(p==null||!p.isAlive()||p.isCreative()||p.isSpectator()||mc.options.hideGui||mc.screen!=null)return;
  var value=WorldLogicApi.clientFatiguePercent();if(value.isEmpty())return;
  int f=Math.max(0,Math.min(100,value.getAsInt()));
  int w=g.guiWidth(),h=g.guiHeight(),x=8,y=h-22;
  var label=Component.translatable("hud.worldlogic.fatigue").append(" "+f+"%");
  int width=Math.max(62,mc.font.width(label));
  if(x+width+4>w/2-94)y=8;
  g.drawString(mc.font,label,x,y,0xffeeeeee,true);
  g.fill(x-1,y+10,x+width+1,y+15,0xff201c19);
  g.fill(x,y+11,x+width,y+14,0xff514740);
  int filled=(width*f+50)/100;
  if(filled>0)g.fill(x,y+11,x+filled,y+14,f>=75?0xffbe5040:f>=40?0xffc69442:0xff839c59);
 }
}
