package dev.minemods.cowardlymobs;

import dev.minemods.cowardlymobs.ai.MobAiRegistration;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

@Mod(CowardlyMobs.MOD_ID)
public final class CowardlyMobs {
    public static final String MOD_ID = "cowardlymobs";

    public CowardlyMobs() {
        NeoForge.EVENT_BUS.addListener(MobAiRegistration::onEntityJoin);
    }
}
