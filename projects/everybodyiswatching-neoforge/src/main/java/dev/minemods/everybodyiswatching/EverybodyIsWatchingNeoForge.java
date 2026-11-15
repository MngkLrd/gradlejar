package dev.minemods.everybodyiswatching;

import net.minecraft.world.entity.npc.Villager;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@Mod("everybodyiswatching")
public final class EverybodyIsWatchingNeoForge {
    private final WatchingEngine engine = new WatchingEngine();

    public EverybodyIsWatchingNeoForge() {
        NeoForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onTick(ServerTickEvent.Post event) {
        engine.serverTick(event.getServer());
    }

    @SubscribeEvent
    public void onCommands(RegisterCommandsEvent event) {
        WatchingCommands.register(event.getDispatcher(), engine);
    }

    @SubscribeEvent
    public void onJoin(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof Villager && !event.getLevel().isClientSide()) {
            engine.entityLoaded(event.getEntity());
        }
    }

    @SubscribeEvent
    public void onLeave(EntityLeaveLevelEvent event) {
        if (event.getEntity() instanceof Villager && !event.getLevel().isClientSide()) {
            engine.entityUnloaded(event.getEntity());
        }
    }

    @SubscribeEvent
    public void onStop(ServerStoppedEvent event) {
        engine.shutdown();
    }
}
