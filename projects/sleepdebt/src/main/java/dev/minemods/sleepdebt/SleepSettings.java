package dev.minemods.sleepdebt;
import net.neoforged.neoforge.common.ModConfigSpec;
public final class SleepSettings {
 private static final ModConfigSpec.Builder B=new ModConfigSpec.Builder();
 public static final ModConfigSpec.IntValue TICKS_PER_POINT,BASE_RECOVERY,SLEEPLESS_THRESHOLD,RESPAWN_GRACE_TICKS,SPAWN_INTERVAL_TICKS;
 public static final ModConfigSpec.BooleanValue KEEP_ON_DEATH,PENALTIES,SHELTER_BONUS,MONSTER_PENALTY,ENABLE_SLEEPLESS,ENABLE_CAMP_REST,MICROSLEEPS,PHANTOM_SYNC;
 public static final ModConfigSpec SPEC;
 static {
  B.push("fatigue");
  TICKS_PER_POINT=B.defineInRange("ticksPerPoint",60,20,2400);
  BASE_RECOVERY=B.defineInRange("baseSleepRecovery",550,0,1000);
  KEEP_ON_DEATH=B.comment("Keep accumulated debt on death; default resets to prevent repeated spawn-kills.").define("persistAfterDeath",false);
  PENALTIES=B.define("enableDebuffs",true);
  MICROSLEEPS=B.comment("Brief warning micro-sleeps during extreme exhaustion.").define("microSleeps",true);
  B.pop();
  B.push("sleep_quality");
  SHELTER_BONUS=B.define("shelterBonus",true);
  MONSTER_PENALTY=B.define("hostilePenalty",true);
  ENABLE_CAMP_REST=B.define("allowCampRest",true);
  B.pop();
  B.push("vanilla");
  PHANTOM_SYNC=B.comment("Use Sleep Debt to suppress vanilla phantoms for rested players; dreamcatchers ward them off.").define("integratePhantoms",true);
  B.pop();
  B.push("sleepless");
  ENABLE_SLEEPLESS=B.comment("Enable rare hostile encounters for severely sleep-deprived players.").define("enabled",true);
  SLEEPLESS_THRESHOLD=B.defineInRange("minimumSleepDebt",760,500,1000);
  SPAWN_INTERVAL_TICKS=B.comment("How often each player rolls for an encounter at night.")
    .defineInRange("spawnIntervalTicks",160,80,1200);
  RESPAWN_GRACE_TICKS=B.comment("Temporary protection after a death to avoid spawn-killing.")
    .defineInRange("respawnGraceTicks",3600,0,24000);
  B.pop();
  SPEC=B.build();
 }
 private SleepSettings(){}
}

