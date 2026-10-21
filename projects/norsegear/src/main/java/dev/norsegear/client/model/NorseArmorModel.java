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
        head.addOrReplaceChild("helm_cap", CubeListBuilder.create().texOffs(0, 0)
            .addBox(-4.25F, -8.5F, -4.25F, 8.5F, 2.0F, 8.5F),
            net.minecraft.client.model.geom.PartPose.ZERO);
        head.addOrReplaceChild("brow", CubeListBuilder.create().texOffs(0, 16)
            .addBox(-4.5F, -6.6F, -4.9F, 9.0F, 1.25F, 1.05F),
            net.minecraft.client.model.geom.PartPose.ZERO);
        head.addOrReplaceChild("nasal_guard", CubeListBuilder.create().texOffs(30, 16)
            .addBox(-0.62F, -5.8F, -5.2F, 1.24F, 4.2F, 1.1F),
            net.minecraft.client.model.geom.PartPose.ZERO);
        head.addOrReplaceChild("left_temple", CubeListBuilder.create().texOffs(30, 16)
            .addBox(3.65F, -6.25F, -3.7F, 0.9F, 4.25F, 6.8F),
            net.minecraft.client.model.geom.PartPose.ZERO);
        head.addOrReplaceChild("right_temple", CubeListBuilder.create().texOffs(30, 16)
            .addBox(-4.55F, -6.25F, -3.7F, 0.9F, 4.25F, 6.8F),
            net.minecraft.client.model.geom.PartPose.ZERO);
        head.addOrReplaceChild("back_rim", CubeListBuilder.create().texOffs(16, 16)
            .addBox(-3.75F, -6.3F, 3.55F, 7.5F, 2.6F, 1.0F),
            net.minecraft.client.model.geom.PartPose.ZERO);

        // Torso is intentionally not an opaque vanilla armor box. The open
        // chainmail weave in mail_front must show the underlying player skin.
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create(),
            net.minecraft.client.model.geom.PartPose.ZERO);
        body.addOrReplaceChild("mail_front", CubeListBuilder.create().texOffs(43, 0)
            .addBox(-3.65F, 1.4F, -3.18F, 7.3F, 7.0F, 0.35F),
            net.minecraft.client.model.geom.PartPose.ZERO);
        body.addOrReplaceChild("left_leather_edge", CubeListBuilder.create().texOffs(16, 16)
            .addBox(3.32F, 1.6F, -3.45F, 1.1F, 6.8F, 1.0F),
            net.minecraft.client.model.geom.PartPose.ZERO);
        body.addOrReplaceChild("right_leather_edge", CubeListBuilder.create().texOffs(16, 16)
            .addBox(-4.42F, 1.6F, -3.45F, 1.1F, 6.8F, 1.0F),
            net.minecraft.client.model.geom.PartPose.ZERO);
        body.addOrReplaceChild("upper_chest_strap", CubeListBuilder.create().texOffs(16, 16)
            .addBox(-4.4F, 1.0F, -3.68F, 8.8F, 1.15F, 1.25F),
            net.minecraft.client.model.geom.PartPose.ZERO);
        body.addOrReplaceChild("belt", CubeListBuilder.create().texOffs(16, 16)
            .addBox(-4.4F, 8.15F, -3.12F, 8.8F, 2.0F, 6.24F),
            net.minecraft.client.model.geom.PartPose.ZERO);
        body.addOrReplaceChild("belt_buckle", CubeListBuilder.create().texOffs(30, 16)
            .addBox(-1.1F, 8.1F, -3.9F, 2.2F, 2.1F, 0.95F),
            net.minecraft.client.model.geom.PartPose.ZERO);
        body.addOrReplaceChild("back_plate", CubeListBuilder.create().texOffs(16, 16)
            .addBox(-3.65F, 1.3F, 2.85F, 7.3F, 6.75F, 0.6F),
            net.minecraft.client.model.geom.PartPose.ZERO);

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
        hat.visible = false; // Never overlay an extra vanilla hat on the helmet.
        for (String part : new String[]{"nasal_guard", "brow", "helm_cap", "left_temple", "right_temple", "back_rim"}) {
            head.getChild(part).visible = helmet;
        }

        body.visible = chest;
        for (String part : new String[]{"mail_front", "left_leather_edge", "right_leather_edge", "upper_chest_strap", "belt", "belt_buckle", "back_plate"}) {
            body.getChild(part).visible = chest;
        }
        leftArm.visible = chest;
        rightArm.visible = chest;
        leftArm.getChild("pauldron").visible = chest;
        rightArm.getChild("pauldron").visible = chest;

        leftLeg.visible = legs || boots;
        rightLeg.visible = legs || boots;
        for (ModelPart leg : new ModelPart[]{leftLeg, rightLeg}) {
            leg.getChild("knee_plate").visible = legs;
            leg.getChild("boot_toe").visible = boots;
        }
    }
}
