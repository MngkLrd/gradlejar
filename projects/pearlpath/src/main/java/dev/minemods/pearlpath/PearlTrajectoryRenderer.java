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
 * Client-only Ender Pearl prediction with a PUBG-like dashed world-space preview.
 * Physical simulation stays at the real thrown-pearl origin, while the FIRST-PERSON
 * preview begins forward of the camera, precisely along the crosshair's ray.
 */
final class PearlTrajectoryRenderer {
    private static final int MAX_TICKS = 150;
    private static final double INITIAL_SPEED = 1.5;
    private static final double DRAG = 0.99;
    private static final double GRAVITY = 0.03;
    private static final double PEARL_RADIUS = 0.125;

    // Do not draw from the camera's near plane! That was the long line from the hotbar.
    private static final double CROSSHAIR_START_DISTANCE = 1.05;
    private static final double DASH_LENGTH = 0.35;
    private static final double DASH_GAP = 0.21;

    private PearlTrajectoryRenderer() {}

    static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        Level level = mc.level;
        if (player == null || level == null || mc.options.hideGui) return;
        if (!player.getMainHandItem().is(Items.ENDER_PEARL)
                && !player.getOffhandItem().is(Items.ENDER_PEARL)) return;

        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        Vec3 camera = event.getCamera().getPosition();
        Vec3 look = player.getViewVector(partialTick).normalize();
        Vec3 physicalStart = player.getEyePosition(partialTick).add(0.0, -0.10, 0.0);

        Vec3 motion = player.getDeltaMovement();
        Vec3 velocity = look.scale(INITIAL_SPEED).add(
                motion.x, player.onGround() ? 0.0 : motion.y, motion.z);

        Trajectory prediction = trace(level, player, physicalStart, velocity);
        if (prediction.points().size() < 2) return;

        // Keep the physical simulation untouched. Only replace the NEAR-CAMERA
        // presentation of the first segment. The in-game trajectory still collides
        // with blocks and entities from the vanilla launch position.
        List<Vec3> visible = new ArrayList<>(prediction.points().size());
        if (mc.options.getCameraType().isFirstPerson()) {
            Vec3 firstTravelPoint = prediction.points().get(1);
            double forward = firstTravelPoint.subtract(camera).dot(look);
            if (forward > CROSSHAIR_START_DISTANCE) {
                // This projects exactly to the crosshair in first person.
                visible.add(camera.add(look.scale(CROSSHAIR_START_DISTANCE)));
            }
        }
        for (int i = 1; i < prediction.points().size(); i++) {
            visible.add(prediction.points().get(i));
        }

        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        pose.translate(-camera.x, -camera.y, -camera.z);
        try {
            Matrix4f matrix = pose.last().pose();
            MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
            VertexConsumer lines = buffers.getBuffer(RenderType.lines());

            drawDashedTrajectory(lines, matrix, visible);

            if (prediction.hitSomething()) {
                Vec3 impact = prediction.points().get(prediction.points().size() - 1);
                double r = 0.12;
                addLine(lines, matrix, impact.add(-r, 0.0, 0.0),
                        impact.add(r, 0.0, 0.0), 190, 115, 245, 255);
                addLine(lines, matrix, impact.add(0.0, -r, 0.0),
                        impact.add(0.0, r, 0.0), 190, 115, 245, 255);
                addLine(lines, matrix, impact.add(0.0, 0.0, -r),
                        impact.add(0.0, 0.0, r), 190, 115, 245, 255);
            }
            buffers.endBatch(RenderType.lines());
        } finally {
            pose.popPose();
        }
    }

    private static void drawDashedTrajectory(VertexConsumer lines, Matrix4f matrix, List<Vec3> points) {
        double totalLength = 0.0;
        for (int i = 1; i < points.size(); i++) {
            totalLength += points.get(i).distanceTo(points.get(i - 1));
        }
        if (totalLength < 1.0e-6) return;

        final double period = DASH_LENGTH + DASH_GAP;
        double travelled = 0.0;
        for (int i = 1; i < points.size(); i++) {
            Vec3 from = points.get(i - 1);
            Vec3 to = points.get(i);
            Vec3 displacement = to.subtract(from);
            double length = displacement.length();
            if (length < 1.0e-6) continue;

            double offset = 0.0;
            while (offset < length - 1.0e-6) {
                double phase = (travelled + offset) % period;
                boolean dash = phase < DASH_LENGTH;
                double step = Math.min(length - offset,
                        dash ? DASH_LENGTH - phase : period - phase);
                if (step < 1.0e-6) {
                    offset += 1.0e-5;
                    continue;
                }
                if (dash) {
                    double progress = (travelled + offset) / totalLength;
                    // Mint cyan fading into a subdued light-purple landing tip.
                    int red = (int) (100.0 + progress * 82.0);
                    int green = (int) (225.0 - progress * 78.0);
                    int blue = 240;
                    Vec3 a = from.add(displacement.scale(offset / length));
                    Vec3 b = from.add(displacement.scale((offset + step) / length));
                    addLine(lines, matrix, a, b, red, green, blue, 205);
                }
                offset += step;
            }
            travelled += length;
        }
    }

    private static Trajectory trace(Level level, LocalPlayer thrower, Vec3 start, Vec3 velocity) {
        List<Vec3> path = new ArrayList<>(MAX_TICKS + 1);
        Vec3 pos = start;
        path.add(pos);

        for (int tick = 0; tick < MAX_TICKS; ++tick) {
            Vec3 next = pos.add(velocity);
            BlockHitResult block = level.clip(new ClipContext(
                    pos, next, ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE, thrower));
            Vec3 end = block.getType() == HitResult.Type.MISS ? next : block.getLocation();
            double bestDistSq = pos.distanceToSqr(end);
            boolean hit = block.getType() != HitResult.Type.MISS;

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
            if (hit) return new Trajectory(path, true);
            if (next.y < level.getMinBuildHeight() - 16) break;

            pos = next;
            velocity = velocity.scale(DRAG).add(0.0, -GRAVITY, 0.0);
        }
        return new Trajectory(path, false);
    }

    private static void addLine(VertexConsumer buffer, Matrix4f matrix,
                                Vec3 a, Vec3 b, int red, int green, int blue, int alpha) {
        Vec3 difference = b.subtract(a);
        float length = (float) difference.length();
        if (length < 1.0e-5f) return;
        float nx = (float) difference.x / length;
        float ny = (float) difference.y / length;
        float nz = (float) difference.z / length;
        buffer.addVertex(matrix, (float) a.x, (float) a.y, (float) a.z)
                .setColor(red, green, blue, alpha).setNormal(nx, ny, nz);
        buffer.addVertex(matrix, (float) b.x, (float) b.y, (float) b.z)
                .setColor(red, green, blue, alpha).setNormal(nx, ny, nz);
    }

    private record Trajectory(List<Vec3> points, boolean hitSomething) {}
}
