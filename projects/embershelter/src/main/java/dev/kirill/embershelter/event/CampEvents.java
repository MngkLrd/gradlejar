package dev.kirill.embershelter.event;

import dev.kirill.embershelter.camp.CampBuilder;
import dev.kirill.embershelter.camp.CampProtection;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

public final class CampEvents {
    @SubscribeEvent
    public void serverTick(ServerTickEvent.Post event) {
        CampBuilder.tick(event.getServer());
    }

    @SubscribeEvent
    public void onTarget(LivingChangeTargetEvent event) {
        if (!(event.getEntity() instanceof Mob mob) ||
                !(event.getNewAboutToBeSetTarget() instanceof ServerPlayer player) ||
                !(mob.level() instanceof ServerLevel level)) return;
        if (CampProtection.protectedFrom(mob, player, level)) {
            event.setNewAboutToBeSetTarget(null);
        }
    }

    @SubscribeEvent
    public void serverStopped(ServerStoppedEvent event) {
        CampBuilder.clear();
    }
}
