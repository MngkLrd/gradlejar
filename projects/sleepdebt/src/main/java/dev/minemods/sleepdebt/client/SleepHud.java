package dev.minemods.sleepdebt.client;

import dev.minemods.sleepdebt.SleepDebtMod;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** Native-font status panel. No custom fonts or screen-space blurring. */
public final class SleepHud {
 private static final ResourceLocation PANEL=ResourceLocation.fromNamespaceAndPath(
  SleepDebtMod.MOD_ID,"textures/gui/sleep_hud.png");
 private static int width(int value){return Math.max(0,Math.min(72,value*72/1000));}
 private static void meter(GuiGraphics g,int x,int y,int value,int color){
  int len=width(value);
  if(len<=0)return;
  // Broad connected tone-planes instead of a single flat 4px strip.
  int red=(color>>16)&255,green=(color>>8)&255,blue=color&255;
  int highlight=0xFF000000 | (Math.min(255,red+24)<<16)
     | (Math.min(255,green+21)<<8) | Math.min(255,blue+17);
  int shadow=0xFF000000 | (Math.max(0,red-25)<<16)
     | (Math.max(0,green-20)<<8) | Math.max(0,blue-21);
  g.fill(x+74,y+1,x+74+len,y+5,color);
  g.fill(x+74,y+1,x+74+len,y+2,highlight);
  g.fill(x+74,y+4,x+74+len,y+5,shadow);
  if(len>6)g.fill(x+74+len-2,y+2,x+74+len-1,y+4,highlight);
 }
 public static void render(GuiGraphics g,DeltaTracker delta){
  Minecraft mc=Minecraft.getInstance();
  if(mc.options.hideGui||mc.player==null||mc.player.isSpectator())return;
  if(!ClientSettings.VISIBLE.get())return;
  if(ClientState.debt==0&&ClientState.fatigue==0&&ClientState.stress==0&&
    !ClientSettings.RESTED.get())return;
  int x=7,y=7+ClientSettings.OFFSET.get();
  g.blit(PANEL,x,y,0f,0f,158,68,158,68);
  g.drawString(mc.font,Component.translatable("hud.sleepdebt.title"),
   x+12,y+8,0xFFE6C9A0,false);
  g.drawString(mc.font,Component.translatable("hud.sleepdebt.debt"),
   x+17,y+26,0xFFE0D9CF,false);
  g.drawString(mc.font,Component.translatable("hud.sleepdebt.fatigue"),
   x+17,y+39,0xFFE0D9CF,false);
  g.drawString(mc.font,Component.translatable("hud.sleepdebt.stress"),
   x+17,y+52,0xFFE0D9CF,false);
  meter(g,x,y+28,ClientState.debt,0xFFA65C67);
  meter(g,x,y+41,ClientState.fatigue,0xFFC79360);
  meter(g,x,y+54,ClientState.stress,0xFF9683B2);
  if(ClientState.maskTicks>0)g.drawString(mc.font,"+",x+141,y+8,0xFFCBA65D,false);
  else if(ClientState.crashTicks>0)g.drawString(mc.font,"!",x+122,y+6,0xFFD9796E,false);
 }
 private SleepHud(){}
}
