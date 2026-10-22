package com.kirill.pocketrealms.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

/** Three truly distinct 3D silhouettes: broad quadruped bruiser, ceiling crab, choir spire. */
public final class DepthsModel<T extends LivingEntity> extends EntityModel<T> {
    public static final ModelLayerLocation KNOCKER=new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath("pocketrealms","knocker"),"main");
    public static final ModelLayerLocation LURKER=new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath("pocketrealms","ceiling_lurker"),"main");
    public static final ModelLayerLocation CHOIR=new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath("pocketrealms","choir_wraith"),"main");
    private final ModelPart root;
    private final ModelPart body;
    public DepthsModel(ModelPart root){this.root=root;this.body=root.getChild("body");}
    @Override public void setupAnim(T mob,float limbSwing,float limbSwingAmount,float age,float yaw,float pitch){
        body.yRot=(float)Math.sin(age*.075)*.07F;
        body.zRot=(float)Math.sin(age*.12)*.045F;
    }
    @Override public void renderToBuffer(PoseStack pose,VertexConsumer buffer,int light,int overlay,int color){
        root.render(pose,buffer,light,overlay,color);
    }
    public static LayerDefinition knocker(){
        MeshDefinition mesh=new MeshDefinition();
        PartDefinition root=mesh.getRoot();
        PartDefinition body=root.addOrReplaceChild("body",CubeListBuilder.create().texOffs(0,0)
                .addBox(-9,-22,-7,18,19,14),PartPose.offset(0,24,0));
        body.addOrReplaceChild("cranium",CubeListBuilder.create().texOffs(0,35)
                .addBox(-6,-29,-10,12,9,8),PartPose.ZERO);
        body.addOrReplaceChild("jaw",CubeListBuilder.create().texOffs(40,35)
                .addBox(-5,-21,-12,10,5,4),PartPose.ZERO);
        for(int side:new int[]{-1,1}){
            body.addOrReplaceChild("hammer"+side,CubeListBuilder.create().texOffs(60,0)
                    .addBox(side<0?-16:10,-18,-6,6,16,12),PartPose.ZERO);
            body.addOrReplaceChild("leg"+side,CubeListBuilder.create().texOffs(0,54)
                    .addBox(side<0?-8:3,-5,-4,5,6,9),PartPose.ZERO);
            body.addOrReplaceChild("horn"+side,CubeListBuilder.create().texOffs(64,36)
                    .addBox(side<0?-7:5,-36,-4,2,10,3),PartPose.ZERO);
        }
        return LayerDefinition.create(mesh,128,128);
    }
    public static LayerDefinition lurker(){
        MeshDefinition mesh=new MeshDefinition();
        PartDefinition root=mesh.getRoot();
        PartDefinition body=root.addOrReplaceChild("body",CubeListBuilder.create().texOffs(0,0)
                .addBox(-8,-3,-8,16,6,16),PartPose.offset(0,6,0));
        body.addOrReplaceChild("eyeplate",CubeListBuilder.create().texOffs(0,27)
                .addBox(-6,0,-11,12,3,5),PartPose.ZERO);
        body.addOrReplaceChild("spine",CubeListBuilder.create().texOffs(48,0)
                .addBox(-3,-7,-3,6,6,6),PartPose.ZERO);
        for(int i=0;i<4;i++)for(int side:new int[]{-1,1}){
            body.addOrReplaceChild("talon"+side+"_"+i,CubeListBuilder.create().texOffs(0,38)
                    .addBox(side<0?-15:8,-2,-7+i*4,7,4,2),PartPose.ZERO);
        }
        return LayerDefinition.create(mesh,128,128);
    }
    public static LayerDefinition choir(){
        MeshDefinition mesh=new MeshDefinition();
        PartDefinition root=mesh.getRoot();
        PartDefinition body=root.addOrReplaceChild("body",CubeListBuilder.create().texOffs(0,0)
                .addBox(-5,-34,-4,10,32,8),PartPose.offset(0,24,0));
        body.addOrReplaceChild("mask",CubeListBuilder.create().texOffs(36,0)
                .addBox(-4,-42,-5,8,9,8),PartPose.ZERO);
        body.addOrReplaceChild("mouth",CubeListBuilder.create().texOffs(0,45)
                .addBox(-2,-37,-7,4,7,3),PartPose.ZERO);
        for(int i=0;i<3;i++){
            body.addOrReplaceChild("rib"+i,CubeListBuilder.create().texOffs(40,20)
                    .addBox(-7,-27+i*6,-5,14,2,10),PartPose.ZERO);
        }
        for(int side:new int[]{-1,1}){
            body.addOrReplaceChild("hanging_arm"+side,CubeListBuilder.create().texOffs(70,0)
                    .addBox(side<0?-10:6,-26,-3,4,26,5),PartPose.ZERO);
        }
        return LayerDefinition.create(mesh,128,128);
    }
}
