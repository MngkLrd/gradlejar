package dev.minemods.snowleopard.client;

import dev.minemods.snowleopard.SnowLeopardMod;
import dev.minemods.snowleopard.entity.SnowLeopardEntity;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public final class SnowLeopardModel<T extends SnowLeopardEntity> extends HierarchicalModel<T> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
        new ResourceLocation(SnowLeopardMod.MOD_ID, "snow_leopard"), "main");

    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart frontLeft;
    private final ModelPart frontRight;
    private final ModelPart rearLeft;
    private final ModelPart rearRight;
    private final ModelPart tail0;
    private final ModelPart tail1;
    private final ModelPart tail2;

    public SnowLeopardModel(ModelPart root) {
        this.root = root;
        this.head = root.getChild("head");
        this.frontLeft = root.getChild("front_left");
        this.frontRight = root.getChild("front_right");
        this.rearLeft = root.getChild("rear_left");
        this.rearRight = root.getChild("rear_right");
        this.tail0 = root.getChild("tail0");
        this.tail1 = tail0.getChild("tail1");
        this.tail2 = tail1.getChild("tail2");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        CubeDeformation fur = new CubeDeformation(0.05f);

        root.addOrReplaceChild("body", CubeListBuilder.create()
            .texOffs(0, 31).addBox(-6, -5, -9, 12, 10, 18, fur)
            .texOffs(63, 32).addBox(-5, -6, -5, 10, 3, 11, fur),
            PartPose.offset(0, 12, 1));

        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create()
            .texOffs(0, 0).addBox(-5, -5, -9, 10, 8, 9, fur)
            .texOffs(42, 0).addBox(-3, -1, -12, 6, 4, 4, fur)
            .texOffs(64, 0).addBox(-5, -8, -6, 3, 4, 3, fur)
            .texOffs(77, 0).addBox(2, -8, -6, 3, 4, 3, fur),
            PartPose.offset(0, 12, -8));
        head.addOrReplaceChild("chin", CubeListBuilder.create()
            .texOffs(91, 0).addBox(-2.5f, 1.8f, -11.7f, 5, 1.3f, 3.5f),
            PartPose.ZERO);

        root.addOrReplaceChild("front_left", CubeListBuilder.create()
            .texOffs(67, 15).addBox(-2, 0, -2, 4, 9, 4, fur)
            .texOffs(84, 0).addBox(-2.4f, 7, -3.2f, 4.8f, 2, 5, fur),
            PartPose.offset(4, 15, -6));
        root.addOrReplaceChild("front_right", CubeListBuilder.create()
            .texOffs(67, 15).addBox(-2, 0, -2, 4, 9, 4, fur)
            .texOffs(84, 0).addBox(-2.4f, 7, -3.2f, 4.8f, 2, 5, fur),
            PartPose.offset(-4, 15, -6));
        root.addOrReplaceChild("rear_left", CubeListBuilder.create()
            .texOffs(67, 15).addBox(-2.5f, -1, -2.5f, 5, 10, 5, fur)
            .texOffs(84, 0).addBox(-2.4f, 7, -3.2f, 4.8f, 2, 5, fur),
            PartPose.offset(4, 15, 8));
        root.addOrReplaceChild("rear_right", CubeListBuilder.create()
            .texOffs(67, 15).addBox(-2.5f, -1, -2.5f, 5, 10, 5, fur)
            .texOffs(84, 0).addBox(-2.4f, 7, -3.2f, 4.8f, 2, 5, fur),
            PartPose.offset(-4, 15, 8));

        PartDefinition tail0 = root.addOrReplaceChild("tail0", CubeListBuilder.create()
            .texOffs(0, 21).addBox(-2.3f, -2.3f, 0, 4.6f, 4.6f, 7, fur),
            PartPose.offset(0, 11, 10));
        PartDefinition tail1 = tail0.addOrReplaceChild("tail1", CubeListBuilder.create()
            .texOffs(24, 21).addBox(-2.5f, -2.5f, 0, 5, 5, 7, fur),
            PartPose.offset(0, 0, 6));
        tail1.addOrReplaceChild("tail2", CubeListBuilder.create()
            .texOffs(49, 18).addBox(-2.7f, -2.7f, 0, 5.4f, 5.4f, 7, fur),
            PartPose.offset(0, 0, 6));

        return LayerDefinition.create(mesh, 128, 64);
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(T leopard, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        head.yRot = netHeadYaw * ((float)Math.PI / 180f);
        head.xRot = headPitch * ((float)Math.PI / 180f);
        float pace = limbSwing * 0.6662f;
        frontLeft.xRot = Mth.cos(pace) * 1.05f * limbSwingAmount;
        frontRight.xRot = Mth.cos(pace + Mth.PI) * 1.05f * limbSwingAmount;
        rearLeft.xRot = Mth.cos(pace + Mth.PI) * 0.95f * limbSwingAmount;
        rearRight.xRot = Mth.cos(pace) * 0.95f * limbSwingAmount;
        tail0.xRot = -0.28f;
        tail1.xRot = 0.30f;
        tail2.xRot = 0.38f;
        tail0.yRot = Mth.sin(ageInTicks * 0.10f) * 0.17f;
        tail1.yRot = Mth.sin(ageInTicks * 0.10f + 0.7f) * 0.13f;
        tail2.yRot = Mth.sin(ageInTicks * 0.10f + 1.4f) * 0.15f;
    }
}
