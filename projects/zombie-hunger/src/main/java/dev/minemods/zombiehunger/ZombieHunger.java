package dev.minemods.zombiehunger;

import dev.minemods.zombiehunger.config.ZombieHungerConfig;
import dev.minemods.zombiehunger.event.ZombieHungerEvents;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;

@Mod(ZombieHunger.MOD_ID)
public final class ZombieHunger {
    public static final String MOD_ID = "zombiehunger";

    public ZombieHunger(ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, ZombieHungerConfig.SPEC);
        NeoForge.EVENT_BUS.addListener(ZombieHungerEvents::onJoin);
        NeoForge.EVENT_BUS.addListener(ZombieHungerEvents::onHurt);
    }
}
