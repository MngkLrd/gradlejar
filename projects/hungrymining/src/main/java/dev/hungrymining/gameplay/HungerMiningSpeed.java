package dev.hungrymining.gameplay;

import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Scales the current block break speed using the player's food level (0..20).
 *
 * BreakSpeed is fired as digging speed is calculated. Using getNewSpeed() rather
 * than getOriginalSpeed() preserves adjustments from earlier event listeners.
 * This handler runs in common code; both client prediction and server
 * validation should apply the same modifier.
 */
public final class HungerMiningSpeed {
    private HungerMiningSpeed() {}

    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        Player player = event.getEntity();

        // Keep the normal behavior in Creative and Spectator.
        if (player.getAbilities().instabuild || player.isSpectator()) {
            return;
        }

        float multiplier = multiplierForFood(player.getFoodData().getFoodLevel());
        if (multiplier != 1.0F) {
            event.setNewSpeed(event.getNewSpeed() * multiplier);
        }
    }

    public static float multiplierForFood(int foodLevel) {
        if (foodLevel <= 4) {
            return 0.60F; // Starving: 40% slower
        }
        if (foodLevel <= 9) {
            return 0.80F; // Hungry: 20% slower
        }
        if (foodLevel <= 15) {
            return 1.00F; // Neutral
        }
        if (foodLevel <= 19) {
            return 1.05F; // Well-fed: 5% faster
        }
        return 1.08F;     // Full: 8% faster
    }
}
