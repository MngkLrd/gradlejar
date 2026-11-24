package dev.ataflore.huss.client;

import dev.ataflore.huss.HussMod;
import dev.ataflore.huss.training.Training;
import net.minecraft.client.model.VillagerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.npc.Villager;

public final class TrainingModel extends VillagerModel<Villager> {
    private final ModelPart head,body,leftLeg,rightLeg,leftArm,rightArm,leftFore,rightFore;
    public TrainingModel(ModelPart root) {
        super(root);
        head=root.getChild("head");body=root.getChild("body");
        leftLeg=root.getChild("left_leg");rightLeg=root.getChild("right_leg");
        leftArm=root.getChild("huss_left_arm");rightArm=root.getChild("huss_right_arm");
        leftFore=leftArm.getChild("forearm");rightFore=rightArm.getChild("forearm");
    }
    public static LayerDefinition layer() {
        MeshDefinition mesh=VillagerModel.createBodyModel();
        PartDefinition root=mesh.getRoot();
        PartDefinition right=root.addOrReplaceChild("huss_right_arm",
            CubeListBuilder.create().texOffs(44,22).addBox(-3.5f,-2,-2,4,6,4),PartPose.offset(-4,2,0));
        right.addOrReplaceChild("forearm",CubeListBuilder.create().texOffs(44,28).addBox(-3.5f,0,-2,4,5,4),PartPose.offset(0,4,0));
        PartDefinition left=root.addOrReplaceChild("huss_left_arm",
            CubeListBuilder.create().texOffs(44,22).mirror().addBox(-.5f,-2,-2,4,6,4),PartPose.offset(4,2,0));
        left.addOrReplaceChild("forearm",CubeListBuilder.create().texOffs(44,28).mirror().addBox(-.5f,0,-2,4,5,4),PartPose.offset(0,4,0));
        return LayerDefinition.create(mesh,64,64);
    }
    private static float hit(float t,float center,float width) {
        float x=Mth.clamp(1-Math.abs(t-center)/width,0,1);
        return x*x*(3-2*x);
    }
    @Override public void setupAnim(Villager v,float swing,float amount,float age,float yaw,float pitch) {
        root().getAllParts().forEach(ModelPart::resetPose);
        super.setupAnim(v,swing,amount,age,yaw,pitch);
        root().getChild("arms").visible=false;
        MobEffectInstance effect=v.getEffect(HussMod.trainingEffect());
        float partial=age-v.tickCount;
        float elapsed=effect==null?0:Math.max(0,Training.DURATION+2-effect.getDuration()+partial);
        float t=elapsed%Training.CYCLE;
        float a=Math.max(hit(t,2,2.5f),hit(t,21,3)),b=Math.max(hit(t,6,2.5f),hit(t,25,3)),c=Math.max(hit(t,10,3),hit(t,29,3));
        float beat=Math.max(a,Math.max(b,c));
        float crouch=hit(t,0,2)+hit(t,16,5)+hit(t,34,2);
        float settle=hit(t,33,3);
        float sway=.07f*Mth.sin(t*.35f);
        float mode=(int)(elapsed/Training.CYCLE)%3;
        // Boxing, double-arm flex, then high knee power punches.
        body.xRot=.12f+.19f*crouch-.14f*beat;
        body.yRot=.40f*(b-a);body.zRot=.12f*(a-b);
        head.xRot=-.10f+.32f*crouch-.24f*beat;
        head.yRot=-.22f*(b-a);head.zRot=.13f*(a-b);
        root().y=1.6f*crouch-.6f*c;
        root().xScale=1+.045f*beat;root().zScale=1+.045f*beat;root().yScale=1-.035f*crouch+.045f*c;
        rightArm.xRot=-.85f-1.15f*a-.82f*c;
        leftArm.xRot=-.85f-1.15f*b-.82f*c;
        rightArm.zRot=.26f+.30f*c;leftArm.zRot=-.26f-.30f*c;
        rightArm.yRot=.12f+.25f*a;leftArm.yRot=-.12f-.25f*b;
        rightFore.xRot=-1.15f+.98f*a+.64f*c;leftFore.xRot=-1.15f+.98f*b+.64f*c;
        rightLeg.xRot=-.33f*crouch+.28f*a-.48f*c;
        leftLeg.xRot=.33f*crouch-.28f*b+.32f*c;
        rightLeg.zRot=.09f;leftLeg.zRot=-.09f;
        if(mode==1) {
            rightArm.xRot=-.9f-.85f*beat;leftArm.xRot=rightArm.xRot;
            rightArm.zRot=.35f+1.0f*beat;leftArm.zRot=-rightArm.zRot;
            rightFore.xRot=-1.5f;leftFore.xRot=-1.5f;
            body.yRot=0;head.yRot=0;
        } else if(mode==2) {
            rightLeg.xRot=-1.1f*a+.25f*b;leftLeg.xRot=-1.1f*b+.25f*a;
            rightArm.xRot=-.65f-1.0f*b-1.0f*c;leftArm.xRot=-.65f-1.0f*a-1.0f*c;
            rightFore.xRot=-.9f;leftFore.xRot=-.9f;
        }
        rightArm.zRot+=sway;leftArm.zRot+=sway;
        head.xRot+=.2f*settle;
    }
}
