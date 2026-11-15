package dev.minemods.pearlpath;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

final class PearlTrajectory {
    private static final double DRAG = 0.99;
    private static final double GRAVITY = 0.03;
    private static final double DASH = 0.35;
    private static final double GAP = 0.21;
    private static final double FACE_CLIP = 0.45;

    private PearlTrajectory() {}

    static void install() {
        LevelRenderEvents.COLLECT_SUBMITS.register(PearlTrajectory::submit);
    }

    private static void submit(LevelRenderContext event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) return;
        if (!player.getMainHandItem().is(Items.ENDER_PEARL)
                && !player.getOffhandItem().is(Items.ENDER_PEARL)) return;

        Vec3 camera = mc.gameRenderer.mainCamera().position();
        Vec3 look = player.getViewVector(1.0f).normalize();
        Vec3 initial = player.getEyePosition().add(0.0, -0.10, 0.0);
        Vec3 playerVelocity = player.getDeltaMovement();
        Vec3 projectileVelocity = look.scale(1.5).add(
                playerVelocity.x, player.onGround() ? 0.0 : playerVelocity.y, playerVelocity.z);

        Flight flight = predict(mc.level, player, initial, projectileVelocity);
        if (flight.points.size() < 2) return;
        ArrayList<Vec3> visible = new ArrayList<>(flight.points);

        // Keep the real origin by the player's face. Clip only the near-camera
        // portion to prevent lines from appearing to grow out of the hotbar.
        if (mc.options.getCameraType().isFirstPerson()) {
            Vec3 a = visible.get(0), b = visible.get(1);
            Vec3 d = b.subtract(a);
            double length = d.length();
            if (length > 0.0001) visible.set(0,
                    a.add(d.scale(Math.min(FACE_CLIP, length * 0.9) / length)));
        }
        List<Segment> strokes = segments(visible, flight.hit);
        PoseStack stack = event.poseStack();
        stack.pushPose();
        stack.translate(-camera.x, -camera.y, -camera.z);
        event.submitNodeCollector().submitCustomGeometry(
                stack, RenderTypes.lines(),
                (pose, vertex) -> strokes.forEach(s ->
                        line(vertex, pose, s.a, s.b, s.r, s.g, s.bColor)));
        stack.popPose();
    }

    private static Flight predict(Level level, LocalPlayer player, Vec3 start, Vec3 velocity) {
        List<Vec3> points = new ArrayList<>(150);
        points.add(start);
        Vec3 pos = start;
        for (int tick = 0; tick < 150; tick++) {
            Vec3 next = pos.add(velocity);
            BlockHitResult block = level.clip(new ClipContext(
                    pos, next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
            boolean collision = block.getType() != HitResult.Type.MISS;
            Vec3 endpoint = collision ? block.getLocation() : next;
            double nearest = pos.distanceToSqr(endpoint);
            AABB sweep = new AABB(pos, next).inflate(0.275);
            for (Entity target : level.getEntities(player, sweep,
                    e -> e.isPickable() && !e.isSpectator())) {
                Optional<Vec3> contact = target.getBoundingBox().inflate(0.125).clip(pos, next);
                if (contact.isPresent()) {
                    double distance = pos.distanceToSqr(contact.get());
                    if (distance < nearest) {
                        nearest = distance;
                        endpoint = contact.get();
                        collision = true;
                    }
                }
            }
            points.add(endpoint);
            if (collision) return new Flight(List.copyOf(points), true);
            if (next.y < level.getMinY() - 16) break;
            pos = next;
            velocity = velocity.scale(DRAG).add(0.0, -GRAVITY, 0.0);
        }
        return new Flight(List.copyOf(points), false);
    }

    private static List<Segment> segments(List<Vec3> points, boolean impact) {
        List<Segment> result = new ArrayList<>();
        double length = 0;
        for (int i = 1; i < points.size(); i++) length += points.get(i).distanceTo(points.get(i - 1));
        if (length < 0.00001) return result;
        double distanceSoFar = 0;
        for (int i = 1; i < points.size(); i++) {
            Vec3 from = points.get(i - 1);
            Vec3 diff = points.get(i).subtract(from);
            double size = diff.length();
            if (size < 0.00001) continue;
            double along = 0;
            while (along < size - 0.000001) {
                double phase = (distanceSoFar + along) % (DASH + GAP);
                boolean filled = phase < DASH;
                double span = Math.min(size - along, filled ? DASH - phase : DASH + GAP - phase);
                if (span < 0.000001) { along += 0.00001; continue; }
                if (filled) {
                    double t = (distanceSoFar + along) / length;
                    result.add(new Segment(
                            from.add(diff.scale(along / size)),
                            from.add(diff.scale((along + span) / size)),
                            (int) (100 + 82 * t), (int) (225 - 78 * t), 240));
                }
                along += span;
            }
            distanceSoFar += size;
        }
        if (impact) {
            Vec3 end = points.get(points.size() - 1);
            double r = 0.12;
            result.add(new Segment(end.add(-r, 0, 0), end.add(r, 0, 0), 190, 115, 245));
            result.add(new Segment(end.add(0, -r, 0), end.add(0, r, 0), 190, 115, 245));
            result.add(new Segment(end.add(0, 0, -r), end.add(0, 0, r), 190, 115, 245));
        }
        return result;
    }

    private static void line(VertexConsumer vertex, PoseStack.Pose pose,
                             Vec3 a, Vec3 b, int r, int g, int colorB) {
        Vec3 dir = b.subtract(a);
        float distance = (float) dir.length();
        if (distance < 0.00001f) return;
        float nx = (float) (dir.x / distance), ny = (float) (dir.y / distance), nz = (float) (dir.z / distance);
        vertex.addVertex(pose.pose(), (float) a.x, (float) a.y, (float) a.z)
                .setColor(r, g, colorB, 220).setNormal(nx, ny, nz);
        vertex.addVertex(pose.pose(), (float) b.x, (float) b.y, (float) b.z)
                .setColor(r, g, colorB, 220).setNormal(nx, ny, nz);
    }

    private record Flight(List<Vec3> points, boolean hit) {}
    private record Segment(Vec3 a, Vec3 b, int r, int g, int bColor) {}
}
