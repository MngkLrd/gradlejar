package dev.minemods.sleepdebt.registry;

import dev.minemods.sleepdebt.SleepDebtMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Real, spatialized audio cues, never reused vanilla Enderman/Warden effects. */
public final class ModSounds {
 public static final DeferredRegister<SoundEvent> SOUNDS=
   DeferredRegister.create(Registries.SOUND_EVENT,SleepDebtMod.MOD_ID);
 private static DeferredHolder<SoundEvent,SoundEvent> sound(String name){
  return SOUNDS.register(name,()->SoundEvent.createVariableRangeEvent(
   ResourceLocation.fromNamespaceAndPath(SleepDebtMod.MOD_ID,name)));
 }
 public static final DeferredHolder<SoundEvent,SoundEvent> BREATH=sound("sleepless.breath");
 public static final DeferredHolder<SoundEvent,SoundEvent> WHISPER=sound("sleepless.whisper");
 public static final DeferredHolder<SoundEvent,SoundEvent> GROWL=sound("sleepless.growl");
 public static final DeferredHolder<SoundEvent,SoundEvent> STRIKE=sound("sleepless.strike");
 public static final DeferredHolder<SoundEvent,SoundEvent> HEARTBEAT=sound("sleepless.heartbeat");
 private ModSounds(){}
}
