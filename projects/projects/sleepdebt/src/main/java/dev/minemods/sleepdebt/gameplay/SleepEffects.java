package dev.minemods.sleepdebt.gameplay;
import dev.minemods.sleepdebt.SleepSettings;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
/** Consolidates fatigue penalties. Effect duration is short after recovery. */
final class SleepEffects {
 static void apply(ServerPlayer player,CompoundTag tag){
  if(!SleepSettings.PENALTIES.get())return;
  int debt=tag.getInt("debt"),fatigue=tag.getInt("fatigue"),stress=tag.getInt("stress");
  boolean masked=tag.getInt("mask")>0;
  if(!masked&&(debt>=340||fatigue>=750))
   player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN,110,debt>=780?1:0,true,false,true));
  if(!masked&&(debt>=640||fatigue>=900))
   player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,110,0,true,false,true));
  if((!masked&&(debt>=820||stress>=940))||tag.getInt("crash")>0)
   player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,110,0,true,false,true));
  if(tag.getInt("caffeine")>=95)
   player.addEffect(new MobEffectInstance(MobEffects.CONFUSION,110,0,true,false,true));
 }
 private SleepEffects(){}
}