package dev.minemods.sleepdebt;
import net.neoforged.neoforge.common.ModConfigSpec;
public final class SleepSettings {
 private static final ModConfigSpec.Builder B=new ModConfigSpec.Builder();
 public static final ModConfigSpec.IntValue TICKS_PER_POINT,BASE_RECOVERY;
 public static final ModConfigSpec.BooleanValue KEEP_ON_DEATH,PENALTIES,SHELTER_BONUS,MONSTER_PENALTY,ENABLE_CAMP_REST,MICROSLEEPS,PHANTOM_SYNC;
 public static final ModConfigSpec.DoubleValue SLEEP_PENALTY,BODY_PENALTY,STRESS_PENALTY;
 public static final ModConfigSpec SPEC;
 static {
  B.push("fatigue");
  TICKS_PER_POINT=B.defineInRange("ticksPerPoint",60,20,2400);
  BASE_RECOVERY=B.defineInRange("baseSleepRecovery",550,0,1000);
  KEEP_ON_DEATH=B.comment("Keep accumulated fatigue and debt on death.").define("persistAfterDeath",false);
  PENALTIES=B.define("enableDebuffs",true);
  MICROSLEEPS=B.comment("Brief warning micro-sleeps during extreme exhaustion.").define("microSleeps",true);
  B.pop();
  B.push("penalty_strength");
  SLEEP_PENALTY=B.defineInRange("sleep",1.0,0.0,1.5);
  BODY_PENALTY=B.defineInRange("body",1.0,0.0,1.5);
  STRESS_PENALTY=B.defineInRange("stress",1.0,0.0,1.5);
  B.pop();
  B.push("sleep_quality");
  SHELTER_BONUS=B.define("shelterBonus",true);
  MONSTER_PENALTY=B.define("hostilePenalty",true);
  ENABLE_CAMP_REST=B.define("allowCampRest",true);
  B.pop();
  B.push("vanilla");
  PHANTOM_SYNC=B.comment("Use Sleep Debt to suppress vanilla phantoms for rested players.").define("integratePhantoms",true);
  B.pop();

  SPEC=B.build();
 }
 private SleepSettings(){}
}

