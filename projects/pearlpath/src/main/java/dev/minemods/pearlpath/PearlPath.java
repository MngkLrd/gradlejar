package dev.minemods.pearlpath;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = PearlPath.MOD_ID, dist = Dist.CLIENT)
public final class PearlPath {
    public static final String MOD_ID = "pearlpath";

    public PearlPath(IEventBus modBus) {
        NeoForge.EVENT_BUS.addListener(PearlTrajectoryRenderer::render);
    }
}
