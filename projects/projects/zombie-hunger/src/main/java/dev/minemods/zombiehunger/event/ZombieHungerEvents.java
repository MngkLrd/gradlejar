package dev.minemods.zombiehunger.event;

import dev.minemods.zombiehunger.ai.HungerSignals;
import dev.minemods.zombiehunger.ai.ZombieFoodGoal;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

public final class ZombieHungerEvents {
    // This guard prevents duplicate goals on dimension transfers and chunk reentry.
    private static final Set<Zombie> REGISTERED =
            Collections.newSetFromMap(new WeakHashMap<>());

    private ZombieHungerEvents() {}

    public static void onJoin(EntityJoinLevelEvent event) {
        // Never scan the level in EntityJoinLevelEvent. Chunks may not be ready yet.
        if (event.getLevel().isClientSide()) return;
        if (event.getEntity() instanceof Zombie zombie && REGISTERED.add(zombie)) {
            zombie.goalSelector.addGoal(1, new ZombieFoodGoal(zombie));
        }
    }

    public static void onHurt(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof Zombie zombie
                && !zombie.level().isClientSide()
                && event.getSource().getEntity() instanceof Player player) {
            HungerSignals.attacked(zombie, player);
        }
    }
}
