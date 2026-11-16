package com.kirillmods.shieldbashlite;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.server.level.ServerPlayer;

/** Fabric: AFTER_DAMAGE reports whether the incoming attack was truly shield-blocked. */
public final class ShieldBashLiteFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        ServerLivingEntityEvents.AFTER_DAMAGE.register(
            (entity, source, original, actual, blocked) -> {
                if (blocked && entity instanceof ServerPlayer player) {
                    ShieldBashLogic.onBlockedMelee(player, source);
                }
            }
        );
    }
}
