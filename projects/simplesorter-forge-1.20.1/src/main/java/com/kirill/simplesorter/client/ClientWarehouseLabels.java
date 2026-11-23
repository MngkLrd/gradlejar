package com.kirill.simplesorter.client;

import com.kirill.simplesorter.network.WarehouseLabelsPayload;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;

/**
 * Client-only, zero-entity billboards. No copied ItemStacks enter the world:
 * representative models are visual only, so displayed ores cannot duplicate.
 * These signs do not occupy blocks or prevent opening stacked chests.
 */
@Mod.EventBusSubscriber(modid = "simplesorter", value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ClientWarehouseLabels {
    private static final Map<Long, WarehouseLabelsPayload.Entry> LABELS = new HashMap<>();
    private static net.minecraft.client.multiplayer.ClientLevel previousLevel;
    private static final int MAX_VISIBLE = 64;
    private static final double MAX_RENDER_DISTANCE_SQ = 28D * 28D;

    private ClientWarehouseLabels() {}

    public static void accept(WarehouseLabelsPayload payload) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        if (previousLevel != mc.level) {
            LABELS.clear();
            previousLevel = mc.level;
        }
        LABELS.entrySet().removeIf(entry -> {
            BlockPos p = entry.getValue().pos();
            return (p.getX() >> 4) == payload.chunkX() && (p.getZ() >> 4) == payload.chunkZ();
        });
        for (WarehouseLabelsPayload.Entry entry : payload.labels()) {
            LABELS.put(entry.pos().asLong(), entry);
        }
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != previousLevel) {
            LABELS.clear();
            previousLevel = mc.level;
        }
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || LABELS.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.level != previousLevel) return;

        Vec3 camera = event.getCamera().getPosition();
        PoseStack poses = event.getPoseStack();
        MultiBufferSource.BufferSource buffer = mc.renderBuffers().bufferSource();
        int count = 0;

        for (WarehouseLabelsPayload.Entry label : LABELS.values()) {
            if (count >= MAX_VISIBLE) break;
            BlockPos p = label.pos();
            if (!mc.level.hasChunkAt(p)) continue;
            if (mc.level.getBlockEntity(p) == null) continue;
            double dx = p.getX() + 0.5D - camera.x;
            double dy = p.getY() + 1.15D - camera.y;
            double dz = p.getZ() + 0.5D - camera.z;
            if (dx*dx + dy*dy + dz*dz > MAX_RENDER_DISTANCE_SQ) continue;

            poses.pushPose();
            poses.translate(dx, dy, dz);
            poses.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());

            if (label.style() == 2 && !label.icon().isEmpty()) {
                poses.pushPose();
                poses.translate(0D, 0.14D, 0D);
                poses.scale(0.56F, 0.56F, 0.56F);
                mc.getItemRenderer().renderStatic(label.icon(), ItemDisplayContext.FIXED,
                        LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                        poses, buffer, mc.level, (int)(p.asLong() & 0xFFFF));
                poses.popPose();
            }
            poses.pushPose();
            poses.translate(0D, label.style() == 2 ? -0.27D : 0D, 0D);
            float scale = 0.020F;
            poses.scale(-scale, -scale, scale);
            Component name = Component.translatable("label.simplesorter." + label.family());
            int width = mc.font.width(name);
            mc.font.drawInBatch(name, -width / 2.0F, 0F, 0xFFF8E7C6, false,
                    poses.last().pose(), buffer, Font.DisplayMode.NORMAL,
                    0x96311E10, LightTexture.FULL_BRIGHT);
            poses.popPose();
            poses.popPose();
            count++;
        }
        if (count > 0) buffer.endBatch();
    }
}
