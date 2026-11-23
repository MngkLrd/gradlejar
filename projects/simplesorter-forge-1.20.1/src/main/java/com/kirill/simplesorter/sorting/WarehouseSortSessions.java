package com.kirill.simplesorter.sorting;

import com.kirill.simplesorter.config.SorterSettings;
import com.kirill.simplesorter.item.SortingWandItem;
import com.kirill.simplesorter.label.AutoLabelService;
import com.kirill.simplesorter.registry.ModItems;
import com.kirill.simplesorter.settings.SortingWandSettings;
import com.kirill.simplesorter.visual.SortingVisuals;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * One RMB = one complete warehouse action.
 *
 * Only an explicitly activated wand starts a session. Inventory reads/moves
 * remain server main-thread only; up to 4ms and 120 inspected slots per
 * server tick, continuing over subsequent ticks. No async thread, always-on
 * scans, client-trusted item changes or repeated click requirements.
 *
 * Two bounded phases: stabilize all warehouse owners and relocate misplaced
 * stacks, then merge/compact each chest. Send visuals and refresh labels
 * once on completion.
 */
@Mod.EventBusSubscriber(modid = "simplesorter",
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class WarehouseSortSessions {
    private static final Map<UUID, Job> ACTIVE = new HashMap<>();
    private static final long SORT_TIME_PER_TICK_NS = 4_000_000L;
    private static final int SORT_SLOTS_PER_TICK = 120;
    private static final int COMPACT_CONTAINERS_PER_TICK = 5;
    private static final long COMPACT_TIME_PER_TICK_NS = 5_000_000L;

    private WarehouseSortSessions() {}

    public static boolean start(ServerLevel level, ServerPlayer player,
                                List<ContainerRef> containers, ItemStack wand,
                                SortingWandSettings settings) {
        UUID id = player.getUUID();
        if (ACTIVE.containsKey(id)) {
            player.displayClientMessage(Component.literal(
                    "Simple Sorter: sorting is already in progress."), true);
            return false;
        }
        if (containers.isEmpty()) return false;
        InteractionHand hand = player.getItemInHand(InteractionHand.MAIN_HAND) == wand
                ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        Job job = new Job(level, player, hand, containers, settings);
        ACTIVE.put(id, job);
        player.displayClientMessage(Component.literal(
                "Simple Sorter: organizing " + containers.size()
                        + " containers (radius " + settings.radius()
                        + "). One click is enough."), true);
        return true;
    }

    @SubscribeEvent
    public static void tick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END
                || !(event.level instanceof ServerLevel level)
                || ACTIVE.isEmpty()) return;
        List<UUID> finished = new ArrayList<>();
        for (var entry : ACTIVE.entrySet()) {
            Job job = entry.getValue();
            if (job.level != level) continue;
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(entry.getKey());
            if (player == null || player.serverLevel() != level || !player.isAlive()) {
                finished.add(entry.getKey());
                continue;
            }
            try {
                if (job.advance(player)) finished.add(entry.getKey());
            } catch (RuntimeException err) {
                // The inventory transaction logic is defensive, but a buggy
                // modded capability must not leave an eternal active job.
                org.slf4j.LoggerFactory.getLogger("SimpleSorter/SortSessions")
                        .error("Auto-organize interrupted by a modded inventory", err);
                player.displayClientMessage(Component.literal(
                        "Simple Sorter: stopped because a storage inventory failed. Check latest.log."), true);
                finished.add(entry.getKey());
            }
        }
        for (UUID id : finished) ACTIVE.remove(id);
    }

    @SubscribeEvent
    public static void stopping(ServerStoppingEvent event) {
        ACTIVE.clear();
    }

    private static final class Job {
        private final ServerLevel level;
        private final InteractionHand hand;
        private final List<ContainerRef> containers;
        private final SortingWandSettings settings;
        private final WarehouseReorganizer.Session session;
        private final Set<BlockPos> changed = new HashSet<>();
        private final List<MoveRecord> visualMoves = new ArrayList<>();
        private final MoveListener listener = move -> {
            changed.add(move.sourcePos());
            changed.add(move.targetPos());
            if (visualMoves.size() < SorterSettings.MAX_MOVE_PARTICLE_TRACES
                    && !move.sourcePos().equals(move.targetPos())) {
                visualMoves.add(move);
            }
        };

        private int compactIndex = 0;
        private int compactedItems = 0;
        private boolean compaction = false;

        private Job(ServerLevel level, ServerPlayer player, InteractionHand hand,
                    List<ContainerRef> containers, SortingWandSettings settings) {
            this.level = level;
            this.hand = hand;
            this.containers = List.copyOf(containers);
            this.settings = settings;
            this.session = WarehouseReorganizer.begin(containers);
        }

        private boolean advance(ServerPlayer player) {
            if (!compaction) {
                if (session.step(level, listener, SORT_SLOTS_PER_TICK, SORT_TIME_PER_TICK_NS)) {
                    compaction = true;
                }
                return false;
            }

            int done = 0;
            long deadline = System.nanoTime() + COMPACT_TIME_PER_TICK_NS;
            while (compactIndex < containers.size()
                    && done < COMPACT_CONTAINERS_PER_TICK
                    && (done == 0 || System.nanoTime() < deadline)) {
                ContainerRef container = containers.get(compactIndex++);
                if (!level.hasChunkAt(container.pos())
                        || level.getBlockEntity(container.pos()) != container.identity()) continue;
                if (InventoryMover.needsCompaction(container)) {
                    compactedItems += InventoryMover.cleanUpContainer(container, listener);
                }
                done++;
            }
            if (compactIndex < containers.size()) return false;

            int count = session.movedItems() + compactedItems;
            if (count > 0) {
                SortingVisuals.playSortingEffects(level, changed, visualMoves, settings);
            }

            ItemStack wand = player.getItemInHand(hand);
            if (!wand.isEmpty() && wand.getItem() == ModItems.SORTING_WAND.get()) {
                if (settings.labelMode() != 0) {
                    AutoLabelService.refresh(level, player, wand, settings, containers);
                }
                if (count > 0) SortingWandItem.consumeUse(wand, player);
            }

            player.displayClientMessage(Component.literal(
                    "Simple Sorter: completed. " + session.movedItems()
                            + " items transferred between chests, "
                            + compactedItems + " internally packed. "
                            + (count == 0 ? "Storage is already stable." :
                                "Repeat sorting is not required.")), true);
            return true;
        }
    }
}
