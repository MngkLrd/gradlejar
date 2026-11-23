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
import com.mojang.math.Axis;

import java.util.ArrayList;
import java.util.List;

/**
 * Animated, client-only representations of already transferred stacks.
 * No item entities are spawned and no gameplay inventory is ever changed here.
 */
@Mod.EventBusSubscriber(modid = "simplesorter", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ClientFlyingItems {
    private static final int MAX_ACTIVE = 160;
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
            Vec3 baseFrom = Vec3.atCenterOf(flight.from()).add(0, 0.48D, 0);
            Vec3 baseTo = Vec3.atCenterOf(flight.to()).add(0, 0.48D, 0);
            int moved = flight.movedItems();
            // More visible: one successful stack transfer becomes a small
            // train of 1-4 models, not one tiny object. Still client-only.
            int copies = moved >= 24 ? 4 : moved >= 8 ? 3 : moved >= 2 ? 2 : 1;
            int duration = Math.min(30, 17 + (int) Math.ceil(baseFrom.distanceTo(baseTo) / 3D));
            for (int copy = 0; copy < copies; copy++) {
                if (ACTIVE.size() >= MAX_ACTIVE) ACTIVE.remove(0);
                double offsetX = Math.sin((i + 1) * 1.71D + copy * 2.47D) * 0.25D;
                double offsetZ = Math.cos((i + 1) * 1.19D + copy * 2.13D) * 0.25D;
                Vec3 from = baseFrom.add(offsetX, copy * 0.07D, offsetZ);
                Vec3 to = baseTo.add(offsetX * 0.45D, copy * 0.035D, offsetZ * 0.45D);
                ACTIVE.add(new FlightState(from, to, flight.icon().copyWithCount(1),
                        tick + i % 7 + copy * 3, duration,
                        i * 37 + copy * 71));
            }
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
            double arc = Math.min(1.85D, 0.58D + flight.from().distanceTo(flight.to()) * 0.09D);
            pos = pos.add(0D, Math.sin(Math.PI * t) * arc, 0D);

            float fade = (float) (Math.min(1.0D, Math.min(rawT * 6D, (1D - rawT) * 6D)));
            float scale = 0.70F * (0.70F + 0.30F * fade);
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
