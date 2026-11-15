package dev.minemods.cowardlymobs.ai;

import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;

/**
 * A short-lived reaction signal, recorded when a player actually attacks.
 * The goal evaluates fear after damage has modified health on the next AI tick.
 */
public final class FearSignals {
    private static final Map<PathfinderMob, Signal> ATTACKS = new WeakHashMap<>();
    private record Signal(Player attacker, int tick) {}

    private FearSignals() {}

    public static void onPlayerAttack(PathfinderMob victim, Player player) {
        ATTACKS.put(victim, new Signal(player, victim.tickCount));
    }

    public static boolean pending(PathfinderMob victim, Player target) {
        Signal signal = ATTACKS.get(victim);
        if (signal == null) return false;
        int elapsed = victim.tickCount - signal.tick;
        if (elapsed < 0 || elapsed > 30) {
            ATTACKS.remove(victim);
            return false;
        }
        return signal.attacker == target;
    }

    public static void acknowledge(PathfinderMob victim) {
        ATTACKS.remove(victim);
    }
}
