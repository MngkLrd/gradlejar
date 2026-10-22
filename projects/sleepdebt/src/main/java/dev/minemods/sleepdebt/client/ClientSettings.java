package dev.minemods.sleepdebt.client;
import net.neoforged.neoforge.common.ModConfigSpec;
public final class ClientSettings {
 private static final ModConfigSpec.Builder B=new ModConfigSpec.Builder();
 public static final ModConfigSpec.BooleanValue VISIBLE=B.define("hudVisible",true);
 public static final ModConfigSpec.BooleanValue RESTED=B.define("showWhenRested",false);
 public static final ModConfigSpec.IntValue OFFSET=B.defineInRange("verticalOffset",0,-55,70);
 public static final ModConfigSpec SPEC=B.build();
 private ClientSettings(){}
}
