package dev.minemods.redstonevision.client;

import dev.minemods.redstonevision.RedstoneVision;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

/** Face-mounted goggles. The head part follows normal vanilla head animation. */
public final class GogglesArmorModel extends HumanoidModel<LivingEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
        ResourceLocation.fromNamespaceAndPath(RedstoneVision.MOD_ID, "engineer_goggles"), "main");

    public GogglesArmorModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0f);
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("head", CubeListBuilder.create()
            // Left and right steel housings, mounted in front of eyes at y=-6..-3.
            .texOffs(0, 0).addBox(-3.8f, -6.35f, -5.05f, 3.5f, 3.0f, 1.15f)
            .texOffs(18, 0).addBox(0.3f, -6.35f, -5.05f, 3.5f, 3.0f, 1.15f)
            // Glass lenses sit slightly outside the metallic rims.
            .texOffs(0, 9).addBox(-3.35f, -5.95f, -5.22f, 2.65f, 2.20f, 0.16f)
            .texOffs(16, 9).addBox(0.70f, -5.95f, -5.22f, 2.65f, 2.20f, 0.16f)
            // Nose bridge, left/right fasteners, rear leather head strap.
            .texOffs(36, 0).addBox(-0.55f, -5.40f, -4.95f, 1.10f, 0.70f, 0.80f)
            .texOffs(44, 0).addBox(-4.18f, -5.55f, -3.95f, 0.45f, 1.0f, 7.70f)
            .texOffs(54, 0).addBox(3.73f, -5.55f, -3.95f, 0.45f, 1.0f, 7.70f),
            PartPose.ZERO);
        // No vanilla helmet/hat shell above the head.
        root.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 32);
    }
}
