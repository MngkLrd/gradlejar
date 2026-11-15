package dev.minemods.potiontimerbar;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;

/** Selects the effect that will expire LAST, regardless of type or source. */
final class EffectTracker {
    private static final Map<Holder<MobEffect>, Timer> TIMERS = new HashMap<>();
    private static ClientLevel lastLevel;
    private static Player lastPlayer;

    static Selection select(Player player, ClientLevel level) {
        // Never allow history to leak into a different world, dimension or player life.
        if (lastLevel != level || lastPlayer != player) {
            TIMERS.clear();
            lastLevel = level;
            lastPlayer = player;
        }

        Set<Holder<MobEffect>> present = new HashSet<>();
        MobEffectInstance latestExpiring = null;
        long longestRemaining = Long.MIN_VALUE;

        for (MobEffectInstance effect : player.getActiveEffects()) {
            if (!effect.isInfiniteDuration() && effect.getDuration() <= 0) {
                continue;
            }

            // Infinite effects beat finite effects and display a full, steady strip.
            long remaining = effect.isInfiniteDuration() ? Long.MAX_VALUE : effect.getDuration();
            if (remaining > longestRemaining) {
                longestRemaining = remaining;
                latestExpiring = effect;
            }

            if (effect.isInfiniteDuration()) {
                continue;
            }

            Holder<MobEffect> id = effect.getEffect();
            present.add(id);
            int ticks = effect.getDuration();
            Timer timer = TIMERS.get(id);

            // A refreshed/reapplied effect must refill its line, not keep the old maximum.
            if (timer == null || ticks > timer.lastDuration + 1 ||
                effect.getAmplifier() != timer.amplifier) {
                TIMERS.put(id, new Timer(ticks, ticks, effect.getAmplifier()));
            } else {
                timer.lastDuration = ticks;
            }
        }

        TIMERS.keySet().retainAll(present);

        if (latestExpiring == null) {
            return null;
        }
        if (latestExpiring.isInfiniteDuration()) {
            return new Selection(latestExpiring, 1.0f);
        }

        Timer timer = TIMERS.get(latestExpiring.getEffect());
        float fraction = Math.max(0.0f, Math.min(1.0f,
            latestExpiring.getDuration() / (float) Math.max(1, timer.initialDuration)));
        return new Selection(latestExpiring, fraction);
    }

    record Selection(MobEffectInstance effect, float fraction) {}

    private static final class Timer {
        final int initialDuration;
        int lastDuration;
        final int amplifier;

        Timer(int initialDuration, int lastDuration, int amplifier) {
            this.initialDuration = initialDuration;
            this.lastDuration = lastDuration;
            this.amplifier = amplifier;
        }
    }

    private EffectTracker() {}
}
