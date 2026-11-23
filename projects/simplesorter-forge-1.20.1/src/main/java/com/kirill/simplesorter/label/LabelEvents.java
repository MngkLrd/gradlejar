package com.kirill.simplesorter.label;

import com.kirill.simplesorter.sorting.StorageFilter;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ChunkWatchEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Loads persisted labels as chunks become visible and refunds spent material on chest break. */
@Mod.EventBusSubscriber(modid = "simplesorter", bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class LabelEvents {
    private LabelEvents() {}

    @SubscribeEvent
    public static void watch(ChunkWatchEvent.Watch event) {
        AutoLabelService.sendChunk(event.getPlayer(), event.getLevel(), event.getPos());
    }

    @SubscribeEvent
    public static void unwatch(ChunkWatchEvent.UnWatch event) {
        // Clear labels when a chunk leaves the render distance.
        com.kirill.simplesorter.network.ModNetworking.CHANNEL.send(
                net.minecraftforge.network.PacketDistributor.PLAYER.with(event::getPlayer),
                new com.kirill.simplesorter.network.WarehouseLabelsPayload(
                        event.getPos().x, event.getPos().z, java.util.List.of()));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void breakChest(BlockEvent.BreakEvent event) {
        if (event.isCanceled() || !(event.getLevel() instanceof ServerLevel level)
                || !(event.getPlayer() instanceof ServerPlayer)) return;
        BlockPos pos = event.getPos();
        if (!StorageFilter.isEligible(event.getState())) return;
        WarehouseLabelData.Label label = WarehouseLabelData.get(level).remove(pos);
        if (label == null) return;
        if (!label.material().isEmpty()) {
            Containers.dropItemStack(level, pos.getX() + 0.5D, pos.getY() + 0.5D,
                    pos.getZ() + 0.5D, label.material().copy());
        }
        AutoLabelService.broadcast(level, new ChunkPos(pos));
    }
}
