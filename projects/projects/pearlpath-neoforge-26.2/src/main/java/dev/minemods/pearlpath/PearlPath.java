package dev.minemods.pearlpath;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
@Mod(value = "pearlpath", dist = Dist.CLIENT)
public final class PearlPath {
    public PearlPath() { PearlTrajectory.install(); }
}
