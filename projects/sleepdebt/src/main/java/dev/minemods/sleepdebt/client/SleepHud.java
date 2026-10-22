package dev.minemods.sleepdebt.client;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
public final class SleepHud {
 public static void render(GuiGraphics g,DeltaTracker delta){
  var mc=Minecraft.getInstance();
  if(mc.options.hideGui||mc.player==null||mc.player.isSpectator())return;
  ClientHallucinations.render(g);
  if(!ClientSettings.VISIBLE.get())return;
  int debt=Math.max(0,Math.min(1000,ClientState.debt));
  if(debt==0&&!ClientSettings.RESTED.get())return;
  int x=g.guiWidth()/2-91,y=g.guiHeight()-61+ClientSettings.OFFSET.get();
  int stage=debt>=830?5:debt>=620?4:debt>=420?3:debt>=250?2:debt>=120?1:0;
  int[] colors={0xFF8FBEA7,0xFFA9B8A0,0xFFC9B77C,0xFFB28A6F,0xFF95727A,0xFF764C65};
  g.fill(x,y,x+83,y+15,0xA21D191A);
  g.drawString(mc.font,"Zz",x+3,y+1,0xFFE8DFC6,false);
  for(int i=0;i<10;i++){
   int xx=x+18+i*6;g.fill(xx,y+2,xx+5,y+7,0xFF343039);
   if(i*100<debt)g.fill(xx+1,y+3,xx+4,y+6,colors[stage]);
  }
  // Short lines are physical fatigue (warm copper) and stress (dusk blue).
  g.fill(x+18,y+10,x+78,y+11,0xFF343039);
  g.fill(x+18,y+10,x+18+Math.min(60,ClientState.fatigue*60/1000),y+11,0xFFC8916F);
  g.fill(x+18,y+12,x+78,y+13,0xFF343039);
  g.fill(x+18,y+12,x+18+Math.min(60,ClientState.stress*60/1000),y+13,0xFF8F8DA9);
  if(ClientState.maskTicks>0)g.fill(x+18,y+14,x+18+Math.min(60,ClientState.maskTicks/180),y+15,0xFFB78555);
  else if(ClientState.crashTicks>0)g.fill(x+18,y+14,x+18+Math.min(60,ClientState.crashTicks/30),y+15,0xFFAF5852);
 }
 private SleepHud(){}
}

