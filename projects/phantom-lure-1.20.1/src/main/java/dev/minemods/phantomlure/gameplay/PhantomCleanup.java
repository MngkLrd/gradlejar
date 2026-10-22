package dev.minemods.phantomlure.gameplay;

import dev.minemods.phantomlure.blockentity.PhantomLureBlockEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraftforge.event.TickEvent;

/** Remove ONLY lure-created phantoms before they can burn and drop membranes at sunrise. */
public final class PhantomCleanup {
    public static final String LURE_TAG = "phantomlure.summoned";

    public static void onTick(TickEvent.LivingTickEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof Phantom phantom)
            || !(phantom.level() instanceof ServerLevel level)
            || !phantom.getTags().contains(LURE_TAG)) return;

        if (!PhantomLureBlockEntity.isLureNight(level)) {
            phantom.discard();
            return;
        }

        for (ServerPlayer player : level.players()) {
            if (player.isSleeping() && player.distanceToSqr(phantom) < 96.0 * 96.0) {
                phantom.discard();
                return;
            }
        }
    }

    private PhantomCleanup() { }
}
