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
  B.comment("Dark pixel vignette while the REAL Sleepless approaches.")
   .define("sleeplessNearVignette",true);
 public static final ModConfigSpec.BooleanValue SLEEPLESS_AUDIO=
  B.comment("Enable unique whispers, breath and heartbeat when the real hunter is close.")
   .define("sleeplessHorrorAudio",true);
 public static final ModConfigSpec.DoubleValue DREAD_STRENGTH=
  B.comment("Custom world fog and vignette strength: 0=off, 1=designed, up to 1.5=hardcore.")
   .defineInRange("sleeplessFogStrength",1.0,0.0,1.5);
 public static final ModConfigSpec.IntValue MIRAGE_MAX=B.comment("Hard cap for fake players; performance control.").defineInRange("maxFakePlayers",5,1,8);
 public static final ModConfigSpec.IntValue OFFSET=B.defineInRange("verticalOffset",0,-55,70);
 public static final ModConfigSpec SPEC=B.build();
 private ClientSettings(){}
}


