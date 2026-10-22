package dev.wallslam.mechanics;

import dev.wallslam.config.WallslamConfig;
import java.util.Map;
import java.util.WeakHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import import net.minecraft.resources.ResourceLocation;;

/**
 * One active melee wall check per target. Only a near-contact block collision
 * can trigger the slam; no AI changes and no scan across all world entities.
 * Gameplay is authoritative on the logical server.
 */
public final class ImpactTracker {
    public static final ResourceKey<DamageType> COLLISION_DAMAGE = ResourceKey.create(
        Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath("wallslam", "collision"));

    private static final Logger LOGGER = LoggerFactory.getLogger("wallslam");
    private static final int HIT_WINDOW = 11;
    private static final int COOLDOWN_TICKS = 12;
    private static final double CONTACT_REACH = 0.095D;
    private static final double COLLISION_FALLBACK_REACH = 0.25D;

    private static final Map<LivingEntity, Pending> PENDING = new WeakHashMap<>();
    private static final Map<LivingEntity, Long> COOLDOWNS = new WeakHashMap<>();

    private ImpactTracker() {}

    /** Called before vanilla computes melee knockback. */
    public static void prepareMeleeHit(LivingEntity victim, DamageSource source, float damage) {
        if (!(victim instanceof Mob)
            || !(victim.level() instanceof ServerLevel level)
            || !victim.isAlive() || damage <= 0.0F || source.is(COLLISION_DAMAGE)) return;

        if (!(source.getDirectEntity() instanceof LivingEntity attacker)
            || attacker == victim || source.getEntity() != attacker) return;

        long now = level.getGameTime();
        Long previousHit = COOLDOWNS.get(victim);
        if (previousHit != null && now - previousHit < COOLDOWN_TICKS) return;

        double dx = victim.getX() - attacker.getX();
        double dz = victim.getZ() - attacker.getZ();
        double len = Math.hypot(dx, dz);
        if (len < 0.01D) {
            dx = attacker.getLookAngle().x;
            dz = attacker.getLookAngle().z;
            len = Math.hypot(dx, dz);
        }
        if (len < 0.01D) return;

        double attack = Math.max(1.0D, attacker.getAttributeValue(Attributes.ATTACK_DAMAGE));
        float power = (float) Math.min(12.0D, attack * 0.65D + damage * 0.35D);
        float collisionDamage = Math.min(3.3F, Math.max(0.7F, 0.45F + power * 0.23F));
        float bonus = Math.min(0.22F, Math.max(0.0F, (float) ((attack - 1.0D) * 0.024D)));

        PENDING.put(victim, new Pending(dx / len, dz / len,
            now, collisionDamage, bonus, attacker.getId(), false));
        debug("incoming", victim);
    }

    /** Confirm damage once vanilla and other mods have resolved the hit. */
    public static void confirmMeleeHit(LivingEntity victim, DamageSource source, float damage) {
        Pending pending = PENDING.get(victim);
        if (pending == null) return;
        if (source.is(COLLISION_DAMAGE)) return;
        if (!(source.getDirectEntity() instanceof LivingEntity attacker)
            || attacker.getId() != pending.attackerId) return;

        if (damage <= 0.0F) {
            PENDING.remove(victim);
            return;
        }
        PENDING.put(victim, pending.withConfirmed());
        debug("confirmed", victim);
    }

    /** The pending hit exists before this event, unlike the original 0.3.2 code. */
    public static float knockbackBonus(LivingEntity victim) {
        Pending pending = PENDING.get(victim);
        if (pending == null || !(victim.level() instanceof ServerLevel level)
            || level.getGameTime() - pending.time > 1L) return 0.0F;
        return pending.bonus;
    }

