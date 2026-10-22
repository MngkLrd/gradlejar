package dev.minemods.zombiehunger.ai;

import dev.minemods.zombiehunger.config.ZombieHungerConfig;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public final class HungerSignals {
    private static final Map<Zombie, Integer> INTERRUPTED_UNTIL = new WeakHashMap<>();
    private static final Map<Zombie, Integer> LAST_ALERT_TICK = new WeakHashMap<>();
    private static final Map<Zombie, FoodAlert> FOOD_ALERTS = new WeakHashMap<>();

    private record FoodAlert(UUID itemId, int expiresAt) {}

    private HungerSignals() {}

    public static void attacked(Zombie zombie, Player player) {
        // No persistent timestamp: entity tickCount restarts after a reload.
        INTERRUPTED_UNTIL.put(zombie, zombie.tickCount + ZombieHungerConfig.HIT_INTERRUPT_TICKS.get());
    }

    public static boolean interrupted(Zombie zombie) {
        return zombie.tickCount < INTERRUPTED_UNTIL.getOrDefault(zombie, -1);
    }

    public static UUID alertedItem(Zombie zombie) {
        FoodAlert alert = FOOD_ALERTS.get(zombie);
        if (alert == null || alert.expiresAt() < zombie.tickCount) {
            FOOD_ALERTS.remove(zombie);
            return null;
        }
        return alert.itemId();
    }

    public static void alertNeighbors(Zombie source, ItemEntity food) {
        int radius = ZombieHungerConfig.ALERT_RADIUS.get();
        if (radius <= 0 || source.tickCount - LAST_ALERT_TICK.getOrDefault(source, -10000) < 100) {
            return;
        }
        LAST_ALERT_TICK.put(source, source.tickCount);
        Level level = source.level();
        for (Zombie other : level.getEntitiesOfClass(
                Zombie.class, source.getBoundingBox().inflate(radius),
                other -> other != source && other.isAlive())) {
            if (!interrupted(other)) {
                FOOD_ALERTS.put(other, new FoodAlert(food.getUUID(), other.tickCount + 120));
            }
        }
    }
}
