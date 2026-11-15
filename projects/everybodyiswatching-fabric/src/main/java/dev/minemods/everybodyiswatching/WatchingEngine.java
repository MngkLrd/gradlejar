package dev.minemods.everybodyiswatching;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * Server-authoritative staged horror event:
 * GATHERING (normal AI + forced navigation) -> STARING (immobile + synced head/body tracking).
 * Shared Mojang-mapped 1.21.1 implementation for NeoForge and Fabric.
 */
public final class WatchingEngine {
    private static final String LOCK_TAG = "eiw_temporary_ai_lock";
    private static final ResourceLocation FREEZE_SPEED_ID =
            ResourceLocation.fromNamespaceAndPath("everybodyiswatching", "watching_freeze");
    private static final AttributeModifier FREEZE_SPEED =
            new AttributeModifier(FREEZE_SPEED_ID, -1.0D,
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);

    private static final int RADIUS = 48;
    private static final int MAX_SPECTATORS = 24;
    private static final int MIN_VILLAGERS = 4;
    private static final int GATHER_MIN = 140;       // 7 seconds
    private static final int GATHER_EXTRA = 61;     // up to 10 seconds
    private static final int ARRIVAL_STAGGER = 61;  // stagger departures by 0-3 seconds
    private static final int REPATH_INTERVAL = 20;  // one path request per villager per second
    private static final float WALK_SPEED = 0.88F;  // normal-speed, not a chase
    private static final double ARRIVAL_DISTANCE_SQR = 5.0D;
    private static final double TOO_CLOSE_SQR = 12.25D; // don't crowd inside 3.5 blocks
    private static final int MIN_STARE_TICKS = 200; // 10 seconds
    private static final int EXTRA_STARE_TICKS = 81; // up to 14 seconds
    private static final float BODY_TURN_STEP = 9.0F;
    private static final float HEAD_TURN_STEP = 20.0F;
    private static final int ENTRY_DELAY_MIN = 60;
    private static final int ENTRY_DELAY_EXTRA = 101;
    private static final int REENTRY_COOLDOWN = 4 * 60 * 20;
    private static final double ENTRY_CHANCE = 0.65D;

    private final Random random = new Random();
    private final Map<UUID, Visit> visits = new HashMap<>();
    private final Map<ResourceKey<Level>, StareEvent> active = new HashMap<>();
    private final Map<UUID, Spectator> participants = new HashMap<>();
    private long tick;

