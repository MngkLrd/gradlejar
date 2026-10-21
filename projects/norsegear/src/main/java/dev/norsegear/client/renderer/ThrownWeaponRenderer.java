package dev.norsegear.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.norsegear.entity.ThrownWeaponEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** Stable 3D orientation; the axe spins around its transverse axis during flight. */
public final class ThrownWeaponRenderer extends EntityRenderer<ThrownWeaponEntity> {
    private final net.minecraft.client.renderer.entity.ItemRenderer itemRenderer;

    public ThrownWeaponRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
        this.shadowRadius = 0.11F;
    }

    @Override
    public void render(ThrownWeaponEntity entity, float yaw, float partialTick,
                       PoseStack pose, MultiBufferSource buffers, int light) {
        pose.pushPose();
        float interpolatedYaw = Mth.lerp(partialTick, entity.yRotO, entity.getYRot());
        float interpolatedPitch = Mth.lerp(partialTick, entity.xRotO, entity.getXRot());
        pose.mulPose(Axis.YP.rotationDegrees(180.0F - interpolatedYaw));
        pose.mulPose(Axis.XP.rotationDegrees(90.0F - interpolatedPitch));
        if (!entity.isSpearShape()) {
            pose.mulPose(Axis.ZP.rotationDegrees((entity.tickCount + partialTick) * 31.0F));
        }
        // JSON model is real cuboid geometry, not an item-generated billboard.
        pose.translate(-0.5D, -0.5D, -0.5D);
        ItemStack stack = entity.getItem();
        itemRenderer.renderStatic(stack, ItemDisplayContext.FIXED, light,
            OverlayTexture.NO_OVERLAY, pose, buffers, entity.level(), entity.getId());
        pose.popPose();
        super.render(entity, yaw, partialTick, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(ThrownWeaponEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
