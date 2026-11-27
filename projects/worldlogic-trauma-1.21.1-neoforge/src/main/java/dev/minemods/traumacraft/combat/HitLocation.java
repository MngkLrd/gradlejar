package dev.minemods.traumacraft.combat;

import dev.minemods.traumacraft.health.BodyPart;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.Optional;

/** Approximate impact locations inside the vanilla player hitbox. */
public final class HitLocation {
    private HitLocation() {}
    public static BodyPart locate(Player victim, DamageSource source) {
        Entity direct = source.getDirectEntity();
        AABB box = victim.getBoundingBox();
        Vec3 hit;
        if (direct != null && direct != source.getEntity()) {
            hit = direct.position();
        } else if (source.getEntity() != null) {
            Entity attacker = source.getEntity();
            Vec3 start = attacker.getEyePosition();
            Vec3 end = start.add(attacker.getLookAngle().scale(5.0));
            Optional<Vec3> intersection = box.inflate(0.04).clip(start, end);
            hit = intersection.orElseGet(() -> new Vec3(
                    victim.getX(), Math.min(box.maxY, Math.max(box.minY, start.y)), victim.getZ()));
        } else {
            hit = box.getCenter();
        }
        return classify(victim, hit);
    }
    static BodyPart classify(Player victim, Vec3 hit) {
        AABB box = victim.getBoundingBox();
        double y = Math.max(0, Math.min(1, (hit.y - box.minY) / box.getYsize()));
        if (y >= 0.84) return BodyPart.HEAD;
        if (y < 0.43) {
            return lateral(victim, hit) > 0 ? BodyPart.LEFT_LEG : BodyPart.RIGHT_LEG;
        }
        if (y < 0.55) return BodyPart.STOMACH;
        if (y > 0.58 && Math.abs(lateral(victim, hit)) > 0.20) {
            return lateral(victim, hit) > 0 ? BodyPart.LEFT_ARM : BodyPart.RIGHT_ARM;
        }
        return BodyPart.CHEST;
    }
    private static double lateral(Player victim, Vec3 hit) {
        Vec3 f = victim.getLookAngle();
        double length = Math.hypot(f.x, f.z);
        if (length < 1.0e-5) return hit.x - victim.getX();
        return ((hit.x - victim.getX()) * f.z - (hit.z - victim.getZ()) * f.x) / length;
    }
}
