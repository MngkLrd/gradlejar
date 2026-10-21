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
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.PolarBear;

/**
 * Independent 3D leather saddle mesh, attached to the animated vanilla bear torso.
 * This does NOT render a saddle item and therefore never depends on the item GUI transform.
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

        // Replicate the current bear torso animation instead of placing a 2D item in world space.
        poseModel.prepareMobModel(bear, limbSwing, limbSwingAmount, partialTick);
        poseModel.setupAnim(bear, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);

        poseStack.pushPose();
        poseModel.torso().translateAndRotate(poseStack);
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        saddle.render(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
    }

    private static LayerDefinition createMesh() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // Geometry uses the same local coordinate system and pivot as the vanilla polar bear body.
        // Body top corresponds approximately to local Z = 15 while the body is rotated on X.
        root.addOrReplaceChild("saddle", CubeListBuilder.create()
                // Shaped padded leather seat resting on the spine.
                .texOffs(0, 0).addBox(-5.4F, -10.4F, 14.3F, 10.8F, 8.2F, 1.8F)
                // Elevated leather pommel and cantle, not a huge floating item.
                .texOffs(32, 0).addBox(-5.5F, -10.6F, 15.6F, 11.0F, 1.1F, 1.5F)
                .texOffs(66, 0).addBox(-5.5F, -3.2F, 15.6F, 11.0F, 1.1F, 1.5F)
                // Left and right leather flaps hug the animal's shoulders.
                .texOffs(0, 20).addBox(-5.9F, -10.0F, 7.1F, 1.1F, 7.5F, 7.7F)
                .texOffs(22, 20).addBox(4.8F, -10.0F, 7.1F, 1.1F, 7.5F, 7.7F)
                // Narrow girth straps with metal buckles, both on the body.
                .texOffs(46, 20).addBox(-6.05F, -7.2F, 5.2F, 0.8F, 2.1F, 9.0F)
                .texOffs(66, 20).addBox(5.25F, -7.2F, 5.2F, 0.8F, 2.1F, 9.0F)
                .texOffs(86, 20).addBox(-6.25F, -7.35F, 9.0F, 0.5F, 2.4F, 2.4F)
                .texOffs(106, 20).addBox(5.75F, -7.35F, 9.0F, 0.5F, 2.4F, 2.4F),
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
