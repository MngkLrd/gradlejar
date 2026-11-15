package dev.minemods.sleepdebt.client;

import dev.minemods.sleepdebt.entity.SleeplessEntity;
import dev.minemods.sleepdebt.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.event.ClientTickEvent;

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
  presence+= (target-presence)*(target>presence?.18f:.22f);
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


 /** Server entity proximity is read separately from vanilla fog/lighting.
  *  The GLSL pass uses this intensity to cover ALREADY rendered world pixels. */
 public static float density(){
  if(!ClientSettings.SLEEPLESS_AURA.get())return 0f;
  return Mth.clamp(presence*ClientSettings.DREAD_STRENGTH.get().floatValue(),0f,1f);
 }
 public static float shock(){return Mth.clamp(impact,0f,1f);}
 private SleeplessDread(){}
}
