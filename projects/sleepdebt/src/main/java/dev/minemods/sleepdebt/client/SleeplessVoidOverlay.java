package dev.minemods.sleepdebt.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.minemods.sleepdebt.entity.SleeplessEntity;
import java.util.Locale;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import org.lwjgl.glfw.GLFW;

/**
 * A separate testable GUI compositing backend, not vanilla Darkness or fog.
 * The event is on NeoForge.EVENT_BUS, not the mod's GUI-layer system.
 * Entity detection is intentionally independent of ambient audio ticks.
 */
public final class SleeplessVoidOverlay {
 private static final KeyMapping TEST=new KeyMapping("key.sleepdebt.test_void",
   InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_F8,"key.categories.sleepdebt");
 private static final KeyMapping DEBUG=new KeyMapping("key.sleepdebt.void_diagnostics",
   InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_F9,"key.categories.sleepdebt");

 private static Object level,player;
 private static int scanned=-10000,seenCount,preFrames,postFrames,lastAlpha;
 private static long lastSeen=-10000L;
 private static float nearest=64,pressure,target;
 private static boolean forced,debug;

 public static void registerKeys(RegisterKeyMappingsEvent e){
  e.register(TEST);
  e.register(DEBUG);
 }

 public static void tickKeys(ClientTickEvent.Post e){
  Minecraft mc=Minecraft.getInstance();
  if(mc.player==null||mc.level==null)return;
  while(TEST.consumeClick()){
   forced=!forced;
   mc.player.displayClientMessage(Component.literal("Sleep Debt VOID: "+
     (forced?"FORCED BLACKOUT":"automatic")),true);
  }
  while(DEBUG.consumeClick()){
   debug=!debug;
   mc.player.displayClientMessage(Component.literal("Sleep Debt VOID diagnostics: "+
     (debug?"ON":"OFF")),true);
  }
 }

 private static void scan(Minecraft mc){
  if(level!=mc.level||player!=mc.player){
   level=mc.level;player=mc.player;scanned=-10000;lastSeen=-10000L;
   nearest=64;pressure=0;target=0;seenCount=0;
  }
  if(mc.player.tickCount==scanned)return;
  scanned=mc.player.tickCount;
  nearest=64;seenCount=0;
  for(SleeplessEntity mob:mc.level.getEntitiesOfClass(SleeplessEntity.class,
      mc.player.getBoundingBox().inflate(52))){
   if(!mob.isAlive())continue;
   seenCount++;
   nearest=Math.min(nearest,mc.player.distanceTo(mob));
  }
  long now=mc.level.getGameTime();
  if(seenCount>0)lastSeen=now;
  if(!mc.level.isDay()&&!mc.player.isSleeping()&&
     !mc.player.isSpectator()&&ClientSettings.VOID_ENABLED.get()){
   float proximity=Mth.clamp((41f-nearest)/36f,0f,1f);
   // Hold darkness briefly during the hunter's vanish/teleport animation.
   if(seenCount==0&&now-lastSeen<55){
    float fade=Mth.clamp((now-lastSeen-15)/40f,0f,1f);
    proximity=.78f*(1f-fade);
   }
   target=Mth.clamp(proximity*
     ClientSettings.VOID_STRENGTH.get().floatValue(),0f,1f);
  }else target=0;
 }

 public static void renderPre(RenderGuiEvent.Pre e){
  Minecraft mc=Minecraft.getInstance();
  if(mc.level==null||mc.player==null||mc.screen!=null)return;
  preFrames++;
  scan(mc);
  float wanted=forced?1f:target;
  pressure+=(wanted-pressure)*(wanted>pressure?.24f:.10f);
  if(forced)pressure=1f;
  if(pressure<.002f){lastAlpha=0;return;}
  GuiGraphics g=e.getGuiGraphics();
  int w=g.guiWidth(),h=g.guiHeight();
  if(w<=0||h<=0)return;
  paint(g,w,h,pressure,mc.player.tickCount);
  // Commit the black layer BEFORE vanilla HUD and any mod can reset state.
  g.flush();
 }

 public static void renderPost(RenderGuiEvent.Post e){
  postFrames++;
  Minecraft mc=Minecraft.getInstance();
  if(mc.level==null||mc.player==null||mc.screen!=null)return;
  if(!debug&&!ClientSettings.VOID_DEBUG.get())return;
  GuiGraphics g=e.getGuiGraphics();
  int y=g.guiHeight()-32;
  g.fill(2,y,Math.min(g.guiWidth()-2,330),g.guiHeight()-2,0xE0000000);
  String first=String.format(Locale.ROOT,
    "VOID PRE:%d POST:%d real:%d nearest:%.1fm",
    preFrames,postFrames,seenCount,nearest);
  String second=String.format(Locale.ROOT,
    "F8:%s config:%s target:%.2f pressure:%.2f alpha:%d",
    forced,ClientSettings.VOID_ENABLED.get(),target,pressure,lastAlpha);
  g.drawString(mc.font,first,5,y+3,0xFFFFFFFF,true);
  g.drawString(mc.font,second,5,y+16,0xFFFFC989,true);
  g.flush();
 }

 private static void paint(GuiGraphics g,int w,int h,float p,int tick){
  p=Mth.clamp(p,0,1);
  float t=tick*.06f;
  float pulse=(float)(Math.sin(t*1.34)*.025+Math.cos(t*.72)*.016);
  float effective=Mth.clamp(p+p*pulse,0,1);
  lastAlpha=Mth.clamp((int)(248*Math.pow(effective,1.06)),0,248);
  // Independent black compositing: covers full world including torch light.
  g.fill(0,0,w,h,lastAlpha<<24);
  for(int i=9;i>=1;i--){
   float scale=i/9f;
   float ripple=(float)(.96+.04*Math.sin(t*.7+i*.81));
   int side=(int)(w*.48*p*scale*ripple);
   int top=(int)(h*.47*p*scale*ripple);
   int a=Mth.clamp((int)(24*p*(1.23-.23*scale)),0,34);
   if(side>0){
    g.fill(0,0,side,h,a<<24);
    g.fill(w-side,0,w,h,a<<24);
   }
   if(top>0){
    g.fill(0,0,w,top,a<<24);
    g.fill(0,h-top,w,h,a<<24);
   }
  }
  // Coherent moving shadow sheets instead of isolated speckle.
  int rows=10,cols=16;
  int dx=(w+cols-1)/cols,dy=(h+rows-1)/rows;
  for(int y=0;y<rows;y++){
   float v=(y+.5f)/rows;
   for(int x=0;x<cols;x++){
    float u=(x+.5f)/cols;
    double folds=Math.sin(u*9.5+v*3.4+t*.57)+
      .65*Math.cos(v*9.8-u*5.3-t*.45);
    double strand=Math.sin(u*18.5+Math.sin(v*8.2+t*.4)*1.5-t*.7);
    int a=Mth.clamp((int)(Math.max(0,folds*.33+strand*.22+.28)*49*p),0,65);
    if(a<2)continue;
    g.fill(x*dx,y*dy,Math.min(w,(x+1)*dx+1),
      Math.min(h,(y+1)*dy+1),a<<24);
   }
  }
 }
 private SleeplessVoidOverlay(){}
}
