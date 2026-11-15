package dev.minemods.cowardlymobs.hunger.event;

import dev.minemods.cowardlymobs.hunger.ai.HungerSignals;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/** Server-only interruption of eating by player attacks. */
public final class ZombieHungerEvents {
    private ZombieHungerEvents() {}

    public static void onHurt(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof Zombie zombie
                && !zombie.level().isClientSide()
                && event.getSource().getEntity() instanceof Player player) {
            HungerSignals.attacked(zombie, player);
        }
    }
}
