package dev.norsegear.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.norsegear.NorseGear;
import dev.norsegear.client.ClientRegistration;
import dev.norsegear.entity.SlingStoneEntity;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/** Irregular low-poly 3D pebble in flight. The inventory item remains a 16x16 sprite. */
public final class SlingStoneRenderer extends EntityRenderer<SlingStoneEntity> {
    private static final ResourceLocation STONE_TEXTURE = ResourceLocation.fromNamespaceAndPath(
        NorseGear.MODID, "textures/entity/sling_stone.png");
    private final ModelPart stoneMesh;

    public SlingStoneRenderer(EntityRendererProvider.Context context) {
        super(context);
        stoneMesh = context.bakeLayer(ClientRegistration.SLING_STONE_LAYER);
        shadowRadius = 0.10F;
    }

    @Override
    public void render(SlingStoneEntity stone, float yaw, float partialTick, PoseStack pose,
            MultiBufferSource buffers, int light) {
        pose.pushPose();
        float angle = (stone.tickCount + partialTick) * 23.0F;
        pose.mulPose(Axis.XP.rotationDegrees(angle));
        pose.mulPose(Axis.YP.rotationDegrees(angle * 0.65F));
        stoneMesh.render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(STONE_TEXTURE)),
            light, OverlayTexture.NO_OVERLAY);
        pose.popPose();
        super.render(stone, yaw, partialTick, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(SlingStoneEntity stone) { return STONE_TEXTURE; }
}
