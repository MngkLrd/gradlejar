package dev.minemods.sleepdebt;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class SleepSettings {
 private static final ModConfigSpec.Builder B = new ModConfigSpec.Builder();
 public static final ModConfigSpec.IntValue TICKS_PER_POINT;
 public static final ModConfigSpec.IntValue BASE_RECOVERY;
 public static final ModConfigSpec.BooleanValue KEEP_ON_DEATH;
 public static final ModConfigSpec.BooleanValue PENALTIES;
 public static final ModConfigSpec.BooleanValue SHELTER_BONUS;
 public static final ModConfigSpec.BooleanValue MONSTER_PENALTY;
 public static final ModConfigSpec SPEC;
 static {
  B.push("fatigue");
  TICKS_PER_POINT = B.comment("Ticks awake per one fatigue point.").defineInRange("ticksPerPoint",60,20,2400);
  BASE_RECOVERY = B.comment("Debt removed by a real sleep.").defineInRange("baseSleepRecovery",550,0,1000);
  KEEP_ON_DEATH = B.define("persistAfterDeath",true);
  PENALTIES = B.define("enableDebuffs",true);
  B.pop();
  B.push("sleep_quality");
  SHELTER_BONUS = B.comment("Sheltered beds heal an additional 15% debt.").define("shelterBonus",true);
  MONSTER_PENALTY = B.comment("Nearby hostiles reduce sleep quality. Vanilla normally blocks sleeping anyway.").define("hostilePenalty",true);
  B.pop();
  SPEC=B.build();
 }
 private SleepSettings(){}
}
