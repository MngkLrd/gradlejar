package dev.minemods.potiontimerbar;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;

final class EffectTracker {
    private static final Map<Holder<MobEffect>, Timer> TIMERS = new HashMap<>();
    private static ClientLevel lastLevel;

    static Selection select(Player player, ClientLevel level, boolean prioritizeHarmful) {
        if (lastLevel != level) {
            TIMERS.clear();
            lastLevel = level;
        }
        Set<Holder<MobEffect>> present = new HashSet<>();
        MobEffectInstance best = null;
        int bestRank = Integer.MAX_VALUE;
        int bestDuration = Integer.MAX_VALUE;

        for (MobEffectInstance effect : player.getActiveEffects()) {
            if (effect.isInfiniteDuration() || effect.getDuration() <= 0 || !effect.showIcon()) {
                continue;
            }
            Holder<MobEffect> id = effect.getEffect();
            present.add(id);
            int left = effect.getDuration();
            Timer timer = TIMERS.get(id);
            if (timer == null || left > timer.lastDuration + 2 || effect.getAmplifier() != timer.amplifier) {
                timer = new Timer(left, left, effect.getAmplifier());
                TIMERS.put(id, timer);
            } else {
                timer.lastDuration = left;
            }
            int rank = prioritizeHarmful && id.value().getCategory() != MobEffectCategory.HARMFUL ? 1 : 0;
            if (best == null || rank < bestRank || rank == bestRank && left < bestDuration) {
                best = effect;
                bestRank = rank;
                bestDuration = left;
            }
        }
        TIMERS.keySet().retainAll(present);

        if (best == null) {
            return null;
        }
        Timer timer = TIMERS.get(best.getEffect());
        float remainingFraction = Math.max(0.0f, Math.min(1.0f,
            best.getDuration() / (float) Math.max(1, timer.initialDuration)));
        return new Selection(best, remainingFraction);
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
