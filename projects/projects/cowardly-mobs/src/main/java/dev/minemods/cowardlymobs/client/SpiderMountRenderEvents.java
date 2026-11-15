package dev.minemods.cowardlymobs.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.minemods.cowardlymobs.CowardlyMobs;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.Spider;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import com.mojang.math.Axis;

/**
 * Optional client-only landing animation. The leap itself happens on the
 * server with real entity velocity; this adds a smooth forward lean/bob as
 * a previously observed walking skeleton becomes a spider passenger.
 *
 * No client-only class is ever referenced by server gameplay classes.
 */
@EventBusSubscriber(modid = CowardlyMobs.MOD_ID, value = Dist.CLIENT)
public final class SpiderMountRenderEvents {
    private static final int ANIMATION_TICKS = 11;
    private static final Map<Skeleton, Tracker> TRACKED = new WeakHashMap<>();

    private static final class Tracker {
        boolean observed;
        boolean riding;
        boolean posePushed;
        int mountedAt = -10000;
    }

    private SpiderMountRenderEvents() {}

    @SubscribeEvent
    public static void beforeRender(RenderLivingEvent.Pre<?, ?> event) {
        if (!(event.getEntity() instanceof Skeleton skeleton)
                || skeleton.getType() != EntityType.SKELETON) return;

        Tracker tracker = TRACKED.computeIfAbsent(skeleton, unused -> new Tracker());
        boolean onSpider = skeleton.getVehicle() instanceof Spider spider
                && spider.getType() == EntityType.SPIDER;

        if (!onSpider) {
            tracker.observed = true;
            tracker.riding = false;
            tracker.mountedAt = -10000;
            return;
        }

        if (!tracker.riding) {
            // Don't animate already-mounted vanilla jockeys on initial load.
            if (tracker.observed) tracker.mountedAt = skeleton.tickCount;
            tracker.riding = true;
            tracker.observed = true;
        }

        float elapsed = skeleton.tickCount + event.getPartialTick() - tracker.mountedAt;
        if (elapsed < 0.0F || elapsed > ANIMATION_TICKS) return;

        float progress = Math.min(1.0F, elapsed / ANIMATION_TICKS);
        float lean = (float) Math.sin(Math.PI * progress) * 17.0F;
        float bob = (float) Math.sin(Math.PI * progress) * 0.10F;
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        pose.translate(0.0D, bob, 0.0D);
        pose.mulPose(Axis.XP.rotationDegrees(lean));
        tracker.posePushed = true;
    }

    @SubscribeEvent
    public static void afterRender(RenderLivingEvent.Post<?, ?> event) {
        if (!(event.getEntity() instanceof Skeleton skeleton)) return;
        Tracker tracker = TRACKED.get(skeleton);
        if (tracker != null && tracker.posePushed) {
            event.getPoseStack().popPose();
            tracker.posePushed = false;
        }
    }
}
