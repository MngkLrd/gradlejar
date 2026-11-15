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
 * Two Minecraft-style leather panniers, matched to the real body pivot
 * (-2,9,12) and animated together with the bear torso, as with SaddleLayer.
 */
public final class SaddlebagsLayer extends RenderLayer<PolarBear, PolarBearModel<PolarBear>> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(
            Bearback.MOD_ID, "textures/entity/bear_saddlebags.png");
    private final PouchPoseModel poseModel = new PouchPoseModel();
    private final ModelPart bags = mesh().bakeRoot().getChild("bags");

    public SaddlebagsLayer(RenderLayerParent<PolarBear, PolarBearModel<PolarBear>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffer, int light, PolarBear bear,
                       float limbSwing, float limbSwingAmount, float partialTick,
                       float age, float yaw, float pitch) {
        if (!ClientHooks.hasBags(bear) || bear.isBaby()) return;
        poseModel.prepareMobModel(bear, limbSwing, limbSwingAmount, partialTick);
        poseModel.setupAnim(bear, limbSwing, limbSwingAmount, age, yaw, pitch);
        pose.pushPose();
        poseModel.torso().translateAndRotate(pose);
        VertexConsumer vertices = buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        bags.render(pose, vertices, light, OverlayTexture.NO_OVERLAY);
        pose.popPose();
    }

    private static LayerDefinition mesh() {
        MeshDefinition mesh = new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("bags", CubeListBuilder.create()
                // Left and right satchels hug the body sides (body x=-5..9).
                // Body-local Y runs along the bear's length, Z is vertical after rotation.
                .texOffs(0, 0).addBox(-9.8F, -11.7F, -1.75F, 4.1F, 8.4F, 6.3F)
                .texOffs(34, 0).addBox(9.7F, -11.7F, -1.75F, 4.1F, 8.4F, 6.3F)
                // Outer lid flaps, leather piping and buckles.
                .texOffs(70, 0).addBox(-10.1F, -12.0F, 2.45F, 4.7F, 9.0F, 2.0F)
                .texOffs(96, 0).addBox(9.4F, -12.0F, 2.45F, 4.7F, 9.0F, 2.0F)
                .texOffs(0, 22).addBox(-6.1F, -10.6F, 4.14F, 1.0F, 6.3F, 0.48F)
                .texOffs(48, 22).addBox(9.1F, -10.6F, 4.14F, 1.0F, 6.3F, 0.48F)
                .texOffs(72, 40).addBox(-10.3F, -9.0F, 0.55F, 0.6F, 2.0F, 2.5F)
                .texOffs(94, 40).addBox(13.7F, -9.0F, 0.55F, 0.6F, 2.0F, 2.5F),
                PartPose.ZERO);
        return LayerDefinition.create(mesh, 128, 64);
    }

    private static final class PouchPoseModel extends PolarBearModel<PolarBear> {
        PouchPoseModel() {
            super(PolarBearModel.createBodyLayer().bakeRoot());
        }
        ModelPart torso() { return this.body; }
    }
}
