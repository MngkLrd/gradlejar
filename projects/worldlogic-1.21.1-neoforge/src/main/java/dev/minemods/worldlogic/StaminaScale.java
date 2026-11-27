package dev.minemods.worldlogic;
/** Vanilla-style 20 half-pips representing remaining stamina, not accumulated fatigue. */
public final class StaminaScale {
 private StaminaScale(){}
 public static int halfUnits(int fatiguePercent){return Math.max(0,Math.min(20,(100-Math.max(0,Math.min(100,fatiguePercent))+2)/5));}
}
