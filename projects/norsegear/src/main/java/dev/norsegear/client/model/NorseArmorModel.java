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
        // Replace the opaque vanilla head shell: face, mouth and eyes stay uncovered.
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(),
            net.minecraft.client.model.geom.PartPose.ZERO);
        head.addOrReplaceChild("helm_cap", CubeListBuilder.create().texOffs(1, 1)
            .addBox(-4.25F, -8.5F, -4.25F, 8.5F, 2.0F, 8.5F),
            net.minecraft.client.model.geom.PartPose.ZERO);
        head.addOrReplaceChild("brow", CubeListBuilder.create().texOffs(37, 1)
            .addBox(-4.5F, -6.6F, -4.9F, 9.0F, 1.25F, 1.05F),
            net.minecraft.client.model.geom.PartPose.ZERO);
        head.addOrReplaceChild("nasal_guard", CubeListBuilder.create().texOffs(60, 1)
            .addBox(-0.5F, -5.6F, -5.05F, 1.0F, 3.2F, 0.9F),
            net.minecraft.client.model.geom.PartPose.ZERO);
        head.addOrReplaceChild("left_temple", CubeListBuilder.create().texOffs(66, 1)
            .addBox(3.65F, -6.25F, -2.9F, 0.9F, 3.5F, 6.0F),
            net.minecraft.client.model.geom.PartPose.ZERO);
        head.addOrReplaceChild("right_temple", CubeListBuilder.create().texOffs(82, 1)
            .addBox(-4.55F, -6.25F, -2.9F, 0.9F, 3.5F, 6.0F),
            net.minecraft.client.model.geom.PartPose.ZERO);
        head.addOrReplaceChild("back_rim", CubeListBuilder.create().texOffs(98, 1)
            .addBox(-3.75F, -6.3F, 3.55F, 7.5F, 2.6F, 1.0F),
            net.minecraft.client.model.geom.PartPose.ZERO);

        // Torso is intentionally not an opaque vanilla armor box. The open
        // chainmail weave in mail_front must show the underlying player skin.
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create(),
            net.minecraft.client.model.geom.PartPose.ZERO);
        body.addOrReplaceChild("mail_front", CubeListBuilder.create().texOffs(117, 1)
            .addBox(-3.65F, 1.4F, -3.18F, 7.3F, 7.0F, 0.35F),
            net.minecraft.client.model.geom.PartPose.ZERO);
        body.addOrReplaceChild("left_leather_edge", CubeListBuilder.create().texOffs(135, 1)
            .addBox(3.32F, 1.6F, -3.45F, 1.1F, 6.8F, 1.0F),
            net.minecraft.client.model.geom.PartPose.ZERO);
        body.addOrReplaceChild("right_leather_edge", CubeListBuilder.create().texOffs(142, 1)
            .addBox(-4.42F, 1.6F, -3.45F, 1.1F, 6.8F, 1.0F),
            net.minecraft.client.model.geom.PartPose.ZERO);
        body.addOrReplaceChild("upper_chest_strap", CubeListBuilder.create().texOffs(149, 1)
            .addBox(-4.4F, 1.0F, -3.68F, 8.8F, 1.15F, 1.25F),
            net.minecraft.client.model.geom.PartPose.ZERO);
        body.addOrReplaceChild("belt", CubeListBuilder.create().texOffs(172, 1)
            .addBox(-4.4F, 8.15F, -3.12F, 8.8F, 2.0F, 6.24F),
            net.minecraft.client.model.geom.PartPose.ZERO);
        body.addOrReplaceChild("belt_buckle", CubeListBuilder.create().texOffs(205, 1)
            .addBox(-1.1F, 8.1F, -3.9F, 2.2F, 2.1F, 0.95F),
            net.minecraft.client.model.geom.PartPose.ZERO);
        body.addOrReplaceChild("back_plate", CubeListBuilder.create().texOffs(214, 1)
            .addBox(-3.65F, 1.3F, 2.85F, 7.3F, 6.75F, 0.6F),
            net.minecraft.client.model.geom.PartPose.ZERO);

        root.getChild("right_arm").addOrReplaceChild("pauldron", CubeListBuilder.create().texOffs(1, 14)
            .addBox(-4.1F, -2.05F, -2.6F, 5.2F, 3.4F, 5.2F), net.minecraft.client.model.geom.PartPose.ZERO);
        root.getChild("left_arm").addOrReplaceChild("pauldron", CubeListBuilder.create().texOffs(24, 14)
            .addBox(-1.1F, -2.05F, -2.6F, 5.2F, 3.4F, 5.2F), net.minecraft.client.model.geom.PartPose.ZERO);
        root.getChild("left_leg").addOrReplaceChild("knee_plate", CubeListBuilder.create().texOffs(47, 14)
            .addBox(-2.35F, 5.1F, -2.95F, 4.7F, 3.2F, 1.2F), net.minecraft.client.model.geom.PartPose.ZERO);
        root.getChild("right_leg").addOrReplaceChild("knee_plate", CubeListBuilder.create().texOffs(61, 14)
            .addBox(-2.35F, 5.1F, -2.95F, 4.7F, 3.2F, 1.2F), net.minecraft.client.model.geom.PartPose.ZERO);
        root.getChild("left_leg").addOrReplaceChild("boot_toe", CubeListBuilder.create().texOffs(75, 14)
            .addBox(-2.45F, 9.6F, -4.0F, 4.9F, 2.3F, 5.8F), net.minecraft.client.model.geom.PartPose.ZERO);
        root.getChild("right_leg").addOrReplaceChild("boot_toe", CubeListBuilder.create().texOffs(99, 14)
            .addBox(-2.45F, 9.6F, -4.0F, 4.9F, 2.3F, 5.8F), net.minecraft.client.model.geom.PartPose.ZERO);
        // Ornamental highlights and structured armor silhouette. These remain
        // attached to animated limb bones and obey visibility masking below.
        head.addOrReplaceChild("helm_ridge", CubeListBuilder.create().texOffs(123, 14)
            .addBox(-0.7F, -9.08F, -4.45F, 1.4F, 1.6F, 8.9F),
            net.minecraft.client.model.geom.PartPose.ZERO);
        head.addOrReplaceChild("brow_emblem", CubeListBuilder.create().texOffs(146, 14)
            .addBox(-1.15F, -6.65F, -5.12F, 2.3F, 1.4F, 0.65F),
            net.minecraft.client.model.geom.PartPose.ZERO);
        head.addOrReplaceChild("back_strap", CubeListBuilder.create().texOffs(154, 14)
            .addBox(-3.8F, -3.85F, 3.8F, 7.6F, 0.8F, 0.72F),
            net.minecraft.client.model.geom.PartPose.ZERO);
        body.addOrReplaceChild("fur_collar", CubeListBuilder.create().texOffs(173, 14)
            .addBox(-4.25F, -0.22F, -3.0F, 8.5F, 1.35F, 6.0F),
            net.minecraft.client.model.geom.PartPose.ZERO);
        body.addOrReplaceChild("cross_chest_plate", CubeListBuilder.create().texOffs(204, 14)
            .addBox(-1.45F, 2.75F, -3.90F, 2.9F, 3.4F, 0.7F),
            net.minecraft.client.model.geom.PartPose.ZERO);
        body.addOrReplaceChild("chest_buckle", CubeListBuilder.create().texOffs(214, 14)
            .addBox(-1.0F, 3.4F, -4.13F, 2.0F, 2.05F, 0.65F),
            net.minecraft.client.model.geom.PartPose.ZERO);
        root.getChild("right_arm").addOrReplaceChild("pauldron_rim", CubeListBuilder.create().texOffs(222, 14)
            .addBox(-4.24F, -2.22F, -2.70F, 5.46F, 0.8F, 5.45F),
            net.minecraft.client.model.geom.PartPose.ZERO);
        root.getChild("left_arm").addOrReplaceChild("pauldron_rim", CubeListBuilder.create().texOffs(1, 27)
            .addBox(-1.22F, -2.22F, -2.70F, 5.46F, 0.8F, 5.45F),
            net.minecraft.client.model.geom.PartPose.ZERO);
        head.addOrReplaceChild("crown_upper_band", CubeListBuilder.create().texOffs(25, 27)
            .addBox(-4.34F, -8.60F, -4.35F, 8.68F, 0.48F, 8.70F),
            net.minecraft.client.model.geom.PartPose.ZERO);
        head.addOrReplaceChild("crown_lower_band", CubeListBuilder.create().texOffs(62, 27)
            .addBox(-4.35F, -6.45F, -4.52F, 8.70F, 0.48F, 8.90F),
            net.minecraft.client.model.geom.PartPose.ZERO);
        head.addOrReplaceChild("nasal_crest", CubeListBuilder.create().texOffs(100, 27)
            .addBox(-0.36F, -6.65F, -5.35F, 0.72F, 4.35F, 0.42F),
            net.minecraft.client.model.geom.PartPose.ZERO);
        head.addOrReplaceChild("left_cheek_rim", CubeListBuilder.create().texOffs(105, 27)
            .addBox(3.72F, -4.10F, -4.24F, 0.62F, 2.35F, 1.50F),
            net.minecraft.client.model.geom.PartPose.ZERO);
        head.addOrReplaceChild("right_cheek_rim", CubeListBuilder.create().texOffs(112, 27)
            .addBox(-4.34F, -4.10F, -4.24F, 0.62F, 2.35F, 1.50F),
            net.minecraft.client.model.geom.PartPose.ZERO);
        head.addOrReplaceChild("back_brow_stud", CubeListBuilder.create().texOffs(119, 27)
            .addBox(-0.55F, -6.56F, 4.25F, 1.10F, 0.70F, 0.42F),
            net.minecraft.client.model.geom.PartPose.ZERO);
        head.addOrReplaceChild("headband_stud_left", CubeListBuilder.create().texOffs(125, 27)
            .addBox(-3.15F, -6.78F, -5.16F, 0.65F, 0.63F, 0.46F),
            net.minecraft.client.model.geom.PartPose.ZERO);
        head.addOrReplaceChild("headband_stud_right", CubeListBuilder.create().texOffs(130, 27)
            .addBox(2.55F, -6.78F, -5.16F, 0.65F, 0.63F, 0.46F),
            net.minecraft.client.model.geom.PartPose.ZERO);
        body.addOrReplaceChild("left_chest_strap", CubeListBuilder.create().texOffs(135, 27)
            .addBox(-3.58F, 1.60F, -3.95F, 0.92F, 6.18F, 0.58F),
            net.minecraft.client.model.geom.PartPose.ZERO);
        body.addOrReplaceChild("right_chest_strap", CubeListBuilder.create().texOffs(140, 27)
            .addBox(2.65F, 1.60F, -3.95F, 0.92F, 6.18F, 0.58F),
            net.minecraft.client.model.geom.PartPose.ZERO);
        body.addOrReplaceChild("left_armor_rail", CubeListBuilder.create().texOffs(145, 27)
            .addBox(-4.15F, 3.00F, -3.66F, 0.66F, 4.60F, 0.64F),
            net.minecraft.client.model.geom.PartPose.ZERO);
        body.addOrReplaceChild("right_armor_rail", CubeListBuilder.create().texOffs(150, 27)
            .addBox(3.49F, 3.00F, -3.66F, 0.66F, 4.60F, 0.64F),
            net.minecraft.client.model.geom.PartPose.ZERO);
        body.addOrReplaceChild("chest_heart_plate", CubeListBuilder.create().texOffs(155, 27)
            .addBox(-1.35F, 3.35F, -4.33F, 2.70F, 1.65F, 0.58F),
            net.minecraft.client.model.geom.PartPose.ZERO);
        body.addOrReplaceChild("belt_lower_trim", CubeListBuilder.create().texOffs(164, 27)
            .addBox(-4.33F, 9.75F, -3.16F, 8.66F, 0.50F, 6.32F),
            net.minecraft.client.model.geom.PartPose.ZERO);
        body.addOrReplaceChild("left_mail_tab", CubeListBuilder.create().texOffs(196, 27)
            .addBox(-3.80F, 10.10F, -3.01F, 2.00F, 2.80F, 0.85F),
            net.minecraft.client.model.geom.PartPose.ZERO);
        body.addOrReplaceChild("right_mail_tab", CubeListBuilder.create().texOffs(204, 27)
            .addBox(1.80F, 10.10F, -3.01F, 2.00F, 2.80F, 0.85F),
            net.minecraft.client.model.geom.PartPose.ZERO);
        body.addOrReplaceChild("back_horizontal_strap", CubeListBuilder.create().texOffs(212, 27)
            .addBox(-3.90F, 4.55F, 3.34F, 7.80F, 0.90F, 0.75F),
            net.minecraft.client.model.geom.PartPose.ZERO);
        root.getChild("right_arm").addOrReplaceChild("pauldron_top_plate", CubeListBuilder.create().texOffs(1, 39)
            .addBox(-4.23F, -2.48F, -2.75F, 5.38F, 1.05F, 5.50F),
            net.minecraft.client.model.geom.PartPose.ZERO);
        root.getChild("left_arm").addOrReplaceChild("pauldron_top_plate", CubeListBuilder.create().texOffs(25, 39)
            .addBox(-1.15F, -2.48F, -2.75F, 5.38F, 1.05F, 5.50F),
            net.minecraft.client.model.geom.PartPose.ZERO);
        root.getChild("right_arm").addOrReplaceChild("upper_arm_plate", CubeListBuilder.create().texOffs(49, 39)
            .addBox(-3.70F, 1.00F, -2.55F, 4.85F, 2.30F, 5.07F),
            net.minecraft.client.model.geom.PartPose.ZERO);
        root.getChild("left_arm").addOrReplaceChild("upper_arm_plate", CubeListBuilder.create().texOffs(71, 39)
            .addBox(-1.14F, 1.00F, -2.55F, 4.85F, 2.30F, 5.07F),
            net.minecraft.client.model.geom.PartPose.ZERO);
        root.getChild("right_arm").addOrReplaceChild("arm_bracer", CubeListBuilder.create().texOffs(93, 39)
            .addBox(-3.52F, 6.05F, -2.37F, 4.44F, 2.00F, 4.74F),
            net.minecraft.client.model.geom.PartPose.ZERO);
        root.getChild("left_arm").addOrReplaceChild("arm_bracer", CubeListBuilder.create().texOffs(114, 39)
            .addBox(-0.92F, 6.05F, -2.37F, 4.44F, 2.00F, 4.74F),
            net.minecraft.client.model.geom.PartPose.ZERO);
        root.getChild("right_leg").addOrReplaceChild("greave_front", CubeListBuilder.create().texOffs(135, 39)
            .addBox(-2.28F, 7.05F, -3.04F, 4.56F, 2.30F, 0.82F),
            net.minecraft.client.model.geom.PartPose.ZERO);
        root.getChild("left_leg").addOrReplaceChild("greave_front", CubeListBuilder.create().texOffs(148, 39)
            .addBox(-2.28F, 7.05F, -3.04F, 4.56F, 2.30F, 0.82F),
            net.minecraft.client.model.geom.PartPose.ZERO);
        root.getChild("right_leg").addOrReplaceChild("shin_band", CubeListBuilder.create().texOffs(161, 39)
            .addBox(-2.35F, 8.85F, -3.09F, 4.70F, 0.56F, 1.13F),
            net.minecraft.client.model.geom.PartPose.ZERO);
        root.getChild("left_leg").addOrReplaceChild("shin_band", CubeListBuilder.create().texOffs(175, 39)
            .addBox(-2.35F, 8.85F, -3.09F, 4.70F, 0.56F, 1.13F),
            net.minecraft.client.model.geom.PartPose.ZERO);
        return LayerDefinition.create(mesh, 256, 256);
    }

    /**
     * Slot masking must be reversible. Individual ModelPart children hold the
     * volumetric details, and must be switched back on after a previous slot
     * hid them. Otherwise the first armor piece to render can affect the next.
     */
    public void onlyPiece(net.minecraft.world.entity.EquipmentSlot slot) {
        setAllVisible(false);
        boolean helmet = slot == net.minecraft.world.entity.EquipmentSlot.HEAD;
        boolean chest = slot == net.minecraft.world.entity.EquipmentSlot.CHEST;
        boolean legs = slot == net.minecraft.world.entity.EquipmentSlot.LEGS;
        boolean boots = slot == net.minecraft.world.entity.EquipmentSlot.FEET;

        head.visible = helmet;
        for (String detail : new String[]{"crown_upper_band", "crown_lower_band", "nasal_crest", "left_cheek_rim", "right_cheek_rim", "back_brow_stud", "headband_stud_left", "headband_stud_right"}) head.getChild(detail).visible = helmet;
        hat.visible = false; // Never overlay an extra vanilla hat on the helmet.
        for (String part : new String[]{"nasal_guard", "brow", "helm_cap", "left_temple", "right_temple", "back_rim", "helm_ridge", "brow_emblem", "back_strap"}) {
            head.getChild(part).visible = helmet;
        }

        body.visible = chest;
        for (String detail : new String[]{"left_chest_strap", "right_chest_strap", "left_armor_rail", "right_armor_rail", "chest_heart_plate", "belt_lower_trim", "left_mail_tab", "right_mail_tab", "back_horizontal_strap"}) body.getChild(detail).visible = chest;
        for (String part : new String[]{"mail_front", "left_leather_edge", "right_leather_edge", "upper_chest_strap", "belt", "belt_buckle", "back_plate", "fur_collar", "cross_chest_plate", "chest_buckle"}) {
            body.getChild(part).visible = chest;
        }
        leftArm.visible = chest;
        rightArm.visible = chest;
        leftArm.getChild("pauldron").visible = chest;
        leftArm.getChild("pauldron_rim").visible = chest;
        leftArm.getChild("pauldron_top_plate").visible = chest;
        leftArm.getChild("upper_arm_plate").visible = chest;
        leftArm.getChild("arm_bracer").visible = chest;
        rightArm.getChild("pauldron").visible = chest;
        rightArm.getChild("pauldron_rim").visible = chest;
        rightArm.getChild("pauldron_top_plate").visible = chest;
        rightArm.getChild("upper_arm_plate").visible = chest;
        rightArm.getChild("arm_bracer").visible = chest;

        leftLeg.visible = legs || boots;
        rightLeg.visible = legs || boots;
        for (ModelPart leg : new ModelPart[]{leftLeg, rightLeg}) {
            leg.getChild("knee_plate").visible = legs;
            leg.getChild("boot_toe").visible = boots;
            leg.getChild("greave_front").visible = legs;
            leg.getChild("shin_band").visible = legs;
        }
    }
}
