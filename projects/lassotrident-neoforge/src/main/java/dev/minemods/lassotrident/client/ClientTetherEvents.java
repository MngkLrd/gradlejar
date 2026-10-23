package dev.minemods.lassotrident.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.minemods.lassotrident.LassoTrident;
import dev.minemods.lassotrident.TetherNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

/** A vanilla-shaded leash mesh: no emissive debug lines or extra textures. */
@EventBusSubscriber(modid = LassoTrident.MOD_ID, value = Dist.CLIENT)
public final class ClientTetherEvents {
    private static final int SEGMENTS = 24;
    private static final double HALF_WIDTH = 0.016;
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

        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        Vec3 camera = event.getCamera().getPosition();
        Vec3 start = mc.player.getEyePosition(partialTick).add(0, -0.22, 0);
        Vec3 end = anchor.getPosition(partialTick).add(0, 0.03, 0);
        if (start.distanceToSqr(end) < 0.05) return;

        int startBlock = mc.level.getBrightness(LightLayer.BLOCK, BlockPos.containing(start));
        int endBlock = mc.level.getBrightness(LightLayer.BLOCK, BlockPos.containing(end));
        int startSky = mc.level.getBrightness(LightLayer.SKY, BlockPos.containing(start));
        int endSky = mc.level.getBrightness(LightLayer.SKY, BlockPos.containing(end));
        double time = mc.level.getGameTime() + partialTick;
        Vec3 momentum = mc.player.getDeltaMovement();

        PoseStack poses = event.getPoseStack();
        poses.pushPose();
        poses.translate(-camera.x, -camera.y, -camera.z);
        try {
            Matrix4f matrix = poses.last().pose();
            MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
            VertexConsumer vertices = buffers.getBuffer(RenderType.leash());

            Vec3 axis = end.subtract(start).normalize();
            Vec3 sideways = axis.cross(new Vec3(0, 1, 0));
            if (sideways.lengthSqr() < 1.0e-5) sideways = axis.cross(new Vec3(1, 0, 0));
            sideways = sideways.normalize().scale(HALF_WIDTH);
            Vec3 up = axis.cross(sideways).normalize().scale(HALF_WIDTH);
            Vec3[] centres = new Vec3[SEGMENTS + 1];

            double sag = Mth.clamp(start.distanceTo(end) * 0.012, 0.07, 0.28);
            for (int i = 0; i <= SEGMENTS; i++) {
                double t = (double) i / SEGMENTS;
                double envelope = 4.0 * t * (1.0 - t);
                // Small catenary-like sag plus delayed sway following player motion.
                double phase = time * 0.13 + t * 6.5;
                Vec3 sway = sideways.normalize().scale(envelope * 0.045 * Math.sin(phase));
                centres[i] = start.lerp(end, t)
                        .add(momentum.x * envelope * -0.12, -sag * envelope, momentum.z * envelope * -0.12)
                        .add(sway);
            }

            // Two crossed ribbons are visible from every camera angle, even when vertical.
            renderStrip(vertices, matrix, centres, sideways, startBlock, endBlock, startSky, endSky, 1.0F, false);
            renderStrip(vertices, matrix, centres, up, startBlock, endBlock, startSky, endSky, 0.78F, true);
            buffers.endBatch(RenderType.leash());
        } finally {
            poses.popPose();
        }
    }

    private static void renderStrip(VertexConsumer vertices, Matrix4f matrix, Vec3[] centres,
                                    Vec3 width, int blockStart, int blockEnd, int skyStart, int skyEnd,
                                    float tint, boolean reverse) {
        for (int step = 0; step <= SEGMENTS; step++) {
            int i = reverse ? SEGMENTS - step : step;
            float t = (float) i / SEGMENTS;
            int block = Mth.clamp(Math.round(Mth.lerp(t, blockStart, blockEnd)), 0, 15);
            int sky = Mth.clamp(Math.round(Mth.lerp(t, skyStart, skyEnd)), 0, 15);
            int light = LightTexture.pack(block, sky);
            float shade = ((i & 1) == 0 ? 1.0F : 0.83F) * tint;
            int r = (int) (153 * shade);
            int g = (int) (116 * shade);
            int b = (int) (72 * shade);
            Vec3 centre = centres[i];
            Vec3 a = centre.subtract(width);
            Vec3 c = centre.add(width);
            vertices.addVertex(matrix, (float) a.x, (float) a.y, (float) a.z)
                    .setColor(r, g, b, 255).setLight(light);
            vertices.addVertex(matrix, (float) c.x, (float) c.y, (float) c.z)
                    .setColor(r, g, b, 255).setLight(light);
        }
    }
}
