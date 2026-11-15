package dev.minemods.pearlpath;
import net.fabricmc.api.ClientModInitializer;

public final class PearlPath implements ClientModInitializer {
    @Override public void onInitializeClient() {
        PearlTrajectory.install();
    }
}
