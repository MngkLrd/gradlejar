package dev.minemods.campfirebatch;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
public final class CampfireBatch implements ModInitializer {
    @Override public void onInitialize() {
        UseBlockCallback.EVENT.register(BatchLoader::interact);
    }
}
