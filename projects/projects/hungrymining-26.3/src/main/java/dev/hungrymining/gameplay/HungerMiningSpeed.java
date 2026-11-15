package dev.hungrymining.gameplay;

import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * The first two missing food points do not change digging speed.
 * Losing the third food point (food = 17) removes the +8% full-food bonus.
 * Below 17, mining scales linearly down to 30% at food = 0.
 *
 * This is common-side gameplay code, not client rendering code.
 */
public final class HungerMiningSpeed {
    private static final float FED_SPEED = 1.08F;
    private static final float EMPTY_SPEED = 0.30F;
    private static final int THRESHOLD_FOOD = 17;

    private HungerMiningSpeed() {}

    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        Player player = event.getEntity();
        if (player.getAbilities().instabuild || player.isSpectator()) {
            return;
        }
        float factor = multiplierForFood(player.getFoodData().getFoodLevel());
        if (factor != 1.0F) {
            event.setNewSpeed(event.getNewSpeed() * factor);
        }
    }

    public static float multiplierForFood(int foodLevel) {
        int food = Math.max(0, Math.min(20, foodLevel));
        if (food > THRESHOLD_FOOD) {
            return FED_SPEED;
        }
        return EMPTY_SPEED + (1.0F - EMPTY_SPEED) * food / THRESHOLD_FOOD;
    }
}
