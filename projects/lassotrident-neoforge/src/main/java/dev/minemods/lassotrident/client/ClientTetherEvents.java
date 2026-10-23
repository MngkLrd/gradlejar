package dev.minemods.lassotrident.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.minemods.lassotrident.LassoTrident;
import dev.minemods.lassotrident.TetherNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

@EventBusSubscriber(modid = LassoTrident.MOD_ID, value = Dist.CLIENT)
public final class ClientTetherEvents {
    private static int anchorEntityId = -1;
    private static boolean lastKeyDown;

    private ClientTetherEvents() {}

    public static void setAnchor(int entityId) {
        anchorEntityId = entityId;
        lastKeyDown = Minecraft.getInstance().options.keyUse.isDown();
    }

    @SubscribeEvent
    public static void clientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            anchorEntityId = -1;
            lastKeyDown = false;
            return;
        }
        if (mc.screen != null) {
            if (anchorEntityId != -1 && lastKeyDown) TetherNetwork.sendControl(false);
            lastKeyDown = false;
            return;
        }
        boolean down = mc.options.keyUse.isDown();
        if (anchorEntityId != -1 && down != lastKeyDown) TetherNetwork.sendControl(down);
        lastKeyDown = down;
    }

    @SubscribeEvent
    public static void renderRope(RenderLevelStageEvent event) {
        if (anchorEntityId < 0 || event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        Entity anchor = mc.level.getEntity(anchorEntityId);
        if (anchor == null) return;

        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        Vec3 camera = event.getCamera().getPosition();
        Vec3 start = mc.player.getEyePosition(partial).add(0.0, -0.25, 0.0);
        Vec3 end = anchor.getPosition(partial);

        PoseStack poses = event.getPoseStack();
        poses.pushPose();
        poses.translate(-camera.x, -camera.y, -camera.z);
        try {
            Matrix4f matrix = poses.last().pose();
            MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
            VertexConsumer line = buffers.getBuffer(RenderType.lines());
            draw(line, matrix, start, end, 145, 109, 62);
            draw(line, matrix, start.add(0.02, 0, 0), end.add(0.02, 0, 0), 206, 176, 115);
            buffers.endBatch(RenderType.lines());
        } finally {
            poses.popPose();
        }
    }

    private static void draw(VertexConsumer buffer, Matrix4f matrix, Vec3 start, Vec3 end,
                             int red, int green, int blue) {
        Vec3 delta = end.subtract(start);
        double len = delta.length();
        if (len < 1.0e-6) return;
        float nx = (float) (delta.x / len);
        float ny = (float) (delta.y / len);
        float nz = (float) (delta.z / len);
        buffer.addVertex(matrix, (float) start.x, (float) start.y, (float) start.z)
                .setColor(red, green, blue, 255).setNormal(nx, ny, nz);
        buffer.addVertex(matrix, (float) end.x, (float) end.y, (float) end.z)
                .setColor(red, green, blue, 255).setNormal(nx, ny, nz);
    }
}
