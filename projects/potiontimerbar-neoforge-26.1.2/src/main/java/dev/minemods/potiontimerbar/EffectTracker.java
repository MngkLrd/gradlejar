package dev.minemods.potiontimerbar;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;

/** A separate timer for every active effect, longest remaining first. */
final class EffectTracker {
    private static final Map<Holder<MobEffect>, Timer> TIMERS = new HashMap<>();
    private static ClientLevel lastLevel;
    private static Player lastPlayer;

    static List<Selection> collect(Player player, ClientLevel level) {
        if (lastLevel != level || lastPlayer != player) {
            TIMERS.clear();
            lastLevel = level;
            lastPlayer = player;
        }
        Set<Holder<MobEffect>> present = new HashSet<>();
        List<Selection> result = new ArrayList<>();
        for (MobEffectInstance effect : player.getActiveEffects()) {
            if (!effect.isInfiniteDuration() && effect.getDuration() <= 0) continue;
            var key = effect.getEffect();
            if (effect.isInfiniteDuration()) {
                result.add(new Selection(effect, 1f, Long.MAX_VALUE));
                continue;
            }
            present.add(key);
            int ticks = effect.getDuration();
            Timer timer = TIMERS.get(key);
            if (timer == null || ticks > timer.lastDuration ||
                    effect.getAmplifier() != timer.amplifier) {
                timer = new Timer(ticks, ticks, effect.getAmplifier());
                TIMERS.put(key, timer);
            } else {
                timer.lastDuration = ticks;
            }
            float fraction = Math.max(0f, Math.min(1f, ticks / (float)Math.max(1, timer.initialDuration)));
            result.add(new Selection(effect, fraction, ticks));
        }
        TIMERS.keySet().retainAll(present);
        result.sort(Comparator.comparingLong(Selection::remainingTicks).reversed()
            .thenComparing(s -> s.effect().getEffect().unwrapKey().map(Object::toString).orElse("")));
        return result;
    }

    record Selection(MobEffectInstance effect, float fraction, long remainingTicks) {}
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
