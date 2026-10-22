package dev.minemods.sleepdebt.client;
import net.neoforged.neoforge.common.ModConfigSpec;
public final class ClientSettings {
 private static final ModConfigSpec.Builder B=new ModConfigSpec.Builder();
 public static final ModConfigSpec.BooleanValue VISIBLE=B.define("hudVisible",true);
 public static final ModConfigSpec.BooleanValue RESTED=B.define("showWhenRested",false);
 public static final ModConfigSpec.BooleanValue HALLUCINATIONS=B.comment("Very subtle client-only fatigue hallucinations.").define("hallucinations",true);
 public static final ModConfigSpec.BooleanValue MIRAGE_PLAYERS=B.comment("World-space fake Steve/Alex groups. No real player entities.").define("fakePlayers",true);
 public static final ModConfigSpec.IntValue MIRAGE_MAX=B.comment("Hard cap for fake players; performance control.").defineInRange("maxFakePlayers",5,1,8);
 public static final ModConfigSpec.IntValue OFFSET=B.defineInRange("verticalOffset",0,-55,70);
 public static final ModConfigSpec SPEC=B.build();
 private ClientSettings(){}
}


