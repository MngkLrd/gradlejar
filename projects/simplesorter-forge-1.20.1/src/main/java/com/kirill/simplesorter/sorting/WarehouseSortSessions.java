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
 * First, use a server-only transient wand buffer to exchange stacks in
 * completely full/mixed chests. Each batch is extracted and restored in
 * one server tick. Then compact, play visuals and refresh labels once.
 */
@Mod.EventBusSubscriber(modid = "simplesorter",
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class WarehouseSortSessions {
    private static final Map<UUID, Job> ACTIVE = new HashMap<>();
    private static final long SORT_TIME_PER_TICK_NS = 4_000_000L;
    private static final int SORT_SLOTS_PER_TICK = 120;
    private static final int COMPACT_CONTAINERS_PER_TICK = 5;
    private static final long COMPACT_TIME_PER_TICK_NS = 5_000_000L;
    private static final int STATUS_INTERVAL_TICKS = 80;
    private static final int MAX_JOB_TICKS = 2400;

    private WarehouseSortSessions() {}

    public static boolean conflicts(ServerLevel level,List<ContainerRef> refs){
        for(Job other:ACTIVE.values()){
            if(other.level!=level)continue;
            for(ContainerRef a:refs)for(ContainerRef b:other.containers){
                if(a.pos().equals(b.pos())||a.pos().equals(b.partnerPos())||
                        b.pos().equals(a.partnerPos())||
                        (a.partnerPos()!=null&&a.partnerPos().equals(b.partnerPos())))return true;
            }
        }
        return false;
    }

    public static boolean isRunning(UUID player) {
        return ACTIVE.containsKey(player);
    }

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
        // Two overlapping virtual-buffer jobs must not touch the same
        // physical inventory while their layouts are being processed.
        if(conflicts(level,containers)){
            player.displayClientMessage(Component.literal("Simple Sorter: these chests are already being sorted."),true);
            return false;
        }
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
        private WarehouseVacuumSorter.Session session;
        private static final int MAX_ROUNDS=4;
        private int rounds=1,totalMoved,totalRejected,totalTransactions,totalCompleted,compactedAtRoundStart;
        private boolean convergenceLimited;
        private String lastFailure="none";
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
        private int ticks;

        private Job(ServerLevel level, ServerPlayer player, InteractionHand hand,
                    List<ContainerRef> containers, SortingWandSettings settings) {
            this.level = level;
            this.hand = hand;
            this.containers = List.copyOf(containers);
            this.settings = settings;
            this.session = WarehouseVacuumSorter.begin(containers,ref->ContainerScanner.canAccess(level,player,ref));
        }

        private boolean advance(ServerPlayer player) {
            ticks++;
            ItemStack held=player.getItemInHand(hand);
            if(held.isEmpty()||held.getItem()!=ModItems.SORTING_WAND.get()){
                player.displayClientMessage(Component.literal("Simple Sorter: stopped (wand no longer held)."),true);
                return true;
            }
            if (ticks > MAX_JOB_TICKS) {
                player.displayClientMessage(Component.literal(
                        "Simple Sorter: sorting stopped after the safety time limit. "
                        + "Already transferred items remain safe. Start another sort if needed."), false);
                return true;
            }
            if (ticks % STATUS_INTERVAL_TICKS == 0) {
                player.displayClientMessage(Component.literal(
                        "Simple Sorter: " + (compaction
                        ? "packing containers " + compactIndex + "/" + containers.size()
                        : (session.isPlanning()?"planning warehouse ":"applying plan ") + Math.min(session.currentChestIndex() + 1, containers.size())
                                + "/" + containers.size())
                        + "; transferred " + (totalMoved+session.movedItems())
                        + " items between chests. Job still running."), true);
            }
            if (!compaction) {
                if (session.step(level, listener, SORT_SLOTS_PER_TICK, SORT_TIME_PER_TICK_NS)) {
                    if(session.unsafe()){
                        player.displayClientMessage(Component.literal("Simple Sorter: stopped by inventory safety audit. Check latest.log."),false);
                        return true;
                    }
                    compaction = true;
                }
                return false;
            }

            int done = 0;
            long deadline = System.nanoTime() + COMPACT_TIME_PER_TICK_NS;
            while (compactIndex < containers.size()
                    && done < COMPACT_CONTAINERS_PER_TICK
                    && (done == 0 || System.nanoTime() < deadline)) {
                int current = compactIndex++;
                ContainerRef container = containers.get(current);
                if (!container.isValid() || !session.eligibleForCompaction(container)) continue;
                if (InventoryMover.needsCompaction(container)) {
                    var audit=InventoryConservation.count(InventoryConservation.snapshot(container.handler()));
                    compactedItems+=InventoryMover.cleanUpContainer(container,listener);
                    InventoryConservation.requireEqual(audit,
                            InventoryConservation.count(InventoryConservation.snapshot(container.handler())),
                            "compaction at "+container.pos());
                }
                session.refreshContainer(current);
                done++;
            }
            if (compactIndex < containers.size()) return false;

            totalMoved+=session.movedItems();
            totalRejected+=session.rejectedTransfers();
            totalTransactions+=session.totalComponents();
            totalCompleted+=session.completedComponents();
            if(session.rejectedTransfers()>0)lastFailure=session.lastFailure();
            // Compaction can change the stack-count objective. Re-snapshot
            // automatically instead of requiring repeated wand clicks.
            boolean changedThisRound=session.totalComponents()>0 || compactedItems>compactedAtRoundStart;
            if(changedThisRound && totalRejected==0 && rounds<MAX_ROUNDS){
                rounds++;compactedAtRoundStart=compactedItems;
                session=WarehouseVacuumSorter.begin(containers,ref->ContainerScanner.canAccess(level,player,ref));
                compaction=false;compactIndex=0;
                return false;
            }
            convergenceLimited=changedThisRound&&totalRejected==0&&rounds>=MAX_ROUNDS;
            int count = totalMoved + compactedItems;
            if (count > 0) {
                SortingVisuals.playSortingEffects(level, changed, visualMoves, settings);
            }

            ItemStack wand = player.getItemInHand(hand);
            if (!wand.isEmpty() && wand.getItem() == ModItems.SORTING_WAND.get()) {
                AutoLabelService.refresh(level, player, wand, settings, containers);
                if (count > 0) SortingWandItem.consumeUse(wand, player);
            }

            var quality=session.quality();
            int unresolved=quality.mixed();
            int unknown=session.unknownFamilySlots();
            player.displayClientMessage(Component.literal(
                    "Simple Sorter completed: " + totalMoved
                            + " items transferred BETWEEN chests; "
                            + compactedItems + " items compacted WITHIN chests. "
                            + "Coherence: " + quality.score() + "%; mixed: " + unresolved
                            + "/" + quality.occupied() + "; unknown slots: " + unknown + ". "
                            + (convergenceLimited ? "Refinement limit reached. " : "")
                            + (totalRejected>0 ? "Rejected transactions: "+totalRejected+". "+lastFailure+". " : "")
                            + (session.skippedChests()>0 ? "Restricted inventories: "+session.skippedChests()+". " : "")),
                    false);
            player.displayClientMessage(Component.literal("Simple Sorter planning: "+session.eligibilitySummary()),false);
            org.slf4j.LoggerFactory.getLogger("SimpleSorter/SortSessions").info(
                    "Refinement rounds {}, transactions {}/{}, mixed {}/{}, coherence {}, limited {}",
                    rounds,totalCompleted,totalTransactions,unresolved,quality.occupied(),quality.score(),convergenceLimited);
            if (unknown > 0) {
                player.displayClientMessage(Component.literal(
                        "Simple Sorter unknown item IDs: "
                        + session.unknownIdExamples(8)), false);
            }
            return true;
        }
    }
}
