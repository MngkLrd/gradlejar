package dev.minemods.sleepdebt.client;

import dev.minemods.sleepdebt.SleepDebtMod;
import dev.minemods.sleepdebt.gameplay.RestEnvironment;
import dev.minemods.sleepdebt.network.SleepReportPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** A native Minecraft-font 228x188 pixel-art book screen. */
public final class SleepJournalScreen extends Screen {
 private static final ResourceLocation BG=ResourceLocation.fromNamespaceAndPath(
  SleepDebtMod.MOD_ID,"textures/gui/sleep_journal.png");
 private final SleepReportPayload report;
 public SleepJournalScreen(SleepReportPayload report){
  super(Component.translatable("gui.sleepdebt.journal_title"));
  this.report=report;
 }
 public static void open(SleepReportPayload report){
  Minecraft.getInstance().setScreen(new SleepJournalScreen(report));
 }
 private static int scaled(int value,int max){return Math.max(0,Math.min(112,value*112/max));}
 private void bar(GuiGraphics g,int x,int y,int value,int maximum,int tint){
  g.fill(x,y,x+114,y+7,0xFF514438);
  g.fill(x+1,y+1,x+113,y+6,0xFF9B8D78);
  int fill=scaled(value,maximum);
  if(fill>0)g.fill(x+1,y+1,x+1+fill,y+6,tint);
 }
 private static Component text(String key){return Component.translatable(key);}
 @Override public void render(GuiGraphics gui,int mouseX,int mouseY,float partial){
  renderBackground(gui,mouseX,mouseY,partial);
  int x=(width-228)/2,y=(height-188)/2;
  gui.blit(BG,x,y,0f,0f,228,188,228,188);
  gui.drawCenteredString(font,title,x+114,y+12,0xFF392A24);
  gui.drawString(font,text("gui.sleepdebt.debt"),x+16,y+37,0xFF4A382D,false);
  gui.drawString(font,String.valueOf(report.debt()),x+183,y+37,0xFF4A382D,false);
  bar(gui,x+16,y+48,report.debt(),1000,0xFF9C575B);
  gui.drawString(font,text("gui.sleepdebt.fatigue"),x+16,y+65,0xFF4A382D,false);
  gui.drawString(font,String.valueOf(report.fatigue()),x+183,y+65,0xFF4A382D,false);
  bar(gui,x+16,y+76,report.fatigue(),1000,0xFFC68C61);
  gui.drawString(font,text("gui.sleepdebt.stress"),x+16,y+93,0xFF4A382D,false);
  gui.drawString(font,String.valueOf(report.stress()),x+183,y+93,0xFF4A382D,false);
  bar(gui,x+16,y+104,report.stress(),1000,0xFF8F7A99);
  if(report.quality()>0){
   gui.drawString(font,Component.translatable("gui.sleepdebt.last_sleep",
     report.quality(),report.recovered()),x+16,y+125,0xFF4A382D,false);
  }else{
   gui.drawString(font,text("gui.sleepdebt.no_sleep"),x+16,y+125,0xFF625446,false);
  }
  gui.drawString(font,Component.translatable("gui.sleepdebt.forecast",report.forecast()),
    x+16,y+140,0xFF4A382D,false);
  String reason="gui.sleepdebt.reason.safe";
  int r=report.forecastReasons();
  if((r&RestEnvironment.HOSTILES)!=0)reason="gui.sleepdebt.reason.danger";
  else if((r&RestEnvironment.RAIN)!=0)reason="gui.sleepdebt.reason.rain";
  else if((r&RestEnvironment.COLD)!=0)reason="gui.sleepdebt.reason.cold";
  else if((r&RestEnvironment.CAFFEINE)!=0)reason="gui.sleepdebt.reason.caffeine";
  else if((r&RestEnvironment.STRESS)!=0)reason="gui.sleepdebt.reason.stress";
  else if((r&RestEnvironment.PROTECTION)!=0)reason="gui.sleepdebt.reason.protected";
  gui.drawString(font,text(reason),x+16,y+155,0xFF625446,false);
  gui.drawCenteredString(font,text("gui.sleepdebt.esc"),x+114,y+171,0xFF716457);
  super.render(gui,mouseX,mouseY,partial);
 }
}
