package dev.minemods.ropefences.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.minemods.ropefences.block.RopeSpanBlock;
import dev.minemods.ropefences.blockentity.RopeSpanBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4f;

/** The vanilla leash render pipeline: no block atlas UVs, no custom colored texture. */
public final class RopeSpanRenderer implements BlockEntityRenderer<RopeSpanBlockEntity> {
    private static final int STEPS_PER_BLOCK = 8;
    private static final float HALF = (float) RopeSpanBlock.HALF_WIDTH;

    public RopeSpanRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(RopeSpanBlockEntity entity, float partialTick, PoseStack poses,
                       MultiBufferSource buffers, int light, int overlay) {
        BlockState state = entity.getBlockState();
        boolean x = state.getValue(RopeSpanBlock.AXIS) == Direction.Axis.X;
        double first = state.getValue(RopeSpanBlock.NEG_END) ? -0.5 : 0.0;
        double last = state.getValue(RopeSpanBlock.POS_END) ? 1.5 : 1.0;
        RopeSpanBlockEntity.Span span = entity.renderSpan();
        Matrix4f pose = poses.last().pose();
        VertexConsumer out = buffers.getBuffer(RenderType.leash());
        int segments = (int) Math.round((last - first) * STEPS_PER_BLOCK);

        // Two crossed ribbons per cable. Adjacent segments share the same curve.
        for (double height : new double[]{RopeSpanBlock.LOWER_HEIGHT, RopeSpanBlock.UPPER_HEIGHT}) {
            for (int face = 0; face < 2; face++) {
                // Reverse the second ribbon, like vanilla's two-pass leash.
                for (int i = 0; i <= segments; i++) {
                    int step = face == 0 ? i : segments - i;
                    double u = first + step / (double) STEPS_PER_BLOCK;
                    float y = (float) (height - span.sag(u));
                    int band = (int) Math.round((u - span.start()) * STEPS_PER_BLOCK);
                    float shade = (band & 1) == 0 ? 1.0F : 0.7F;
                    // Duplicate strip ends so separate ropes/faces cannot bridge.
                    if (i == 0) vertex(out, pose, x, u, y, face, -HALF, shade, light);
                    vertex(out, pose, x, u, y, face, -HALF, shade, light);
                    vertex(out, pose, x, u, y, face, HALF, shade, light);
                    if (i == segments) vertex(out, pose, x, u, y, face, HALF, shade, light);
                }
            }
        }
    }

    private static void vertex(VertexConsumer out, Matrix4f pose, boolean x, double u, float y,
                               int face, float offset, float shade, int light) {
        float cross = 0.5F;
        if (face == 0) y += offset;
        else cross += offset;
        float along = (float) u;
        out.addVertex(pose, x ? along : cross, y, x ? cross : along)
                .setColor(0.5F * shade, 0.4F * shade, 0.3F * shade, 1.0F)
                .setLight(light);
    }
}
