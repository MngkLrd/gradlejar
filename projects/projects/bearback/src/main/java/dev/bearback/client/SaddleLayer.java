package dev.bearback.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.bearback.Bearback;
import net.minecraft.client.model.PolarBearModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.PolarBear;

/**
 * A fitted leather saddle in local polar-bear BODY coordinates.
 *
 * Vanilla PolarBearModel.createBodyLayer() uses a body pivot at (-2, 9, 12)
 * with a 90-degree X rotation, and these body boxes:
 *   hind torso: x=-5..9,  y=-13..1,   z=-7..4
 *   shoulders:  x=-4..8,  y=-25..-13, z=-7..3
 *
 * Thus the animal's spine is local z=4 (NOT z=15!), centered on local x=2
 * (NOT x=0). Every part below is placed against those exact bounds.
 *
 * The separate pose model reproduces the animated body transform, including
 * the vanilla rearing-up pose. No ItemStack rendering or unparented entity.
 */
public final class SaddleLayer extends RenderLayer<PolarBear, PolarBearModel<PolarBear>> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            Bearback.MOD_ID, "textures/entity/bear_saddle.png");

    private final BearPoseModel poseModel = new BearPoseModel();
    private final ModelPart saddle = createMesh().bakeRoot().getChild("saddle");

    public SaddleLayer(RenderLayerParent<PolarBear, PolarBearModel<PolarBear>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, PolarBear bear,
                       float limbSwing, float limbSwingAmount, float partialTick,
                       float ageInTicks, float netHeadYaw, float headPitch) {
        if (!bear.getData(Bearback.SADDLED.get()) || bear.isBaby()) return;

        poseModel.prepareMobModel(bear, limbSwing, limbSwingAmount, partialTick);
        poseModel.setupAnim(bear, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);

        poseStack.pushPose();
        // Inherit the vanilla torso origin AND its current stand-up rotation.
        poseModel.torso().translateAndRotate(poseStack);
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        saddle.render(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
    }

    private static LayerDefinition createMesh() {
        MeshDefinition mesh = new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("saddle", CubeListBuilder.create()
                // Full-width quilted saddle blanket: rests directly on top of z=4.
                // Vanilla torso spans x=-5..9, so the saddle is 15.0px wide and centered at x=2.
                .texOffs(0, 0).addBox(-5.5F, -12.9F, 3.94F, 15.0F, 10.8F, 0.45F)
                // Low, wide leather rider pad, embedded into the blanket.
                .texOffs(34, 0).addBox(-5.25F, -12.4F, 4.20F, 14.5F, 9.6F, 0.92F)
                // A small front pommel follows the lower shoulders at z=3.
                .texOffs(68, 0).addBox(-4.5F, -14.0F, 3.08F, 13.0F, 1.5F, 2.17F)
                // Raised rear edge stays immediately on the back, rather than floating.
                .texOffs(68, 8).addBox(-5.15F, -3.2F, 4.0F, 14.3F, 1.3F, 1.55F)
                // Flexible side skirts touch the body walls x=-5 and x=9.
                .texOffs(0, 18).addBox(-5.90F, -11.6F, -1.45F, 1.22F, 8.2F, 5.75F)
                .texOffs(22, 18).addBox(8.68F, -11.6F, -1.45F, 1.22F, 8.2F, 5.75F)
                // The girth straps descend on both sides of the rib cage.
                .texOffs(44, 18).addBox(-6.05F, -10.65F, -6.95F, 0.90F, 1.5F, 11.12F)
                .texOffs(76, 18).addBox(9.15F, -10.65F, -6.95F, 0.90F, 1.5F, 11.12F)
                // Belly girth (not the air above the animal).
                .texOffs(0, 39).addBox(-5.6F, -10.65F, -7.21F, 15.2F, 1.5F, 0.6F)
                // Two low-profile bronze buckles.
                .texOffs(39, 39).addBox(-6.29F, -10.92F, -1.0F, 0.55F, 2.05F, 2.2F)
                .texOffs(63, 39).addBox(9.75F, -10.92F, -1.0F, 0.55F, 2.05F, 2.2F),
                PartPose.ZERO);
        return LayerDefinition.create(mesh, 128, 64);
    }

    private static final class BearPoseModel extends PolarBearModel<PolarBear> {
        private BearPoseModel() {
            super(PolarBearModel.createBodyLayer().bakeRoot());
        }

        private ModelPart torso() {
            return this.body;
        }
    }
}
