package dev.ataflore.catchtheblast.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.ataflore.catchtheblast.CatchTheBlast;
import dev.ataflore.catchtheblast.item.Charge;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ShieldModel;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Render through the actual vanilla ShieldModel, not a cube with vanilla
 * display transforms. This preserves the proper hand grip and model origin.
 */
public final class BlastShieldRenderer extends BlockEntityWithoutLevelRenderer {
    private static final ResourceLocation[] TEXTURES = new ResourceLocation[4];
    private static final ResourceLocation[] GLOW = new ResourceLocation[4];
    static {
        for(int i=0;i<4;i++) {
            TEXTURES[i]=new ResourceLocation(CatchTheBlast.ID,"textures/entity/blast_shield_"+i+".png");
            GLOW[i]=new ResourceLocation(CatchTheBlast.ID,"textures/entity/blast_glow_"+i+".png");
        }
    }
    private final ShieldModel shield;

    public BlastShieldRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),
                Minecraft.getInstance().getEntityModels());
        shield = new ShieldModel(Minecraft.getInstance().getEntityModels().bakeLayer(BlastShieldGeometry.LAYER));
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack poseStack,
                             MultiBufferSource buffers, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        poseStack.scale(1.0F, -1.0F, -1.0F);
        int stage = Charge.stage(stack);
        shield.renderToBuffer(poseStack, buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURES[stage])),
                packedLight, packedOverlay, 1f, 1f, 1f, 1f);
        if(stage>0) shield.renderToBuffer(poseStack, buffers.getBuffer(RenderType.eyes(GLOW[stage])),
                LightTexture.FULL_BRIGHT, packedOverlay, 1f, 1f, 1f, 1f);
        poseStack.popPose();
    }
}
