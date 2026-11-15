package dev.lazywolf.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.lazywolf.LazyWolf;
import net.minecraft.client.model.WolfModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Wolf;
import com.mojang.math.Axis;

public final class WolfBagLayer extends RenderLayer<Wolf, WolfModel<Wolf>> {
    public static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(LazyWolf.ID, "textures/entity/wolf_pack.png");
    private final ModelPart pack;

    public WolfBagLayer(RenderLayerParent<Wolf, WolfModel<Wolf>> renderer, ModelPart root) {
        super(renderer);
        this.pack = root.getChild("pack");
    }

    public static LayerDefinition model() {
        MeshDefinition mesh = new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("pack", CubeListBuilder.create()
                // A rolled saddle pad across the spine.
                .texOffs(0, 0).addBox(-3.0F, -2.0F, -3.0F, 6.0F, 2.0F, 6.0F)
                // Left/right chunky leather satchels, complete 3D geometry.
                .texOffs(0, 8).addBox(-6.0F, -1.0F, -2.5F, 3.0F, 5.0F, 5.0F)
                .texOffs(24, 8).addBox(3.0F, -1.0F, -2.5F, 3.0F, 5.0F, 5.0F)
                // Raised flap and brass clasp.
                .texOffs(0, 19).addBox(-6.2F, -2.0F, -2.8F, 3.4F, 2.0F, 5.6F)
                .texOffs(24, 19).addBox(2.8F, -2.0F, -2.8F, 3.4F, 2.0F, 5.6F),
                PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int light, Wolf wolf,
                       float limbSwing, float limbSwingAmount, float partialTick,
                       float ageInTicks, float netHeadYaw, float headPitch) {
        if (!wolf.getData(LazyWolf.HAS_HARNESS.get())) return;
        poseStack.pushPose();
        if (wolf.isBaby()) {
            poseStack.scale(0.7F, 0.7F, 0.7F);
            poseStack.translate(0.0D, 0.35D, 0.0D);
        }
        if (wolf.isInSittingPose()) {
            poseStack.translate(0.0D, -0.08D, 0.15D);
            poseStack.mulPose(Axis.XP.rotationDegrees(-15.0F));
        }
        poseStack.translate(0.0D, 0.66D, 0.0D);
        pack.render(poseStack, buffer.getBuffer(RenderType.entityCutout(TEXTURE)), light, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
    }
}
