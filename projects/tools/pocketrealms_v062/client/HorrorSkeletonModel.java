package com.kirill.pocketrealms.client;

import com.kirill.pocketrealms.entity.CorruptedSkeleton;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.SkeletonModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;

/** Skeletal horror: antler-like growth and protruding rib cage. */
public final class HorrorSkeletonModel extends SkeletonModel<CorruptedSkeleton> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath("pocketrealms","shattered_skeleton"),"main");

    public HorrorSkeletonModel(ModelPart root) {super(root);}

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh=HumanoidModel.createMesh(CubeDeformation.NONE,0.0F);
        PartDefinition head=mesh.getRoot().getChild("head");
        PartDefinition chest=mesh.getRoot().getChild("body");
        head.addOrReplaceChild("antler_left",CubeListBuilder.create().texOffs(32,0)
                .addBox(-5.0F,-13.0F,-1.0F,2.0F,7.0F,2.0F),PartPose.ZERO);
        head.addOrReplaceChild("antler_right",CubeListBuilder.create().texOffs(40,0)
                .addBox(3.0F,-14.0F,-1.0F,2.0F,8.0F,2.0F),PartPose.ZERO);
        head.addOrReplaceChild("open_maw",CubeListBuilder.create().texOffs(48,0)
                .addBox(-3.0F,-1.0F,-5.0F,6.0F,3.0F,2.0F),PartPose.ZERO);
        chest.addOrReplaceChild("left_ribs",CubeListBuilder.create().texOffs(32,16)
                .addBox(-6.0F,3.0F,-5.0F,5.0F,3.0F,2.0F),PartPose.ZERO);
        chest.addOrReplaceChild("right_ribs",CubeListBuilder.create().texOffs(44,16)
                .addBox(1.0F,7.0F,-5.0F,5.0F,3.0F,2.0F),PartPose.ZERO);
        chest.addOrReplaceChild("back_spine",CubeListBuilder.create().texOffs(36,26)
                .addBox(-1.0F,-2.0F,2.0F,2.0F,15.0F,3.0F),PartPose.ZERO);
        return LayerDefinition.create(mesh,64,64);
    }
}
