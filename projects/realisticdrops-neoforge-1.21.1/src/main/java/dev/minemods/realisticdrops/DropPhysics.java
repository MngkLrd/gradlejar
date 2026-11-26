package dev.minemods.realisticdrops;

import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.Vec3;

/** Server-only sampling; packet-driven player motion is not always in deltaMovement. */
public final class DropPhysics {
    private record Sample(Object level, Vec3 position, Vec3 velocity) {}
    private static final Map<ServerPlayer, Sample> SAMPLES = new WeakHashMap<>();
    private DropPhysics() {}
    public static void sample(ServerPlayer player) {
        Vec3 now = player.position();
        Sample old = SAMPLES.get(player);
        Vec3 motion = old == null || old.level() != player.level()
            ? Vec3.ZERO : now.subtract(old.position());
        if (motion.lengthSqr() > 9.0) motion = Vec3.ZERO;
        SAMPLES.put(player, new Sample(player.level(), now, motion));
    }
    public static void apply(ServerPlayer player, ItemEntity item) {
        if (item == null || !player.isAlive()) return;
        Sample old = SAMPLES.get(player);
        Vec3 motion = player.getDeltaMovement();
        if (old != null && old.level() == player.level()) {
            Vec3 current = player.position().subtract(old.position());
            if (current.lengthSqr() > 9.0) motion = Vec3.ZERO;
            else motion = current.lengthSqr() > 0.000001 ? current : old.velocity();
        }
        if (!Double.isFinite(motion.x) || !Double.isFinite(motion.y) || !Double.isFinite(motion.z)) return;
        double x = motion.x * 0.75, z = motion.z * 0.75;
        double h = Math.hypot(x, z);
        if (h > 0.8) { x *= 0.8 / h; z *= 0.8 / h; }
        double y = player.onGround() ? 0.0 : Math.max(-0.8, Math.min(0.45, motion.y * 0.65));
        Vec3 vanilla = item.getDeltaMovement();
        item.setDeltaMovement(vanilla.add(x, y, z));
    }
}
