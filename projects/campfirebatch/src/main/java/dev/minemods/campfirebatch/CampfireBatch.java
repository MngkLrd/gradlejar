package dev.minemods.campfirebatch;

import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

@Mod("campfirebatch")
public final class CampfireBatch {
    public CampfireBatch() {
        NeoForge.EVENT_BUS.addListener(InteractionHandler::allowSneakingUse);
        NeoForge.EVENT_BUS.addListener(InteractionHandler::loadBatch);
    }
}
