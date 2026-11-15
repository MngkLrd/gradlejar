package dev.hungrymining;

import dev.hungrymining.gameplay.HungerMiningSpeed;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;

@Mod(HungryMining.MOD_ID)
public final class HungryMining {
    public static final String MOD_ID = "hungrymining";

    public HungryMining() {
        MinecraftForge.EVENT_BUS.addListener(HungerMiningSpeed::onBreakSpeed);
    }
}
