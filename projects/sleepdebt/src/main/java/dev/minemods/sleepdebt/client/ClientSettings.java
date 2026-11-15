package dev.minemods.sleepdebt.client;
import net.neoforged.neoforge.common.ModConfigSpec;
public final class ClientSettings {
 private static final ModConfigSpec.Builder B=new ModConfigSpec.Builder();
 public static final ModConfigSpec.BooleanValue VISIBLE=B.define("hudVisible",true);
 public static final ModConfigSpec.BooleanValue RESTED=B.define("showWhenRested",false);
 public static final ModConfigSpec.BooleanValue HALLUCINATIONS=B.comment("Very subtle client-only fatigue hallucinations.").define("hallucinations",true);
 public static final ModConfigSpec.BooleanValue MIRAGE_PLAYERS=B.comment("World-space fake Steve/Alex groups. No real player entities.").define("fakePlayers",true);
 public static final ModConfigSpec.BooleanValue SLEEPLESS_VISIONS=
  B.comment("Distant client-only Sleepless figures which vanish after a direct gaze.")
   .define("sleeplessApparitions",true);
 public static final ModConfigSpec.IntValue SLEEPLESS_VISION_DEBT=
  B.comment("Sleep debt threshold for the first stalking visions.")
   .defineInRange("apparitionMinDebt",570,500,950);
 public static final ModConfigSpec.BooleanValue SLEEPLESS_AURA=
  B.comment("Legacy alpha 15 aura toggle (kept for config compatibility). Use sleeplessVoidEnabled now.")
   .define("sleeplessNearVignette",true);
 public static final ModConfigSpec.BooleanValue SLEEPLESS_AUDIO=
  B.comment("Enable unique whispers, breath and heartbeat when the real hunter is close.")
   .define("sleeplessHorrorAudio",true);
 public static final ModConfigSpec.DoubleValue DREAD_STRENGTH=
  B.comment("Legacy alpha 15 shader strength (deprecated; new sleeplessVoidStrength is used).")
   .defineInRange("sleeplessFogStrength",1.0,0.0,1.5);
 // Fresh keys intentionally bypass stale Alpha 12-15 nearVignette/fogStrength
 // settings, which could silently leave the new blackness disabled on upgrades.
 public static final ModConfigSpec.BooleanValue VOID_ENABLED=
  B.comment("Independent Sleepless near-black compositor. Not vanilla Darkness/fog.")
   .define("sleeplessVoidEnabled",true);
 public static final ModConfigSpec.BooleanValue VOID_GPU=
  B.comment("Add animated GLSL detail. False forces the reliable CPU compositor.")
   .define("sleeplessVoidGpu",true);
 public static final ModConfigSpec.DoubleValue VOID_STRENGTH=
  B.comment("Alpha 16 blackness strength: 1.0 is severe; 1.35 nearly opaque.")
   .defineInRange("sleeplessVoidStrength",1.0,0.0,1.35);
 public static final ModConfigSpec.BooleanValue VOID_DEBUG=
  B.comment("Show local Sleepless distance and actual blackness intensity on HUD.")
   .define("sleeplessVoidDiagnostics",false);
 public static final ModConfigSpec.IntValue MIRAGE_MAX=B.comment("Hard cap for fake players; performance control.").defineInRange("maxFakePlayers",5,1,8);
 public static final ModConfigSpec.IntValue OFFSET=B.defineInRange("verticalOffset",0,-55,70);
 public static final ModConfigSpec SPEC=B.build();
 private ClientSettings(){}
}


