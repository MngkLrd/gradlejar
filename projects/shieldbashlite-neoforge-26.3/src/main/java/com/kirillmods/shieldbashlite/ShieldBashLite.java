package com.kirillmods.shieldbashlite;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent;

/** NeoForge: only a vanilla-successful block can activate the bash. */
@Mod("shieldbashlite")
public final class ShieldBashLite {
    public ShieldBashLite() {
        NeoForge.EVENT_BUS.addListener(ShieldBashLite::onShieldBlock);
    }
    private static void onShieldBlock(LivingShieldBlockEvent event) {
        if (event.getEntity() instanceof ServerPlayer player
                && event.getOriginalBlock() && event.getBlocked()) {
            ShieldBashLogic.onBlockedMelee(player, event.getDamageSource());
        }
    }
}
