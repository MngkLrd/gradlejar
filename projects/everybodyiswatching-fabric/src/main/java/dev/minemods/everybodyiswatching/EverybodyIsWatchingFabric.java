package dev.minemods.everybodyiswatching;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.world.entity.npc.Villager;

public final class EverybodyIsWatchingFabric implements ModInitializer {
    private final WatchingEngine engine = new WatchingEngine();

    @Override
    public void onInitialize() {
        ServerTickEvents.END_SERVER_TICK.register(engine::serverTick);
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                WatchingCommands.register(dispatcher, engine));
        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (entity instanceof Villager) engine.entityLoaded(entity);
        });
        ServerEntityEvents.ENTITY_UNLOAD.register((entity, world) -> {
            if (entity instanceof Villager) engine.entityUnloaded(entity);
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> engine.shutdown());
    }
}
