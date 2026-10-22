package com.kirill.pocketrealms.client;

import com.kirill.pocketrealms.entity.CorruptedZombie;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.ZombieModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;

/** Broken jaw, bone barbs, exposed breastbone: geometry, not a reskinned vanilla mob. */
public final class HorrorZombieModel extends ZombieModel<CorruptedZombie> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath("pocketrealms","flayed_zombie"),"main");

    public HorrorZombieModel(ModelPart root) {super(root);}

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh=HumanoidModel.createMesh(CubeDeformation.NONE,0.0F);
        PartDefinition head=mesh.getRoot().getChild("head");
        PartDefinition chest=mesh.getRoot().getChild("body");
        PartDefinition leftArm=mesh.getRoot().getChild("left_arm");
        head.addOrReplaceChild("split_jaw",CubeListBuilder.create().texOffs(32,0)
                .addBox(-3.0F,-1.0F,-5.0F,6.0F,2.0F,3.0F),PartPose.ZERO);
        head.addOrReplaceChild("brow_spike",CubeListBuilder.create().texOffs(32,6)
                .addBox(2.0F,-11.0F,-1.0F,2.0F,5.0F,2.0F),PartPose.ZERO);
        chest.addOrReplaceChild("sternum",CubeListBuilder.create().texOffs(40,16)
                .addBox(-1.0F,1.0F,-5.0F,2.0F,10.0F,2.0F),PartPose.ZERO);
        chest.addOrReplaceChild("rib_left",CubeListBuilder.create().texOffs(44,30)
                .addBox(-5.0F,3.0F,-5.0F,4.0F,2.0F,2.0F),PartPose.ZERO);
        chest.addOrReplaceChild("rib_right",CubeListBuilder.create().texOffs(44,36)
                .addBox(1.0F,7.0F,-5.0F,4.0F,2.0F,2.0F),PartPose.ZERO);
        leftArm.addOrReplaceChild("elbow_bone",CubeListBuilder.create().texOffs(48,42)
                .addBox(1.0F,6.0F,-3.0F,2.0F,7.0F,2.0F),PartPose.ZERO);
        return LayerDefinition.create(mesh,64,64);
    }
}
