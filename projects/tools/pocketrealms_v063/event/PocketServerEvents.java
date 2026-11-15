package com.kirill.pocketrealms.event;

import com.kirill.pocketrealms.world.CollapseController;
import com.kirill.pocketrealms.world.RiftThreatDirector;
import com.kirill.pocketrealms.world.CorruptionSpawner;
import com.kirill.pocketrealms.world.LivingLabyrinth;
import com.kirill.pocketrealms.world.PressureErosion;
import com.kirill.pocketrealms.world.PocketDreadDirector;
import net.minecraft.world.entity.monster.Monster;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import com.kirill.pocketrealms.world.PocketRunData;
import com.kirill.pocketrealms.world.ModDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import com.kirill.pocketrealms.block.PocketDoorBlock;
import com.kirill.pocketrealms.world.PocketTeleporter;
import net.minecraft.network.chat.Component;
import com.kirill.pocketrealms.registry.ModBlocks;
import net.minecraft.server.level.ServerPlayer;
import com.kirill.pocketrealms.world.PocketAtmosphere;
import com.kirill.pocketrealms.world.PocketDarknessDirector;
import com.kirill.pocketrealms.world.PocketSoundDirector;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

public final class PocketServerEvents {
    private PocketServerEvents() {}

    // Prevent mobs from turning the maze into an accidental ranged friendly-fire arena.
    @SubscribeEvent
    public static void noMonsterFriendlyFire(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Monster victim)) return;
        if (!(event.getSource().getEntity() instanceof Monster aggressor)) return;
        if(victim.level().dimension().equals(ModDimensions.POCKET_DIMENSION)
                && aggressor.level()==victim.level()) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (event.getServer().getTickCount() % 5 == 0) {
            // Crossing the open 2-high door, not clicking a cube, triggers the raid.
            for (var player : event.getServer().getPlayerList().getPlayers()) {
                if (player.isSpectator() || player.level().dimension().equals(ModDimensions.POCKET_DIMENSION)) continue;
                var floor = player.blockPosition();
                var state = player.serverLevel().getBlockState(floor);
                if (!state.is(ModBlocks.POCKET_DOOR.get())) {
                    floor = floor.above();
                    state = player.serverLevel().getBlockState(floor);
                }
                if (state.is(ModBlocks.POCKET_DOOR.get()) && state.hasProperty(DoorBlock.OPEN)
                        && state.getValue(DoorBlock.OPEN)) {
                    if (state.getValue(DoorBlock.HALF) == DoubleBlockHalf.UPPER) floor = floor.below();
                    PocketTeleporter.startRun(player, floor);
                }
            }
        }
        PocketAtmosphere.tick(event.getServer());
        PocketDarknessDirector.tick(event.getServer());
        CollapseController.tick(event.getServer());
        PressureErosion.tick(event.getServer());
        PocketDreadDirector.tick(event.getServer());
        CorruptionSpawner.tick(event.getServer());
        RiftThreatDirector.tick(event.getServer());
        PocketSoundDirector.tick(event.getServer());
        LivingLabyrinth.tick(event.getServer());
        // Rescue stray players after a death, disconnect, old save migration or run retirement.
        if (event.getServer().getTickCount() % 20 == 0) {
            var level = event.getServer().getLevel(ModDimensions.POCKET_DIMENSION);
            if (level != null) {
                PocketRunData data = PocketRunData.get(event.getServer());
                for (ServerPlayer player : java.util.List.copyOf(level.players())) {
                    // No automatic exit directions or objective hints.
                    // Falling below the maze must kill the player, never rescue them.
                    // The retired/orphan check intentionally ignores Y while a run is active.
                    boolean stillInActiveColumn = data.allRuns().stream().anyMatch(run ->
                            !run.completed && !run.retiring
                            && player.blockPosition().getX() >= run.minX && player.blockPosition().getX() <= run.maxX
                            && player.blockPosition().getZ() >= run.minZ && player.blockPosition().getZ() <= run.maxZ);
                    if (data.findRunAt(player.blockPosition()).isEmpty() && !stillInActiveColumn) {
                        var overworld=event.getServer().overworld();
                        BlockPos spawn=overworld.getSharedSpawnPos();
                        player.teleportTo(overworld, spawn.getX()+.5, spawn.getY()+1, spawn.getZ()+.5,player.getYRot(),player.getXRot());
                    }
                }
            }
        }
    }
}
