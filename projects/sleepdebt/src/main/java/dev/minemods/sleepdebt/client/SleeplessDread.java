package dev.minemods.sleepdebt.client;

import dev.minemods.sleepdebt.entity.SleeplessEntity;
import dev.minemods.sleepdebt.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.material.FogType;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

/**
 * A self-contained proximity horror renderer.
 * No vanilla DARKNESS/BLINDNESS potion effect, no full-screen shader dependency.
 * Dynamic black fog consumes the visible world from its edge inward;
 * the renderer retains a small visible area around the player for agency.
 */
public final class SleeplessDread {
 private static Object world,playerId;
 private static float presence,impact;
 private static int lastScanTick=-100;
 private static float nearest=64f;
 private static int lastHurt;
 private static long nextHeartbeat,nextWhisper,nextBreath,nextRattle;

 public static void tick(ClientTickEvent.Post e){
  Minecraft mc=Minecraft.getInstance();
  if(mc.level!=world||mc.player!=playerId){
   world=mc.level;playerId=mc.player;
   presence=0;impact=0;nearest=64;lastScanTick=-100;lastHurt=0;
   nextHeartbeat=0;nextWhisper=0;nextBreath=0;nextRattle=0;
  }
  if(mc.level==null||mc.player==null)return;
  if(mc.player.tickCount-lastScanTick>=4){
   lastScanTick=mc.player.tickCount;
   nearest=64;
   if(!mc.level.isDay()&&!mc.player.isSpectator()){
    for(SleeplessEntity h:mc.level.getEntitiesOfClass(SleeplessEntity.class,
       mc.player.getBoundingBox().inflate(48))){
     if(h.isAlive())nearest=Math.min(nearest,mc.player.distanceTo(h));
    }
   }
  }
  float proximity=Mth.clamp((38-nearest)/33f,0f,1f);
  // Audio and visual intensity are independent toggles. Alpha 12 incorrectly
  // disabled all monster audio when players disabled the visual vignette.
  float target=(!mc.level.isDay()&&!mc.player.isSleeping())?proximity:0f;
  presence+= (target-presence)*(target>presence?.095f:.18f);
  if(presence<.0001f)presence=0;
  if(lastHurt==0&&mc.player.hurtTime>0&&nearest<8)
   impact=Math.max(impact,.8f);
  lastHurt=mc.player.hurtTime;
  impact*=.90f;
  if(!ClientSettings.SLEEPLESS_AUDIO.get()||presence<.15f||
     mc.player.isSleeping()||mc.isPaused())return;
  long t=mc.level.getGameTime();
  // Local sounds are not sent to other players: each player has their own dread.
  if(presence>.23f&&t>=nextHeartbeat){
   nextHeartbeat=t+(long)(Mth.lerp(presence,65,30));
   mc.level.playLocalSound(mc.player.getX(),mc.player.getY(),mc.player.getZ(),
     ModSounds.HEARTBEAT.get(),SoundSource.AMBIENT,.16f+.48f*presence,
     1.05f-.17f*presence,false);
  }
  if(presence>.52f&&t>=nextBreath){
   nextBreath=t+108+mc.level.random.nextInt(80);
   mc.level.playLocalSound(mc.player.getX(),mc.player.getY(),mc.player.getZ(),
     ModSounds.BREATH.get(),SoundSource.AMBIENT,.15f+.24f*presence,.72f,false);
  }
  if(presence>.73f&&t>=nextWhisper){
   nextWhisper=t+210+mc.level.random.nextInt(320);
   mc.level.playLocalSound(mc.player.getX(),mc.player.getY(),mc.player.getZ(),
     ModSounds.WHISPER.get(),SoundSource.AMBIENT,.11f+.23f*presence,
     .67f+mc.level.random.nextFloat()*.09f,false);
  }
  if(presence>.67f&&t>=nextRattle){
   // Directional paranoia: a rare sound arrives from behind the listener.
   nextRattle=t+170+mc.level.random.nextInt(240);
   var look=mc.player.getLookAngle();
   mc.level.playLocalSound(mc.player.getX()-look.x*3.1,
     mc.player.getY()+.5,mc.player.getZ()-look.z*3.1,
     ModSounds.RATTLE.get(),SoundSource.AMBIENT,.24f+.33f*presence,
     .72f+mc.level.random.nextFloat()*.13f,false);
  }
 }

 public static void renderFog(ViewportEvent.RenderFog e){
  if(!ClientSettings.SLEEPLESS_AURA.get())return;
  float p=presence*ClientSettings.DREAD_STRENGTH.get().floatValue();
  if(p<.012f||e.getType()!=FogType.NONE)return;
  p=Mth.clamp(p,0f,1f);
  float originalFar=e.getFarPlaneDistance();
  float far=Mth.lerp(p*p,originalFar,12f);
  float near=Mth.lerp(p*p,e.getNearPlaneDistance(),1.6f);
  e.setFarPlaneDistance(Math.max(7f,far));
  e.setNearPlaneDistance(Math.min(near,Math.max(0f,far-2f)));
  e.setCanceled(true);
 }
 public static void colorFog(ViewportEvent.ComputeFogColor e){
  if(!ClientSettings.SLEEPLESS_AURA.get())return;
  float p=presence*ClientSettings.DREAD_STRENGTH.get().floatValue();
  if(p<.012f||e.getCamera().getFluidInCamera()!=FogType.NONE)return;
  p=Mth.clamp(p*p*.98f,0f,.98f);
  // Desaturated, almost black indigo. Light sources nearby remain visible.
  e.setRed(Mth.lerp(p,e.getRed(),.013f));
  e.setGreen(Mth.lerp(p,e.getGreen(),.011f));
  e.setBlue(Mth.lerp(p,e.getBlue(),.021f));
 }
 public static void render(GuiGraphics g){
  Minecraft mc=Minecraft.getInstance();
  if(mc.player==null||mc.level==null||mc.screen!=null||
    mc.options.hideGui||!ClientSettings.SLEEPLESS_AURA.get())return;
  float p=Mth.clamp(presence*ClientSettings.DREAD_STRENGTH.get().floatValue(),0f,1f);
  if(p<.025f)return;
  int w=g.guiWidth(),h=g.guiHeight();
  float rhythm=.9f+.1f*(float)Math.sin(mc.player.tickCount*.21f);
  // Multiple nonlinear inward rings, not a flat black image slapped over the HUD.
  for(int layer=9;layer>=1;layer--){
   float norm=layer/9f;
   int x=Math.max(1,(int)(w*.34f*norm*p));
   int y=Math.max(1,(int)(h*.41f*norm*p));
   int alpha=(int)(Mth.clamp(p*p*(1f-norm*.32f)*rhythm,0f,1f)*31);
   int col=(alpha<<24)|0x06060B;
   g.fill(0,0,x,h,col);g.fill(w-x,0,w,h,col);
   g.fill(0,0,w,y,col);g.fill(0,h-y,w,h,col);
  }
  // Heartbeat shock on a strike, with quick decay. The core remains readable.
  float pulse=Mth.clamp(impact,0f,1f);
  if(pulse>.02f){
   int shade=(int)(pulse*95);
   g.fill(0,0,w,h,(shade<<24)|0x080509);
  }
 }
 private SleeplessDread(){}
}
