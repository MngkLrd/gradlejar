package dev.minemods.potiontimerbar;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.client.multiplayer.ClientLevel;

/** The displayed effect is the one with the greatest REMAINING duration. */
final class EffectTracker {
    private static final Map<MobEffect, Timer> TIMERS = new HashMap<>();
    private static ClientLevel lastLevel;
    private static Player lastPlayer;

    static Selection select(Player player, ClientLevel level) {
        if (lastLevel != level || lastPlayer != player) {
            TIMERS.clear();
            lastLevel = level;
            lastPlayer = player;
        }

        Set<MobEffect> present = new HashSet<>();
        MobEffectInstance latest = null;
        long maxRemaining = Long.MIN_VALUE;
        for (MobEffectInstance effect : player.getActiveEffects()) {
            if (!effect.isInfiniteDuration() && effect.getDuration() <= 0) continue;

            long remaining = effect.isInfiniteDuration() ? Long.MAX_VALUE : effect.getDuration();
            if (remaining > maxRemaining) {
                latest = effect;
                maxRemaining = remaining;
            }

            if (effect.isInfiniteDuration()) continue;
            MobEffect key = effect.getEffect();
            present.add(key);
            int ticks = effect.getDuration();
            Timer timer = TIMERS.get(key);
            if (timer == null || ticks > timer.lastDuration + 1 ||
                effect.getAmplifier() != timer.amplifier) {
                TIMERS.put(key, new Timer(ticks, ticks, effect.getAmplifier()));
            } else timer.lastDuration = ticks;
        }
        TIMERS.keySet().retainAll(present);

        if (latest == null) return null;
        if (latest.isInfiniteDuration()) return new Selection(latest, 1.0f);
        Timer current = TIMERS.get(latest.getEffect());
        float fraction = Math.max(0.0f, Math.min(1.0f,
                latest.getDuration() / (float) Math.max(1, current.initialDuration)));
        return new Selection(latest, fraction);
    }

    record Selection(MobEffectInstance effect, float fraction) {}

    private static final class Timer {
        final int initialDuration, amplifier;
        int lastDuration;
        Timer(int initialDuration, int lastDuration, int amplifier) {
            this.initialDuration = initialDuration;
            this.lastDuration = lastDuration;
            this.amplifier = amplifier;
        }
    }

    private EffectTracker() {}
}
