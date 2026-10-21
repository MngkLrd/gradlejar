package dev.norsegear.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.norsegear.entity.SlingStoneEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;

/** Renders a physical cuboid stone, not a snowball-like flat item sprite. */
public final class SlingStoneRenderer extends EntityRenderer<SlingStoneEntity> {
    private final ItemRenderer items;

    public SlingStoneRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.items = context.getItemRenderer();
        this.shadowRadius = 0.10F;
    }

    @Override
    public void render(SlingStoneEntity stone, float yaw, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int light) {
        pose.pushPose();
        pose.scale(1.6F, 1.6F, 1.6F);
        float angle = (stone.tickCount + partialTick) * 24.0F;
        pose.mulPose(Axis.XP.rotationDegrees(angle));
        pose.mulPose(Axis.YP.rotationDegrees(angle * 0.7F));
        items.renderStatic(stone.getItem(), ItemDisplayContext.FIXED,
            light, OverlayTexture.NO_OVERLAY, pose, buffers, stone.level(), stone.getId());
        pose.popPose();
        super.render(stone, yaw, partialTick, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(SlingStoneEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
