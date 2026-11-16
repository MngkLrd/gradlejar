package com.kirillmods.shieldbashlite;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.ShieldBlockEvent;
import net.minecraftforge.fml.common.Mod;

/** 1.20.1 Forge/NeoForge share the legacy net.minecraftforge event API. */
@Mod("shieldbashlite")
public final class ShieldBashLite {
    public static final String MOD_ID = "shieldbashlite";

    public ShieldBashLite() {
        MinecraftForge.EVENT_BUS.addListener(ShieldBashLite::onShieldBlock);
    }

    private static void onShieldBlock(ShieldBlockEvent event) {
        if (event.getEntity() instanceof ServerPlayer player
                && event.getBlockedDamage() > 0.0F) {
            ShieldBashLogic.onBlockedMelee(player, event.getDamageSource());
        }
    }
}
