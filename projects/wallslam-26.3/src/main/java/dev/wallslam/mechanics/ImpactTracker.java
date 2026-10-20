package dev.wallslam.mechanics;

import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;

/**
 * Tracks successful melee hits and detects actual solid obstacles in the
 * push direction. There is no enchantment requirement and no global scan.
 */
public final class ImpactTracker {
    private static final int HIT_WINDOW = 11;
    private static final int COOLDOWN_TICKS = 12;
    private static final double WALL_REACH = 0.50D;

    private static final Map<LivingEntity, Pending> PENDING = new WeakHashMap<>();
    private static final Map<LivingEntity, Long> COOLDOWNS = new WeakHashMap<>();

    private ImpactTracker() {}

    public static void onMeleeDamage(LivingEntity victim, DamageSource source, float damage) {
        if (!(victim instanceof Mob) || !(victim.level() instanceof ServerLevel level)
            || damage <= 0.0F || !victim.isAlive()) return;

        if (!(source.getDirectEntity() instanceof LivingEntity attacker) ||
            source.getEntity() != attacker || attacker == victim) return;

        Long last = COOLDOWNS.get(victim);
        long now = level.getGameTime();
        if (last != null && now - last < COOLDOWN_TICKS) return;

        double dirX = victim.getX() - attacker.getX();
        double dirZ = victim.getZ() - attacker.getZ();
        double distance = Math.hypot(dirX, dirZ);
        if (distance < 0.01D) {
            dirX = attacker.getLookAngle().x;
            dirZ = attacker.getLookAngle().z;
            distance = Math.hypot(dirX, dirZ);
        }
        if (distance < 0.01D) return;

        double attackAttribute = Math.max(1.0D, attacker.getAttributeValue(Attributes.ATTACK_DAMAGE));
        float relativePower = (float) Math.min(12.0D, attackAttribute * 0.65D + damage * 0.35D);
        float impactDamage = Math.min(3.3F, Math.max(0.7F, 0.45F + relativePower * 0.23F));
        float knockbackBonus = Math.min(0.22F, Math.max(0.0F, (float)((attackAttribute - 1.0D) * 0.024D)));

        PENDING.put(victim, new Pending(
            dirX / distance, dirZ / distance, now, impactDamage, knockbackBonus));
    }

    public static float knockbackBonus(LivingEntity victim) {
        Pending pending = PENDING.get(victim);
        if (pending == null || !(victim.level() instanceof ServerLevel level) ||
            level.getGameTime() - pending.time > 1) return 0.0F;
        return pending.bonus;
    }

    public static void tick(LivingEntity victim) {
        // Freeze manager runs even if there is no active pending wall impact.
        StunManager.tick(victim);
        Pending impact = PENDING.get(victim);
        if (impact == null) return;

        if (!(victim.level() instanceof ServerLevel level) || !victim.isAlive()) {
            PENDING.remove(victim);
            return;
        }
        long elapsed = level.getGameTime() - impact.time;
        if (elapsed < 0 || elapsed > HIT_WINDOW) {
            PENDING.remove(victim);
            return;
        }

        // The victim can already be touching a wall on the hit tick, or travel
        // into one over the following movement ticks. A collision shape, not
        // horizontalCollision, verifies that there is a genuine solid obstacle.
        AABB probe = victim.getBoundingBox().move(
            impact.directionX * WALL_REACH, 0.0D, impact.directionZ * WALL_REACH);
        if (!level.getBlockCollisions(victim, probe).iterator().hasNext()) return;

        PENDING.remove(victim);
        COOLDOWNS.put(victim, level.getGameTime());

        // The collision is the trigger. Feedback must not depend on the extra
        // damage being accepted by hurt-invulnerability or another mod.
        showCollision(level, victim, impact);

        if (victim.isAlive()) {
            StunManager.apply(victim, 10);
            // Vanilla gives targets brief hurt immunity after the melee strike.
            // Allow one separately capped impact hit, without recursion:
            // the damage event below cannot re-arm because the source has no
            // direct living attacker, and the impact was already removed.
            victim.setInvulnerableTime(0);
            victim.hurtServer(level, victim.damageSources().flyIntoWall(), impact.impactDamage);
        }
    }

    private static void showCollision(ServerLevel level, LivingEntity victim, Pending impact) {
        double x = victim.getX() + impact.directionX * 0.14D;
        double y = victim.getY() + victim.getBbHeight() * 0.55D;
        double z = victim.getZ() + impact.directionZ * 0.14D;

        // Two distinct, audible vanilla layers plus a highly visible burst.
        level.playSound(null, victim.blockPosition(),
            SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 0.95F, 1.42F);
        level.playSound(null, victim.blockPosition(),
            SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 0.48F, 1.8F);

        level.sendParticles(ParticleTypes.EXPLOSION, x, y, z,
            2, 0.07D, 0.16D, 0.07D, 0.01D);
        level.sendParticles(ParticleTypes.CLOUD, x, y, z,
            32, 0.20D, 0.35D, 0.20D, 0.085D);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, x, y + 0.2D, z,
            20, 0.27D, 0.22D, 0.27D, 0.09D);
        level.sendParticles(ParticleTypes.CRIT, x, y, z,
            20, 0.27D, 0.29D, 0.27D, 0.12D);
    }

    private record Pending(double directionX, double directionZ,
                           long time, float impactDamage, float bonus) {}
}
