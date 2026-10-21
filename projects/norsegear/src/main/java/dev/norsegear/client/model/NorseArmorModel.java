package dev.norsegear.client.model;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.world.entity.LivingEntity;

/** Cubic armor silhouette, all details parented to moving humanoid limbs. */
public final class NorseArmorModel extends HumanoidModel<LivingEntity> {
    public NorseArmorModel(ModelPart root) { super(root); }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = HumanoidModel.createMesh(new CubeDeformation(0.55F), 0.0F);
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.getChild("head");
        head.addOrReplaceChild("nasal_guard", CubeListBuilder.create().texOffs(30, 16)
            .addBox(-0.95F, -5.0F, -5.45F, 1.9F, 5.0F, 1.6F), net.minecraft.client.model.geom.PartPose.ZERO);
        head.addOrReplaceChild("brow", CubeListBuilder.create().texOffs(0, 16)
            .addBox(-4.6F, -5.8F, -4.9F, 9.2F, 1.5F, 1.1F), net.minecraft.client.model.geom.PartPose.ZERO);
        head.addOrReplaceChild("helm_crown", CubeListBuilder.create().texOffs(0, 0)
            .addBox(-3.1F, -9.55F, -3.1F, 6.2F, 1.5F, 6.2F), net.minecraft.client.model.geom.PartPose.ZERO);
        head.addOrReplaceChild("left_cheek", CubeListBuilder.create().texOffs(30, 16)
            .addBox(3.5F, -4.6F, -3.5F, 1.3F, 4.2F, 5.0F), net.minecraft.client.model.geom.PartPose.ZERO);
        head.addOrReplaceChild("right_cheek", CubeListBuilder.create().texOffs(30, 16)
            .addBox(-4.8F, -4.6F, -3.5F, 1.3F, 4.2F, 5.0F), net.minecraft.client.model.geom.PartPose.ZERO);

        PartDefinition body = root.getChild("body");
        body.addOrReplaceChild("breast_plate", CubeListBuilder.create().texOffs(16, 16)
            .addBox(-4.1F, 0.1F, -3.65F, 8.2F, 7.5F, 1.1F), net.minecraft.client.model.geom.PartPose.ZERO);
        body.addOrReplaceChild("belt", CubeListBuilder.create().texOffs(16, 16)
            .addBox(-4.8F, 8.2F, -3.55F, 9.6F, 2.4F, 7.1F), net.minecraft.client.model.geom.PartPose.ZERO);
        body.addOrReplaceChild("belt_buckle", CubeListBuilder.create().texOffs(30, 16)
            .addBox(-1.3F, 8.2F, -4.0F, 2.6F, 2.4F, 0.95F), net.minecraft.client.model.geom.PartPose.ZERO);
        body.addOrReplaceChild("back_plate", CubeListBuilder.create().texOffs(16, 16)
            .addBox(-4.1F, 0.5F, 2.8F, 8.2F, 7.0F, 0.9F), net.minecraft.client.model.geom.PartPose.ZERO);

        root.getChild("right_arm").addOrReplaceChild("pauldron", CubeListBuilder.create().texOffs(16, 16)
            .addBox(-4.6F, -2.3F, -2.9F, 5.8F, 4.2F, 5.8F), net.minecraft.client.model.geom.PartPose.ZERO);
        root.getChild("left_arm").addOrReplaceChild("pauldron", CubeListBuilder.create().texOffs(16, 16)
            .addBox(-1.2F, -2.3F, -2.9F, 5.8F, 4.2F, 5.8F), net.minecraft.client.model.geom.PartPose.ZERO);
        root.getChild("left_leg").addOrReplaceChild("knee_plate", CubeListBuilder.create().texOffs(30, 16)
            .addBox(-2.35F, 5.1F, -2.95F, 4.7F, 3.2F, 1.2F), net.minecraft.client.model.geom.PartPose.ZERO);
        root.getChild("right_leg").addOrReplaceChild("knee_plate", CubeListBuilder.create().texOffs(30, 16)
            .addBox(-2.35F, 5.1F, -2.95F, 4.7F, 3.2F, 1.2F), net.minecraft.client.model.geom.PartPose.ZERO);
        root.getChild("left_leg").addOrReplaceChild("boot_toe", CubeListBuilder.create().texOffs(16, 16)
            .addBox(-2.45F, 9.6F, -4.0F, 4.9F, 2.3F, 5.8F), net.minecraft.client.model.geom.PartPose.ZERO);
        root.getChild("right_leg").addOrReplaceChild("boot_toe", CubeListBuilder.create().texOffs(16, 16)
            .addBox(-2.45F, 9.6F, -4.0F, 4.9F, 2.3F, 5.8F), net.minecraft.client.model.geom.PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 32);
    }

    /** Set the visible humanoid parts AND the accessory geometry for each slot. */
    public void onlyPiece(net.minecraft.world.entity.EquipmentSlot slot) {
        setAllVisible(false);
        boolean helmet = slot == net.minecraft.world.entity.EquipmentSlot.HEAD;
        boolean chest = slot == net.minecraft.world.entity.EquipmentSlot.CHEST;
        boolean legs = slot == net.minecraft.world.entity.EquipmentSlot.LEGS;
        boolean boots = slot == net.minecraft.world.entity.EquipmentSlot.FEET;
        head.visible = helmet;
        body.visible = chest;
        leftArm.visible = chest;
        rightArm.visible = chest;
        leftLeg.visible = legs || boots;
        rightLeg.visible = legs || boots;
        for (ModelPart leg : new ModelPart[]{leftLeg, rightLeg}) {
            leg.getChild("knee_plate").visible = legs;
            leg.getChild("boot_toe").visible = boots;
        }
    }
}