    public static void tick(LivingEntity victim) {
        StunManager.tick(victim);
        Pending pending = PENDING.get(victim);
        if (pending == null) return;

        if (!(victim.level() instanceof ServerLevel level) || !victim.isAlive()) {
            PENDING.remove(victim);
            return;
        }
        long age = level.getGameTime() - pending.time;
        if (age < 0L || age > HIT_WINDOW) {
            PENDING.remove(victim);
            debug("expired", victim);
            return;
        }
        if (!pending.confirmed) return;

        AABB box = victim.getBoundingBox();
        AABB contact = box.expandTowards(
            pending.dx * CONTACT_REACH, 0.0D, pending.dz * CONTACT_REACH);
        boolean atWall = level.getBlockCollisions(victim, contact).iterator().hasNext();

        // Some mobs report horizontalCollision just before touching a thin shape
        // (fences / walls). Keep this fallback narrow and direction-sensitive.
        if (!atWall && victim.horizontalCollision) {
            AABB previousMovement = box.expandTowards(
                pending.dx * COLLISION_FALLBACK_REACH, 0.0D,
                pending.dz * COLLISION_FALLBACK_REACH);
            atWall = level.getBlockCollisions(victim, previousMovement).iterator().hasNext();
        }
        if (!atWall) return;

        PENDING.remove(victim);
        COOLDOWNS.put(victim, level.getGameTime());
        debug("impact", victim);

        showImpact(level, victim, pending);
        int stun = WallslamConfig.STUN_TICKS.get();
        if (stun > 0) StunManager.apply(victim, stun);

        // The custom type bypasses only hurt cooldown, NOT entity invulnerability,
        // creative immunity, armor, potion reduction or protection enchantments.
        // Avoid touching invulnerableTime or setHealth.
        float extra = (float) (pending.impactDamage *
            WallslamConfig.DAMAGE_MULTIPLIER.get());
        if (extra > 0.001F && victim.isAlive()) {
            victim.hurt(victim.damageSources().source(COLLISION_DAMAGE), extra);
        }
    }

    public static void onEntityLeave(LivingEntity entity) {
        PENDING.remove(entity);
        COOLDOWNS.remove(entity);
    }

    private static void showImpact(ServerLevel level, LivingEntity mob, Pending p) {
        double x = mob.getX() + p.dx * 0.14D;
        double y = mob.getY() + mob.getBbHeight() * 0.55D;
        double z = mob.getZ() + p.dz * 0.14D;
        level.playSound(null, mob.blockPosition(),
            SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 0.95F, 1.42F);
        level.playSound(null, mob.blockPosition(),
            SoundEvents.STONE_BREAK, SoundSource.HOSTILE, 0.48F, 1.8F);

        double intensity = WallslamConfig.PARTICLE_MULTIPLIER.get();
        if (intensity <= 0.0D) return;
        level.sendParticles(ParticleTypes.EXPLOSION, x, y, z,
            Math.max(1, (int) Math.round(2 * intensity)), 0.07D, 0.16D, 0.07D, 0.01D);
        level.sendParticles(ParticleTypes.CLOUD, x, y, z,
            (int) Math.round(32 * intensity), 0.20D, 0.35D, 0.20D, 0.085D);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, x, y + 0.2D, z,
            (int) Math.round(20 * intensity), 0.27D, 0.22D, 0.27D, 0.09D);
        level.sendParticles(ParticleTypes.CRIT, x, y, z,
            (int) Math.round(20 * intensity), 0.27D, 0.29D, 0.27D, 0.12D);

        BlockPos wallPos = BlockPos.containing(
            mob.getX() + p.dx * (mob.getBbWidth() * 0.5D + 0.20D), y,
            mob.getZ() + p.dz * (mob.getBbWidth() * 0.5D + 0.20D));
        BlockState block = level.getBlockState(wallPos);
        if (!block.isAir()) {
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, block),
                x, y, z, (int) Math.round(13 * intensity),
                0.22D, 0.21D, 0.22D, 0.07D);
        }
    }

    private static void debug(String stage, LivingEntity entity) {
        if (WallslamConfig.DEBUG.get()) {
            LOGGER.info("WallSlam {}: {} at {}", stage, entity.getType(),
                entity.blockPosition());
        }
    }

    private record Pending(double dx, double dz, long time, float impactDamage,
                           float bonus, int attackerId, boolean confirmed) {
        private Pending withConfirmed() {
            return new Pending(dx, dz, time, impactDamage, bonus, attackerId, true);
        }
    }
}
