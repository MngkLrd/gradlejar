package dev.minemods.cowardlymobs.ai;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;

/**
 * Dynamic courage estimate, evaluated only when a mob considers retreat.
 * 0 = terrified, 1 = bold. Nearby allies do not increase speed or damage.
 */
public final class MoraleSystem {
    private MoraleSystem() {}

    public record Evaluation(double courage, double playerThreat, int allies) {
        public boolean shouldConsiderRetreat() {
            return courage < AiBalance.MORALE_FEAR_THRESHOLD;
        }

        public float retreatChance() {
            // Below the threshold: 25% to 70%. Not every wounded mob flees.
            return (float) Mth.clamp(
                    0.25D + (AiBalance.MORALE_FEAR_THRESHOLD - courage) * 1.30D,
                    0.25D, 0.70D);
        }
    }

    public static Evaluation assess(PathfinderMob mob, Player player) {
        int allies = countNearbyAllies(mob, player, AiBalance.MORALE_ALLY_RADIUS);
        double health = Mth.clamp(mob.getHealth() / Math.max(1.0F, mob.getMaxHealth()), 0.0D, 1.0D);
        double armor = Mth.clamp(player.getArmorValue() / 20.0D, 0.0D, 1.0D);
        double attack = Mth.clamp(
                (player.getAttributeValue(Attributes.ATTACK_DAMAGE) - 1.0D) / 9.0D, 0.0D, 1.0D);
        double threat = 0.42D * armor + 0.40D * attack;

        double support = Math.min(AiBalance.MORALE_MAX_ALLIES, allies)
                * (mob instanceof Zombie
                    ? AiBalance.MORALE_ZOMBIE_ALLY_BONUS
                    : AiBalance.MORALE_SKELETON_ALLY_BONUS);
        double courage = Mth.clamp(0.22D + health * 0.55D + support - threat, 0.0D, 1.0D);
        return new Evaluation(courage, threat, allies);
    }

    public static int countNearbyAllies(PathfinderMob mob, Player target, double radius) {
        if (mob instanceof Zombie) {
            return mob.level().getEntitiesOfClass(Zombie.class,
                    mob.getBoundingBox().inflate(radius),
                    zombie -> zombie != mob && zombie.isAlive()
                            && (zombie.getTarget() == null || zombie.getTarget() == target)).size();
        }
        if (mob instanceof AbstractSkeleton) {
            return mob.level().getEntitiesOfClass(AbstractSkeleton.class,
                    mob.getBoundingBox().inflate(radius),
                    other -> other != mob && other.isAlive()
                            && other.getTarget() == target).size();
        }
        return 0;
    }
}
