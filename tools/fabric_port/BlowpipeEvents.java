package dev.minemods.blowpipes;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;

public final class BlowpipeEvents {
    private BlowpipeEvents() {}

    public static void register() {
        // Present on both Fabric API 1.20.1 and 1.21.1.
        // Event fires BEFORE the hit; a sleep dart applies a NEW sleep afterwards.
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((living, source, amount) -> {
            if (living instanceof Mob && living instanceof SleepState state) {
                state.blowpipes$wakeUp();
            }
            return true;
        });
    }

    public static boolean putToSleep(Mob mob, int ticks) {
        if (mob.level().isClientSide() || !mob.isAlive() ||
                mob.getMaxHealth() >= 100.0F ||
                mob.getType() == EntityType.ELDER_GUARDIAN ||
                !(mob instanceof SleepState state)) {
            return false;
        }
        return state.blowpipes$startSleep(ticks);
    }
}
