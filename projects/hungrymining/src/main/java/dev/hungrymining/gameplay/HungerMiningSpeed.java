package dev.hungrymining.gameplay;

import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Scales block breaking speed according to the vanilla food meter (0..20).
 *
 * The first two missing food points do not change the full-hunger bonus:
 * the first speed drop occurs once three food points are lost (at 17/20).
 * From 17 down to 0, speed decreases continuously to 30% of normal.
 *
 * The BreakSpeed event fires in both client prediction and server logic.
 * Multiplying getNewSpeed() preserves changes already made by other mods.
 */
public final class HungerMiningSpeed {
    private static final float WELL_FED_MULTIPLIER = 1.08F;
    private static final int SLOWDOWN_START_FOOD = 17;
    private static final float EMPTY_MULTIPLIER = 0.30F;

    private HungerMiningSpeed() {}

    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        Player player = event.getEntity();

        // Survival/Adventure only; never interfere with instant mining.
        if (player.getAbilities().instabuild || player.isSpectator()) {
            return;
        }

        float multiplier = multiplierForFood(player.getFoodData().getFoodLevel());
        if (multiplier != 1.0F) {
            event.setNewSpeed(event.getNewSpeed() * multiplier);
        }
    }

    /**
     * 18..20: 108% digging speed.
     * 17: 100% (first penalty after losing three food points).
     * 0..17: linear gradient from 30% to 100%.
     */
    public static float multiplierForFood(int foodLevel) {
        int food = Math.max(0, Math.min(20, foodLevel));
        if (food > SLOWDOWN_START_FOOD) {
            return WELL_FED_MULTIPLIER;
        }
        return EMPTY_MULTIPLIER
                + (1.0F - EMPTY_MULTIPLIER) * food / SLOWDOWN_START_FOOD;
    }
}
