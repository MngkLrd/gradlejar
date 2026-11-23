package com.kirill.simplesorter.visual;

import com.kirill.simplesorter.config.SorterSettings;
import com.kirill.simplesorter.network.FlyingItemsPayload;
import com.kirill.simplesorter.network.ModNetworking;
import com.kirill.simplesorter.settings.SortingWandSettings;
import com.kirill.simplesorter.sorting.MoveRecord;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class SortingVisuals {
    private static final int MAX_FLIGHTS = FlyingItemsPayload.MAX_FLIGHTS;
    private SortingVisuals() {}

    public static void playSortingEffects(ServerLevel level, Set<BlockPos> changedContainers,
                                          List<MoveRecord> moves, SortingWandSettings settings) {
        SortingWandSettings safe = settings.sanitized();

        if (safe.showContainerHighlights()) {
            int highlighted = 0;
            for (BlockPos pos : changedContainers) {
                // Eight vertices, not hundreds of instant line particles.
                highlightContainer(level, pos);
                if (++highlighted >= SorterSettings.MAX_HIGHLIGHTED_CONTAINERS) break;
            }
        }
        if (safe.showTransferParticles()) {
            sendFlights(level, moves);
        }
    }

    private static void sendFlights(ServerLevel level, List<MoveRecord> moves) {
        Map<String, FlyingItemsPayload.Flight> grouped = new LinkedHashMap<>();
        for (MoveRecord record : moves) {
            if (record.visualStack().isEmpty() || record.sourcePos().equals(record.targetPos())) continue;

            ItemStack icon = record.visualStack().copyWithCount(1);
            String id = BuiltInRegistries.ITEM.getKey(icon.getItem()).toString();
            String key = record.sourcePos().asLong() + ":" + record.targetPos().asLong() + ":" + id;
            FlyingItemsPayload.Flight previous = grouped.get(key);
            if (previous == null) {
                if (grouped.size() >= MAX_FLIGHTS) continue;
                grouped.put(key, new FlyingItemsPayload.Flight(record.sourcePos(), record.targetPos(),
                        icon, record.movedItems()));
            } else {
                grouped.put(key, new FlyingItemsPayload.Flight(previous.from(), previous.to(),
                        previous.icon(), previous.movedItems() + record.movedItems()));
            }
        }
        if (grouped.isEmpty()) return;

        FlyingItemsPayload packet = new FlyingItemsPayload(new ArrayList<>(grouped.values()));
        // Nearby clients see the same motion; clients farther away don't receive
        // an unnecessary packet containing up to 24 serialized item stacks.
        for (ServerPlayer player : level.players()) {
            boolean close = false;
            for (FlyingItemsPayload.Flight flight : packet.flights()) {
                if (player.distanceToSqr(flight.from().getX() + 0.5D,
                        flight.from().getY() + 0.5D, flight.from().getZ() + 0.5D) < 80D * 80D) {
                    close = true;
                    break;
                }
            }
            if (close) {
                ModNetworking.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
            }
        }
    }

    private static void highlightContainer(ServerLevel level, BlockPos pos) {
        for (int dx : new int[]{0, 1}) {
            for (int dy : new int[]{0, 1}) {
                for (int dz : new int[]{0, 1}) {
                    level.sendParticles(ParticleTypes.END_ROD,
                            pos.getX() + (dx == 0 ? 0.14D : 0.86D),
                            pos.getY() + (dy == 0 ? 0.12D : 0.88D),
                            pos.getZ() + (dz == 0 ? 0.14D : 0.86D),
                            1, 0, 0, 0, 0);
                }
            }
        }
    }
}
