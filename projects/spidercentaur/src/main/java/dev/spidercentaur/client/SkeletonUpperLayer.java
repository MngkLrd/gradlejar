package dev.spidercentaur.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.spidercentaur.entity.SpiderCentaurEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.SkeletonModel;
import net.minecraft.client.model.SpiderModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;

public final class SkeletonUpperLayer
        extends RenderLayer<SpiderCentaurEntity, SpiderModel<SpiderCentaurEntity>> {

    private static final ResourceLocation SKELETON_TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/entity/skeleton/skeleton.png");

    private final SkeletonModel<SpiderCentaurEntity> skeleton;
    private final ItemInHandRenderer itemRenderer;
    private final SkeletonDecorations decorations;

    public SkeletonUpperLayer(RenderLayerParent<SpiderCentaurEntity, SpiderModel<SpiderCentaurEntity>> parent,
                              ModelPart root, ItemInHandRenderer itemRenderer) {
        super(parent);
        this.skeleton = new SkeletonModel<>(root);
        this.skeleton.leftLeg.visible = false;
        this.skeleton.rightLeg.visible = false;
        this.itemRenderer = itemRenderer;
        this.decorations = new SkeletonDecorations();
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int light,
                       SpiderCentaurEntity entity, float limbSwing, float limbSwingAmount,
                       float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (entity.isInvisible()) return;

        this.skeleton.attackTime = getParentModel().attackTime;
        this.skeleton.riding = getParentModel().riding;
        this.skeleton.young = getParentModel().young;
        this.skeleton.rightArmPose = HumanoidModel.ArmPose.BOW_AND_ARROW;
        this.skeleton.leftArmPose = HumanoidModel.ArmPose.EMPTY;
        this.skeleton.prepareMobModel(entity, limbSwing, limbSwingAmount, partialTick);
        this.skeleton.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);

        poseStack.pushPose();
        poseStack.translate(0.0D, 0.19D, -0.26D);

        int overlay = LivingEntityRenderer.getOverlayCoords(entity, 0.0F);
        VertexConsumer vertices = buffer.getBuffer(RenderType.entityCutoutNoCull(SKELETON_TEXTURE));
        this.skeleton.renderToBuffer(poseStack, vertices, light, overlay);

        // All accessories follow the animated skeleton torso/head, not the world axes.
        this.decorations.render(skeleton, poseStack, buffer, light, overlay);

        if (!entity.getMainHandItem().isEmpty()) {
            poseStack.pushPose();
            this.skeleton.translateToHand(HumanoidArm.RIGHT, poseStack);
            poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            poseStack.translate(0.0625D, 0.125D, -0.625D);
            this.itemRenderer.renderItem(entity, entity.getMainHandItem(),
                    ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, false, poseStack, buffer, light);
            poseStack.popPose();
        }
        poseStack.popPose();
    }
}
