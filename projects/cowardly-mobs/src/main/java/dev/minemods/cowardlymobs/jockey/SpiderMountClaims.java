package dev.minemods.cowardlymobs.jockey;

import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.Spider;

/**
 * Local reservation shared by spider hunting and skeleton boarding goals.
 * A short TTL prevents stale reservations after unloads, deaths and failures.
 * All use occurs on the logical server thread.
 */
public final class SpiderMountClaims {
    private static final int CLAIM_TICKS = 30;
    private static final Map<Spider, Claim> CLAIMS = new WeakHashMap<>();

    private record Claim(Skeleton owner, int expiresAtSpiderTick) { }

    private SpiderMountClaims() {}

    public static boolean isReserved(Spider spider) {
        return current(spider) != null;
    }

    public static boolean reserve(Spider spider, Skeleton skeleton) {
        Claim existing = current(spider);
        if (existing != null && existing.owner != skeleton) return false;
        CLAIMS.put(spider, new Claim(skeleton, spider.tickCount + CLAIM_TICKS));
        return true;
    }

    public static boolean isOwner(Spider spider, Skeleton skeleton) {
        Claim current = current(spider);
        return current != null && current.owner == skeleton;
    }

    public static void release(Spider spider, Skeleton skeleton) {
        if (isOwner(spider, skeleton)) CLAIMS.remove(spider);
    }

    private static Claim current(Spider spider) {
        Claim c = CLAIMS.get(spider);
        if (c == null) return null;
        if (!spider.isAlive() || !c.owner.isAlive()
                || spider.tickCount > c.expiresAtSpiderTick) {
            CLAIMS.remove(spider);
            return null;
        }
        return c;
    }
}
