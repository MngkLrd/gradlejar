package com.kirill.simplesorter.client;

import com.kirill.simplesorter.network.FlyingItemsPayload;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.AxisAngle4f;
import com.mojang.math.Axis;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Animated, client-only representations of already transferred stacks.
 * No item entities are spawned and no gameplay inventory is ever changed here.
 */
@Mod.EventBusSubscriber(modid = "simplesorter", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ClientFlyingItems {
    private static final int MAX_ACTIVE = 48;
    private static final List<FlightState> ACTIVE = new ArrayList<>();
    private static net.minecraft.client.multiplayer.ClientLevel lastLevel;

    private ClientFlyingItems() {}

    public static void accept(FlyingItemsPayload payload) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || payload == null) return;
        if (lastLevel != mc.level) {
            ACTIVE.clear();
            lastLevel = mc.level;
        }

        long tick = mc.level.getGameTime();
        int i = 0;
        for (FlyingItemsPayload.Flight flight : payload.flights()) {
            if (flight.icon().isEmpty() || flight.from().equals(flight.to())) continue;
            if (ACTIVE.size() >= MAX_ACTIVE) ACTIVE.remove(0);
            Vec3 from = Vec3.atCenterOf(flight.from()).add(0, 0.55D, 0);
            Vec3 to = Vec3.atCenterOf(flight.to()).add(0, 0.55D, 0);
            int duration = Math.min(15, 8 + (int) Math.ceil(from.distanceTo(to) / 8D));
            ACTIVE.add(new FlightState(from, to, flight.icon().copyWithCount(1),
                    tick + (i % 4), duration, i * 37));
            i++;
        }
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || lastLevel != mc.level) {
            ACTIVE.clear();
            lastLevel = mc.level;
            return;
        }
        long now = mc.level.getGameTime();
        ACTIVE.removeIf(flight -> now > flight.startTick() + flight.duration() + 2);
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || ACTIVE.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.level != lastLevel) return;

        long gameTick = mc.level.getGameTime();
        float partial = event.getPartialTick();
        Vec3 camera = event.getCamera().getPosition();
        PoseStack poses = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();

        boolean any = false;
        for (FlightState flight : ACTIVE) {
            double rawT = (gameTick + partial - flight.startTick()) / flight.duration();
            if (rawT < 0 || rawT > 1) continue;

            double t = rawT * rawT * (3.0D - 2.0D * rawT);
            Vec3 pos = flight.from().lerp(flight.to(), t);
            double arc = Math.min(1.4D, 0.35D + flight.from().distanceTo(flight.to()) * 0.07D);
            pos = pos.add(0D, Math.sin(Math.PI * t) * arc, 0D);

            float fade = (float) (Math.min(1.0D, Math.min(rawT * 6D, (1D - rawT) * 6D)));
            float scale = 0.54F * (0.72F + 0.28F * fade);
            poses.pushPose();
            poses.translate(pos.x - camera.x, pos.y - camera.y, pos.z - camera.z);
            poses.mulPose(Axis.YP.rotationDegrees((float)((gameTick + partial) * 23D + flight.spinOffset())));
            poses.mulPose(Axis.XP.rotationDegrees(15.0F));
            poses.scale(scale, scale, scale);
            mc.getItemRenderer().renderStatic(flight.icon(), ItemDisplayContext.GROUND,
                    LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                    poses, buffers, mc.level, flight.spinOffset());
            poses.popPose();
            any = true;
        }
        if (any) buffers.endBatch();
    }

    private record FlightState(Vec3 from, Vec3 to, ItemStack icon,
                               long startTick, int duration, int spinOffset) {}
}
