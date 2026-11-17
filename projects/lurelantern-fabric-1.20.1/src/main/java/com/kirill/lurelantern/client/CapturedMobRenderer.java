package com.kirill.lurelantern.client;

import com.kirill.lurelantern.block.LureLanternBlock;
import com.kirill.lurelantern.blockentity.LureLanternBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LanternBlock;
import org.joml.Matrix4f;
import com.mojang.math.Axis;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Client-only non-spawning preview. Fits any entity inside the existing vanilla
 * lantern cavity while preserving its vanilla renderer and textures.
 */
public final class CapturedMobRenderer implements BlockEntityRenderer<LureLanternBlockEntity> {
    private static final ResourceLocation AURA_TEXTURE = new ResourceLocation(
            "lurelantern", "textures/entity/preview_aura.png");
    private static final RenderType AURA_LAYER = RenderType.entityTranslucentEmissive(AURA_TEXTURE);
    private static final float MAX_PREVIEW_WIDTH = 0.28F;
    private static final float MAX_PREVIEW_HEIGHT = 0.28F;

    private final EntityRenderDispatcher dispatcher;
    private final Map<LureLanternBlockEntity, CachedPreview> previews = new WeakHashMap<>();
    private record CachedPreview(int revision, Entity entity) { }

    public CapturedMobRenderer(BlockEntityRendererProvider.Context context) {
        this.dispatcher = context.getEntityRenderer();
    }

    @Override
    public void render(LureLanternBlockEntity lantern, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int packedLight, int packedOverlay) {
        Level level = lantern.getLevel();
        if (level == null || !LureLanternBlock.isAdvancedLantern(lantern.getBlockState())
                || !lantern.hasCapturedMob()) {
            previews.remove(lantern);
            return;
        }
        int revision = lantern.getPreviewRevision();
        CachedPreview existing = previews.get(lantern);
        Entity visual = existing != null && existing.revision() == revision ? existing.entity() : null;
        if (visual == null) {
            CompoundTag data = lantern.getCapturedMobTagForPreview();
            if (data == null || !data.contains("id", 8)) return;
            try {
                // loadEntityRecursive creates an object for rendering; it never adds it
                // to the level or calls the original's gameplay spawning routines.
                visual = EntityType.loadEntityRecursive(data, level, entity -> entity);
                if (visual == null) return;
                visual.setNoGravity(true);
                previews.put(lantern, new CachedPreview(revision, visual));
            } catch (RuntimeException ex) {
                return; // A malformed modded mob must not crash the client.
            }
        }

        renderMiniature(visual, level, partialTick,
                lantern.getBlockState().getValue(LanternBlock.HANGING),
                pose, buffers, dispatcher, lantern.getBlockPos().asLong());
    }

    /** Shared by world block rendering and filled-item BEWLR. */
    public static void renderMiniature(Entity visual, Level level, float partialTick,
                                      boolean hanging, PoseStack pose, MultiBufferSource buffers,
                                      EntityRenderDispatcher dispatcher, long animationSeed) {
        float width = Math.max(0.05F, visual.getBbWidth());
        float height = Math.max(0.05F, visual.getBbHeight());
        float scale = Math.min(0.75F,
                Math.min(MAX_PREVIEW_WIDTH / width, MAX_PREVIEW_HEIGHT / height));
        float halfHeight = height * scale / 2.0F;
        float pivotY = hanging ? 0.27F : 0.20F;
        float spin = ((level.getGameTime() + partialTick) * 2.0F
                + (animationSeed & 63)) % 360.0F;

        pose.pushPose();
        try {
            pose.translate(0.5D, pivotY, 0.5D);
            // Rotate only the display; never rotate/move the stored animal NBT.
            pose.mulPose(Axis.YP.rotationDegrees(spin));
            renderAura(pose, buffers, visual);
            pose.translate(0.0D, -halfHeight, 0.0D);
            pose.scale(scale, scale, scale);
            try {
                dispatcher.render(visual, 0, 0, 0, 0, partialTick,
                        pose, buffers, LightTexture.FULL_BRIGHT);
            } catch (RuntimeException ignored) {
                // Misbehaving optional entity renderers should not take down Minecraft.
            }
        } finally {
            pose.popPose();
        }
    }

    /** Restrained vanilla-looking pixel aura: color depends on the mob category. */
    private static void renderAura(PoseStack pose, MultiBufferSource buffers, Entity entity) {
        int r, g, b;
        if (entity instanceof AbstractVillager) {
            r = 102; g = 170; b = 214; // villagers: sky blue
        } else if (entity instanceof TamableAnimal) {
            r = 176; g = 149; b = 213; // companions: muted lilac
        } else if (entity instanceof Animal) {
            r = 133; g = 185; b = 110; // farm animals: green
        } else {
            r = 196; g = 172; b = 116; // modded/other: muted gold
        }
        VertexConsumer consumer = buffers.getBuffer(AURA_LAYER);
        Matrix4f matrix = pose.last().pose();
        // Crossed quads stay inside the metal/glass lantern cavity; no light sources.
        quad(consumer, matrix, 0, r, g, b);
        quad(consumer, matrix, 1, r, g, b);
    }

    private static void quad(VertexConsumer out, Matrix4f matrix, int axis,
                             int r, int g, int b) {
        final float radius = 0.145F;
        final int opacity = 92;
        for (int i = 0; i < 4; i++) {
            float u = (i == 1 || i == 2) ? 1 : 0;
            float v = (i >= 2) ? 0 : 1;
            float x = axis == 0 ? (u * 2 - 1) * radius : 0;
            float z = axis == 1 ? (u * 2 - 1) * radius : 0;
            float y = (v * 2 - 1) * radius;
            out.vertex(matrix,x,y,z).color(r,g,b,opacity).uv(u,v)
                    .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT)
                    .normal(0,0,1).endVertex();
        }
    }
}
