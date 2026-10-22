package dev.minemods.pearlpath;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Client-only Ender Pearl launch prediction. Rendered directly in world space:
 * the line follows the player camera/aim and stops at the earliest block/entity impact.
 */
final class PearlTrajectoryRenderer {
    private static final int MAX_TICKS = 150;
    private static final double INITIAL_SPEED = 1.5;
    private static final double DRAG = 0.99;
    private static final double GRAVITY = 0.03;
    private static final double PEARL_RADIUS = 0.125;

    private PearlTrajectoryRenderer() {}

    static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        Level level = mc.level;
        if (player == null || level == null || mc.options.hideGui) return;
        if (!player.getMainHandItem().is(Items.ENDER_PEARL)
                && !player.getOffhandItem().is(Items.ENDER_PEARL)) return;

        PoseStack stack = event.getPoseStack();
        if (stack == null) return;

        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        Vec3 start = player.getEyePosition(partial).add(0.0, -0.10, 0.0);
        Vec3 view = player.getViewVector(partial).normalize();

        // Vanilla shootFromRotation adds the thrower's horizontal velocity.
        Vec3 movement = player.getDeltaMovement();
        Vec3 velocity = view.scale(INITIAL_SPEED).add(
                movement.x, player.onGround() ? 0.0 : movement.y, movement.z);

        List<Vec3> points = trace(level, player, start, velocity);
        if (points.size() < 2) return;

        Vec3 camera = event.getCamera().getPosition();
        stack.pushPose();
        stack.translate(-camera.x, -camera.y, -camera.z);
        try {
            Matrix4f matrix = stack.last().pose();
            MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
            VertexConsumer lines = buffers.getBuffer(RenderType.lines());

            for (int i = 1; i < points.size(); ++i) {
                float t = i / (float) (points.size() - 1);
                // Cyan near the player, pale amethyst toward the impact.
                int red = (int) (55 + t * 110);
                int green = (int) (229 - t * 88);
                int blue = 245;
                addLine(lines, matrix, points.get(i - 1), points.get(i),
                        red, green, blue, 235);
            }
            // Tiny cross at the predicted first contact point.
            Vec3 end = points.get(points.size() - 1);
            double r = 0.14;
            addLine(lines, matrix, end.add(-r, 0, 0), end.add(r, 0, 0), 198, 116, 255, 255);
            addLine(lines, matrix, end.add(0, -r, 0), end.add(0, r, 0), 198, 116, 255, 255);
            addLine(lines, matrix, end.add(0, 0, -r), end.add(0, 0, r), 198, 116, 255, 255);
            buffers.endBatch(RenderType.lines());
        } finally {
            stack.popPose();
        }
    }

    private static List<Vec3> trace(Level level, LocalPlayer thrower, Vec3 start, Vec3 velocity) {
        List<Vec3> path = new ArrayList<>(MAX_TICKS + 1);
        Vec3 pos = start;
        path.add(pos);

        for (int tick = 0; tick < MAX_TICKS; ++tick) {
            Vec3 next = pos.add(velocity);

            // Actual world collision shapes, not just visual full blocks.
            BlockHitResult block = level.clip(new ClipContext(
                    pos, next, ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE, thrower));
            Vec3 end = block.getType() == HitResult.Type.MISS ? next : block.getLocation();
            double bestDistSq = pos.distanceToSqr(end);
            boolean hit = block.getType() != HitResult.Type.MISS;

            // Include living mobs and other hittable entities in the path.
            AABB sweep = new AABB(pos, next).inflate(PEARL_RADIUS + 0.15);
            for (Entity target : level.getEntities(thrower, sweep,
                    entity -> entity.isPickable() && !entity.isSpectator())) {
                Optional<Vec3> contact = target.getBoundingBox().inflate(PEARL_RADIUS)
                        .clip(pos, next);
                if (contact.isPresent()) {
                    double distanceSq = pos.distanceToSqr(contact.get());
                    if (distanceSq < bestDistSq) {
                        bestDistSq = distanceSq;
                        end = contact.get();
                        hit = true;
                    }
                }
            }

            path.add(end);
            if (hit || next.y < level.getMinBuildHeight() - 16) break;

            pos = next;
            // The pearl has vanilla air drag and gravity.
            velocity = velocity.scale(DRAG).add(0.0, -GRAVITY, 0.0);
        }
        return path;
    }

    private static void addLine(VertexConsumer buffer, Matrix4f matrix,
                                Vec3 a, Vec3 b, int red, int green, int blue, int alpha) {
        Vec3 delta = b.subtract(a);
        float nx = (float) delta.x;
        float ny = (float) delta.y;
        float nz = (float) delta.z;
        float length = (float) delta.length();
        if (length < 0.00001f) return;
        nx /= length;
        ny /= length;
        nz /= length;
        buffer.addVertex(matrix, (float) a.x, (float) a.y, (float) a.z)
                .setColor(red, green, blue, alpha).setNormal(nx, ny, nz);
        buffer.addVertex(matrix, (float) b.x, (float) b.y, (float) b.z)
                .setColor(red, green, blue, alpha).setNormal(nx, ny, nz);
    }
}