    public void serverTick(MinecraftServer server) {
        tick++;
        for (StareEvent event : new ArrayList<>(active.values())) tickEvent(event);
        if (tick % 20 != 0) return;

        Set<UUID> online = new HashSet<>();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            online.add(player.getUUID());
            checkEntry(player);
        }
        visits.keySet().retainAll(online);
    }

    private void checkEntry(ServerPlayer player) {
        Visit visit = visits.computeIfAbsent(player.getUUID(), ignored -> new Visit());
        ServerLevel level = player.serverLevel();
        if (!level.isVillage(player.blockPosition())) {
            if (++visit.outsideChecks >= 6) {
                visit.inside = false;
                visit.attempted = false;
                visit.scheduledAt = -1;
            }
            return;
        }

        visit.outsideChecks = 0;
        if (!visit.inside) {
            visit.inside = true;
            visit.attempted = false;
            visit.scheduledAt = tick + ENTRY_DELAY_MIN + random.nextInt(ENTRY_DELAY_EXTRA);
        }
        if (visit.attempted || tick < visit.scheduledAt) return;
        if (tick < visit.cooldownUntil) {
            visit.attempted = true;
            return;
        }
        if (active.containsKey(level.dimension())) {
            visit.scheduledAt = tick + 80;
            return;
        }
        List<Villager> villagers = findVillagers(player);
        if (villagers.size() < MIN_VILLAGERS) {
            visit.scheduledAt = tick + 80;
            return;
        }
        visit.attempted = true;
        visit.cooldownUntil = tick + REENTRY_COOLDOWN;
        if (random.nextDouble() < ENTRY_CHANCE) begin(player, villagers, false);
    }

    private List<Villager> findVillagers(ServerPlayer player) {
        AABB box = player.getBoundingBox().inflate(RADIUS);
        return player.serverLevel().getEntitiesOfClass(Villager.class, box,
                villager -> villager.isAlive() && !villager.isNoAi()
                        && !participants.containsKey(villager.getUUID())
                        && villager.distanceToSqr(player) <= RADIUS * RADIUS);
    }

    public int force(ServerPlayer player) {
        return force(player, false);
    }

    /** Diagnostic: skip gathering to verify head tracking independently. */
    public int forceStare(ServerPlayer player) {
        return force(player, true);
    }

    private int force(ServerPlayer player, boolean immediateStare) {
        StareEvent current = active.remove(player.serverLevel().dimension());
        if (current != null) stop(current);
        List<Villager> villagers = findVillagers(player);
        if (villagers.isEmpty()) return 0;
        return begin(player, villagers, immediateStare);
    }

    private int begin(ServerPlayer player, List<Villager> villagers, boolean immediateStare) {
        long gatherUntil = tick + GATHER_MIN + random.nextInt(GATHER_EXTRA);
        StareEvent event = new StareEvent(player.serverLevel(), player.getUUID(),
                player.position(), gatherUntil);
        villagers.sort(Comparator.comparingDouble(v -> v.distanceToSqr(player)));
        for (Villager villager : villagers) {
            if (event.members.size() >= MAX_SPECTATORS) break;
            recruit(event, villager);
        }
        if (event.members.isEmpty()) return 0;
        active.put(player.serverLevel().dimension(), event);
        if (immediateStare) enterStare(event, player);
        return event.members.size();
    }

    private void recruit(StareEvent event, Villager villager) {
        if (!villager.isAlive() || villager.isNoAi()
                || participants.containsKey(villager.getUUID())) return;
        if (villager.getTradingPlayer() instanceof ServerPlayer trader) trader.closeContainer();

        // Distributed ring positions prevent every villager from colliding at one point.
        int index = event.members.size();
        double angle = index * 2.399963229728653D + random.nextDouble() * 0.28D;
        double ringRadius = 5.0D + random.nextDouble() * 2.0D;
        long startAt = Math.min(event.gatherUntil - 12,
                tick + random.nextInt(ARRIVAL_STAGGER));
        Spectator spectator = new Spectator(villager, angle, ringRadius, startAt);
        participants.put(villager.getUUID(), spectator);
        event.members.add(villager.getUUID());
        villager.addTag(LOCK_TAG);
    }

    private void tickEvent(StareEvent event) {
        Entity entity = event.level.getEntity(event.target);
        if (!(entity instanceof ServerPlayer player) || !player.isAlive()
                || player.position().distanceToSqr(event.center) > 80 * 80) {
            active.remove(event.level.dimension());
            stop(event);
            return;
        }
        if (event.phase == Phase.GATHERING) {
            if (tick % 20 == 0) gatherMore(event, player);
            if (tick >= event.gatherUntil) {
                enterStare(event, player);
            } else {
                for (UUID id : new ArrayList<>(event.members)) {
                    Spectator spectator = participants.get(id);
                    if (spectator == null || !spectator.villager.isAlive()
                            || spectator.villager.isRemoved()) {
                        release(event, id);
                        continue;
                    }
                    approach(spectator, player);
                }
                if (event.members.isEmpty()) active.remove(event.level.dimension());
                return;
            }
        }
        for (UUID id : new ArrayList<>(event.members)) {
            Spectator spectator = participants.get(id);
            boolean linger = id.equals(event.lingering) && tick < event.lingerUntil;
            if (spectator == null || !spectator.villager.isAlive()
                    || spectator.villager.isRemoved()
                    || (tick >= event.stareUntil && !linger)) {
                release(event, id);
                continue;
            }
            stopWalking(spectator);
            aimAt(spectator.villager, player);
        }
        if (event.members.isEmpty()) active.remove(event.level.dimension());
    }

    /** Find villagers entering the loaded village during the gathering phase. */
    private void gatherMore(StareEvent event, ServerPlayer player) {
        if (event.members.size() >= MAX_SPECTATORS) return;
        List<Villager> newcomers = findVillagers(player);
        newcomers.sort(Comparator.comparingDouble(v -> v.distanceToSqr(player)));
        for (Villager villager : newcomers) {
            if (event.members.size() >= MAX_SPECTATORS) break;
            recruit(event, villager);
        }
    }

    /** Gently override the Brain's walk target using ordinary pathfinding. */
    private void approach(Spectator spectator, ServerPlayer player) {
        Villager villager = spectator.villager;
        if (tick < spectator.startAt) return;

        Vec3 goal = new Vec3(player.getX() + Math.cos(spectator.angle) * spectator.radius,
                player.getY(), player.getZ() + Math.sin(spectator.angle) * spectator.radius);

        if (spectator.arrived || villager.position().distanceToSqr(goal) <= ARRIVAL_DISTANCE_SQR
                || villager.distanceToSqr(player) <= TOO_CLOSE_SQR) {
            spectator.arrived = true;
            stopWalking(spectator);
            return;
        }
        if (tick < spectator.nextPathAt) return;
        spectator.nextPathAt = tick + REPATH_INTERVAL;
        // WALK_TARGET cooperates with villagers' Brain, while navigation.moveTo
        // takes control immediately if a vanilla job behavior changes its goal.
        villager.getBrain().setMemory(MemoryModuleType.WALK_TARGET,
                new WalkTarget(goal, WALK_SPEED, 2));
        villager.getNavigation().moveTo(goal.x, goal.y, goal.z, WALK_SPEED);
    }

    private void enterStare(StareEvent event, ServerPlayer player) {
        event.phase = Phase.STARING;
        event.stareUntil = tick + MIN_STARE_TICKS + random.nextInt(EXTRA_STARE_TICKS);
        if (event.members.size() >= 2 && random.nextInt(4) == 0) {
            int choice = random.nextInt(event.members.size());
            event.lingering = new ArrayList<>(event.members).get(choice);
            event.lingerUntil = event.stareUntil + 60 + random.nextInt(41);
        }
        for (UUID id : new ArrayList<>(event.members)) {
            Spectator spectator = participants.get(id);
            if (spectator == null || !spectator.villager.isAlive() ||
                    spectator.villager.isRemoved()) {
                release(event, id);
                continue;
            }
            stopWalking(spectator);
            aimAt(spectator.villager, player);
        }
    }

    /** Stops position movement without disabling AI, so head and body still rotate. */
    private static void stopWalking(Spectator spectator) {
        Villager villager = spectator.villager;
        villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        villager.getBrain().eraseMemory(MemoryModuleType.PATH);
        villager.getNavigation().stop();
        Vec3 oldSpeed = villager.getDeltaMovement();
        villager.setDeltaMovement(0.0D, oldSpeed.y, 0.0D);
        if (!spectator.immobilized) {
            AttributeInstance speed = villager.getAttribute(Attributes.MOVEMENT_SPEED);
            if (speed != null) speed.addOrUpdateTransientModifier(FREEZE_SPEED);
            spectator.immobilized = true;
        }
    }

    private static void aimAt(Villager villager, ServerPlayer player) {
        Vec3 from = villager.getEyePosition();
        Vec3 to = player.getEyePosition();
        double dx = to.x - from.x;
        double dz = to.z - from.z;
        double dy = to.y - from.y;
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        if (horizontal < 0.001D) return;

        float targetYaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0D);
        float targetPitch = (float) -Math.toDegrees(Math.atan2(dy, horizontal));
        villager.getLookControl().setLookAt(player, 180.0F, 90.0F);
        float body = approachDegrees(villager.yBodyRot, targetYaw, BODY_TURN_STEP);
        float headTarget = body + Mth.clamp(Mth.wrapDegrees(targetYaw - body), -75.0F, 75.0F);
        float head = approachDegrees(villager.getYHeadRot(), headTarget, HEAD_TURN_STEP);
        villager.setYRot(body);
        villager.setYBodyRot(body);
        villager.setYHeadRot(head);
        villager.setXRot(Mth.clamp(targetPitch, -40.0F, 40.0F));
    }

    private static float approachDegrees(float current, float target, float maxStep) {
        return current + Mth.clamp(Mth.wrapDegrees(target - current), -maxStep, maxStep);
    }

    private void release(StareEvent event, UUID id) {
        thaw(id);
        event.members.remove(id);
    }

    private void thaw(UUID id) {
        Spectator spectator = participants.remove(id);
        if (spectator == null) return;
        Villager villager = spectator.villager;
        villager.getNavigation().stop();
        villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        villager.getBrain().eraseMemory(MemoryModuleType.PATH);
        AttributeInstance speed = villager.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) speed.removeModifier(FREEZE_SPEED_ID);
        villager.removeTag(LOCK_TAG);
    }

    private void stop(StareEvent event) {
        for (UUID id : new ArrayList<>(event.members)) release(event, id);
    }

    /** Crash recovery and compatibility with the old 0.1.0 NoAI tag. */
    public void entityLoaded(Entity entity) {
        if (entity instanceof Villager villager && villager.getTags().contains(LOCK_TAG)
                && !participants.containsKey(villager.getUUID())) {
            villager.setNoAi(false);
            villager.getNavigation().stop();
            villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
            villager.getBrain().eraseMemory(MemoryModuleType.PATH);
            AttributeInstance speed = villager.getAttribute(Attributes.MOVEMENT_SPEED);
            if (speed != null) speed.removeModifier(FREEZE_SPEED_ID);
            villager.removeTag(LOCK_TAG);
        }
    }

    public void entityUnloaded(Entity entity) {
        if (entity instanceof Villager villager) thaw(villager.getUUID());
    }

    public void resetPlayer(ServerPlayer player) {
        StareEvent event = active.get(player.serverLevel().dimension());
        if (event != null && event.target.equals(player.getUUID())) {
            active.remove(player.serverLevel().dimension());
            stop(event);
        }
        visits.remove(player.getUUID());
    }

    public String status(ServerPlayer player) {
        Visit visit = visits.get(player.getUUID());
        StareEvent event = active.get(player.serverLevel().dimension());
        boolean activeForPlayer = event != null && event.target.equals(player.getUUID());
        long seconds = visit == null ? 0 : Math.max(0, (visit.scheduledAt - tick + 19) / 20);
        long cooldown = visit == null ? 0 : Math.max(0, (visit.cooldownUntil - tick + 19) / 20);
        return "village=" + player.serverLevel().isVillage(player.blockPosition())
                + ", nearbyVillagers=" + findVillagers(player).size()
                + ", active=" + activeForPlayer
                + ", phase=" + (activeForPlayer ? event.phase : "NONE")
                + ", spectators=" + (activeForPlayer ? event.members.size() : 0)
                + ", phaseSecondsLeft=" + (activeForPlayer
                        ? Math.max(0, ((event.phase == Phase.GATHERING
                                ? event.gatherUntil : event.stareUntil) - tick + 19) / 20) : 0)
                + ", entryChecked=" + (visit != null && visit.attempted)
                + ", pendingSeconds=" + (visit != null && visit.inside && !visit.attempted ? seconds : 0)
                + ", cooldownSeconds=" + cooldown;
    }

    public void shutdown() {
        for (StareEvent event : new ArrayList<>(active.values())) stop(event);
        for (UUID id : new ArrayList<>(participants.keySet())) thaw(id);
        active.clear();
        visits.clear();
        participants.clear();
        tick = 0;
    }

    private enum Phase { GATHERING, STARING }

    private static final class Visit {
        boolean inside;
        boolean attempted;
        int outsideChecks;
        long scheduledAt = -1;
        long cooldownUntil;
    }

    private static final class Spectator {
        final Villager villager;
        final double angle;
        final double radius;
        final long startAt;
        long nextPathAt;
        boolean arrived;
        boolean immobilized;

        Spectator(Villager villager, double angle, double radius, long startAt) {
            this.villager = villager;
            this.angle = angle;
            this.radius = radius;
            this.startAt = startAt;
        }
    }

    private static final class StareEvent {
        final ServerLevel level;
        final UUID target;
        final Vec3 center;
        final long gatherUntil;
        final Set<UUID> members = new HashSet<>();
        Phase phase = Phase.GATHERING;
        long stareUntil;
        long lingerUntil;
        UUID lingering;

        StareEvent(ServerLevel level, UUID target, Vec3 center, long gatherUntil) {
            this.level = level;
            this.target = target;
            this.center = center;
            this.gatherUntil = gatherUntil;
        }
    }
}
