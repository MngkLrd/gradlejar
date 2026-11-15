package dev.minemods.blowpipes;

/**
 * Attached by the Mob mixin; persists through world saves and restores NoAI.
 * This is not vanilla bed sleeping, which mobs other than villagers cannot do.
 */
public interface SleepState {
    boolean blowpipes$startSleep(int ticks);
    void blowpipes$wakeUp();
}
