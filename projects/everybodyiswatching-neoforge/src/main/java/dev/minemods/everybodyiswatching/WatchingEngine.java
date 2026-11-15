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
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/** Common 1.21.1 Mojang-mapped logic, called from the server tick on either loader. */
public final class WatchingEngine {
    private static final String LOCK_TAG = "eiw_temporary_ai_lock";
    /** Movement only. Unlike NoAI, this keeps vanilla head/rotation controllers alive. */
    private static final ResourceLocation FREEZE_SPEED_ID =
            ResourceLocation.fromNamespaceAndPath("everybodyiswatching", "watching_freeze");
    private static final AttributeModifier FREEZE_SPEED = new AttributeModifier(
            FREEZE_SPEED_ID, -1.0D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    private static final int MIN_STARE_TICKS = 200; // 10 seconds
    private static final int EXTRA_STARE_TICKS = 81; // up to 14 seconds
    private static final float BODY_TURN_DEGREES_PER_TICK = 9.0F;
    private static final float HEAD_TURN_DEGREES_PER_TICK = 20.0F;
    private static final int RADIUS = 48;
    private static final int MIN_VILLAGERS = 4;
    private static final int ENTRY_DELAY_MIN = 60;
    private static final int ENTRY_DELAY_EXTRA = 101;
    private static final int REENTRY_COOLDOWN = 4 * 60 * 20;
    private static final double ENTRY_CHANCE = 0.65D;
    private final Random random = new Random();
    private final Map<UUID, Visit> visits = new HashMap<>();
    private final Map<ResourceKey<Level>, StareEvent> active = new HashMap<>();
    private final Map<UUID, FrozenVillager> frozen = new HashMap<>();
    private long tick;

    public void serverTick(MinecraftServer server) {
        tick++;
        for (StareEvent event : new ArrayList<>(active.values())) {
            tickEvent(event);
        }
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
        boolean village = level.isVillage(player.blockPosition());
        if (!village) {
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
        if (random.nextDouble() < ENTRY_CHANCE) begin(player, villagers);
    }

    private List<Villager> findVillagers(ServerPlayer player) {
        AABB box = player.getBoundingBox().inflate(RADIUS);
        return player.serverLevel().getEntitiesOfClass(Villager.class, box,
                villager -> villager.isAlive() && !villager.isNoAi()
                        && !frozen.containsKey(villager.getUUID())
                        && villager.distanceToSqr(player) <= RADIUS * RADIUS);
    }

    public int force(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        StareEvent current = active.remove(level.dimension());
        if (current != null) stop(current);
        List<Villager> villagers = findVillagers(player);
        if (villagers.isEmpty()) return 0;
        begin(player, villagers);
        return villagers.size();
    }

    private void begin(ServerPlayer player, List<Villager> villagers) {
        int duration = MIN_STARE_TICKS + random.nextInt(EXTRA_STARE_TICKS); // 10-14 seconds
        boolean lastOne = villagers.size() >= 2 && random.nextInt(4) == 0;
        UUID lingering = lastOne ? villagers.get(random.nextInt(villagers.size())).getUUID() : null;
        StareEvent event = new StareEvent(player.serverLevel(), player.getUUID(),
                player.position(), tick + duration, tick + duration + 60 + random.nextInt(41), lingering);
        for (Villager villager : villagers) {
            if (villager.isNoAi()) continue;
            if (villager.getTradingPlayer() instanceof ServerPlayer trader) trader.closeContainer();
            villager.getNavigation().stop();
            frozen.put(villager.getUUID(), new FrozenVillager(villager));
            villager.addTag(LOCK_TAG);
            AttributeInstance speed = villager.getAttribute(Attributes.MOVEMENT_SPEED);
            if (speed != null) speed.addOrUpdateTransientModifier(FREEZE_SPEED);
            event.members.add(villager.getUUID());
            aimAt(villager, player);
        }
        active.put(player.serverLevel().dimension(), event);
    }

    private void tickEvent(StareEvent event) {
        Entity entity = event.level.getEntity(event.target);
        if (!(entity instanceof ServerPlayer player) || !player.isAlive()
                || player.position().distanceToSqr(event.center) > 80 * 80) {
            active.remove(event.level.dimension());
            stop(event);
            return;
        }
        for (UUID id : new ArrayList<>(event.members)) {
            FrozenVillager lock = frozen.get(id);
            boolean linger = id.equals(event.lingering) && tick < event.lingerUntil;
            if (lock == null || !lock.villager.isAlive() || lock.villager.isRemoved()
                    || (tick >= event.endAt && !linger)) {
                thaw(id);
                event.members.remove(id);
                continue;
            }
            Villager villager = lock.villager;
            // Villagers use Brain AI, not only goal selectors. Kill the pending walk
            // target as well as navigation so a fresh Brain path cannot keep moving.
            villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
            villager.getBrain().eraseMemory(MemoryModuleType.PATH);
            villager.getNavigation().stop();
            Vec3 velocity = villager.getDeltaMovement();
            villager.setDeltaMovement(0, velocity.y, 0);
            aimAt(villager, player);
        }
        if (event.members.isEmpty()) active.remove(event.level.dimension());
    }

    /**
     * Rotate a real, AI-enabled villager in the vanilla synced rotation fields.
     * The head leads; the torso follows when the target is out of neck range.
     * This is invoked after entity ticks, so the Brain cannot overwrite our look.
     */
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

        // Keep vanilla LookControl informed while doing our own precise tracking.
        villager.getLookControl().setLookAt(player, 180.0F, 90.0F);

        float body = approachDegrees(villager.yBodyRot, targetYaw, BODY_TURN_DEGREES_PER_TICK);
        float headTarget = body + Mth.clamp(Mth.wrapDegrees(targetYaw - body), -75.0F, 75.0F);
        float head = approachDegrees(villager.getYHeadRot(), headTarget, HEAD_TURN_DEGREES_PER_TICK);
        float pitch = Mth.clamp(targetPitch, -40.0F, 40.0F);

        // Head and body both must rotate, particularly when the player is behind.
        villager.setYRot(body);
        villager.setYBodyRot(body);
        villager.setYHeadRot(head);
        villager.setXRot(pitch);
    }

    private static float approachDegrees(float current, float target, float maxStep) {
        return current + Mth.clamp(Mth.wrapDegrees(target - current), -maxStep, maxStep);
    }

    private void thaw(UUID id) {
        FrozenVillager lock = frozen.remove(id);
        if (lock == null) return;
        Villager villager = lock.villager;
        AttributeInstance speed = villager.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) speed.removeModifier(FREEZE_SPEED_ID);
        villager.removeTag(LOCK_TAG);
        // Keep the last orientation; vanilla AI will naturally resume looking around.
    }

