package dev.minemods.sleepdebt.client;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
public final class SleepHud {
 public static void render(GuiGraphics g,DeltaTracker delta){
  var mc=Minecraft.getInstance();
  if(!ClientSettings.VISIBLE.get()||mc.options.hideGui||mc.player==null||mc.player.isSpectator())return;
  int debt=Math.max(0,Math.min(1000,ClientState.debt));
  if(debt==0&&!ClientSettings.RESTED.get())return;
  int x=g.guiWidth()/2-91, y=g.guiHeight()-61+ClientSettings.OFFSET.get();
  int stage=debt>=830?5:debt>=620?4:debt>=420?3:debt>=250?2:debt>=120?1:0;
  int[] colors={0xFF8FBEA7,0xFFA9B8A0,0xFFC9B77C,0xFFB28A6F,0xFF95727A,0xFF764C65};
  g.fill(x,y,x+83,y+9,0xAA1D191A);
  g.drawString(mc.font,"Zz",x+3,y+1,0xFFE8DFC6,false);
  for(int i=0;i<10;i++){
   int xx=x+18+i*6;
   g.fill(xx,y+3,xx+5,y+7,0xFF343039);
   if(i*100<debt)g.fill(xx+1,y+4,xx+4,y+6,colors[stage]);
  }
  if(ClientState.maskTicks>0){
   int fill=Math.min(60,Math.max(1,ClientState.maskTicks/180));
   g.fill(x+18,y+8,x+18+fill,y+9,0xFFB78555);
  }else if(ClientState.crashTicks>0){
   int fill=Math.min(60,Math.max(1,ClientState.crashTicks/30));
   g.fill(x+18,y+8,x+18+fill,y+9,0xFFAF5852);
  }
 }
 private SleepHud(){}
}
