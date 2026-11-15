package dev.hungrymining;

import dev.hungrymining.gameplay.HungerMiningSpeed;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Registers the gameplay event without registering any client-only classes.
 */
@Mod(HungryMining.MOD_ID)
public final class HungryMining {
    public static final String MOD_ID = "hungrymining";

    public HungryMining(IEventBus modBus) {
        NeoForge.EVENT_BUS.addListener(HungerMiningSpeed::onBreakSpeed);
    }
}