    private void stop(StareEvent event) {
        for (UUID id : event.members) thaw(id);
        event.members.clear();
    }

    /** Called on chunk/entity load. Repairs a lock left behind by a crash. */
    public void entityLoaded(Entity entity) {
        if (entity instanceof Villager villager && villager.getTags().contains(LOCK_TAG)
                && !frozen.containsKey(villager.getUUID())) {
            // Restore villagers saved by 0.1.0 with NoAI, too.
            villager.setNoAi(false);
            AttributeInstance speed = villager.getAttribute(Attributes.MOVEMENT_SPEED);
            if (speed != null) speed.removeModifier(FREEZE_SPEED_ID);
            villager.removeTag(LOCK_TAG);
        }
    }

    /** Called before an entity unloads so NoAI never persists when a chunk is saved. */
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
                + ", entryChecked=" + (visit != null && visit.attempted)
                + ", pendingSeconds=" + (visit != null && visit.inside && !visit.attempted ? seconds : 0)
                + ", cooldownSeconds=" + cooldown;
    }

    public void shutdown() {
        for (StareEvent event : new ArrayList<>(active.values())) stop(event);
        for (UUID id : new ArrayList<>(frozen.keySet())) thaw(id);
        active.clear();
        visits.clear();
        frozen.clear();
        tick = 0;
    }

    private static final class Visit {
        boolean inside;
        boolean attempted;
        int outsideChecks;
        long scheduledAt = -1;
        long cooldownUntil;
    }

    private static final class FrozenVillager {
        final Villager villager;
        FrozenVillager(Villager villager) {
            this.villager = villager;
        }
    }

    private static final class StareEvent {
        final ServerLevel level;
        final UUID target;
        final Vec3 center;
        final long endAt;
        final long lingerUntil;
        final UUID lingering;
        final Set<UUID> members = new HashSet<>();
        StareEvent(ServerLevel level, UUID target, Vec3 center, long endAt,
                   long lingerUntil, UUID lingering) {
            this.level = level;
            this.target = target;
            this.center = center;
            this.endAt = endAt;
            this.lingerUntil = lingerUntil;
            this.lingering = lingering;
        }
    }
}
