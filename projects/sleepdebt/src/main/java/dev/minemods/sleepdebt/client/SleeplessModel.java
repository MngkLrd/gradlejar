package dev.minemods.sleepdebt.client;

import dev.minemods.sleepdebt.SleepDebtMod;
import dev.minemods.sleepdebt.entity.SleeplessEntity;
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
import net.minecraft.util.Mth;

/**
 * Original anatomical nightmare, deliberately unlike a vanilla player.
 * An asymmetrical skull, hanging split mandible, exposed rib struts, broken
 * joints and long gripping fingers create a recognisable silhouette at night.
 */
public final class SleeplessModel extends HumanoidModel<SleeplessEntity> {
 public static final ModelLayerLocation LAYER = new ModelLayerLocation(
   ResourceLocation.fromNamespaceAndPath(SleepDebtMod.MOD_ID,"sleepless"),"main");

 private final ModelPart jaw;
 private final ModelPart leftRibs;
 private final ModelPart rightRibs;
 private final ModelPart leftClaws;
 private final ModelPart rightClaws;
 private final ModelPart backSpine;

 public SleeplessModel(ModelPart root) {
  super(root);
  jaw=head.getChild("mandible");
  leftRibs=body.getChild("left_ribs");
  rightRibs=body.getChild("right_ribs");
  backSpine=body.getChild("back_spine");
  leftClaws=leftArm().getChild("claws");
  rightClaws=rightArm().getChild("claws");
 }
 private ModelPart leftArm(){return leftArm;}
 private ModelPart rightArm(){return rightArm;}

 public static LayerDefinition createLayer(){
  MeshDefinition mesh=HumanoidModel.createMesh(CubeDeformation.NONE,0.0f);
  PartDefinition root=mesh.getRoot();

  PartDefinition skull=root.addOrReplaceChild("head",CubeListBuilder.create()
    .texOffs(0,0).addBox(-5.0f,-14.0f,-5.0f,10.0f,15.0f,10.0f),
    PartPose.offset(0f,-3f,0f));
  root.addOrReplaceChild("hat",CubeListBuilder.create(),PartPose.ZERO);
  // The face hangs open; its false second mouth twitches independently.
  skull.addOrReplaceChild("mandible",CubeListBuilder.create()
    .texOffs(112,0).addBox(-4.3f,0f,-1.0f,8.6f,6.5f,2f),
    PartPose.offsetAndRotation(0f,-1f,-5.2f,.08f,0f,0f));
  skull.addOrReplaceChild("throat",CubeListBuilder.create()
    .texOffs(142,0).addBox(-1.2f,0f,-1f,2.4f,8f,2f),
    PartPose.offsetAndRotation(0f,0f,2.5f,-.22f,0f,0f));
  skull.addOrReplaceChild("crown_left",CubeListBuilder.create()
    .texOffs(158,0).addBox(-.8f,-7f,-.7f,1.6f,8f,1.4f),
    PartPose.offsetAndRotation(-3.0f,-11f,1f,-.3f,.22f,-.4f));
  skull.addOrReplaceChild("crown_right",CubeListBuilder.create()
    .texOffs(173,0).addBox(-.8f,-9f,-.7f,1.6f,10f,1.4f),
    PartPose.offsetAndRotation(3f,-12f,1f,.4f,-.2f,.52f));

  PartDefinition chest=root.addOrReplaceChild("body",CubeListBuilder.create()
    .texOffs(0,50).addBox(-3.3f,-1.7f,-2.4f,6.6f,15.3f,4.8f),
    PartPose.offsetAndRotation(0f,0f,0f,-.10f,0f,.085f));
  chest.addOrReplaceChild("left_ribs",CubeListBuilder.create()
    .texOffs(70,73).addBox(-.9f,-1f,-1.2f,1.8f,11f,2.2f),
    PartPose.offsetAndRotation(3f,0f,-1f,.22f,-.16f,-.25f));
  chest.addOrReplaceChild("right_ribs",CubeListBuilder.create()
    .texOffs(82,73).addBox(-.9f,-1f,-1.2f,1.8f,10f,2.3f),
    PartPose.offsetAndRotation(-3f,1f,-1f,-.20f,.10f,.26f));
  chest.addOrReplaceChild("back_spine",CubeListBuilder.create()
    .texOffs(96,72).addBox(-1.0f,-3f,-.8f,2.0f,20f,1.6f),
    PartPose.offsetAndRotation(0f,-1f,2.7f,.27f,0f,-.1f));
  chest.addOrReplaceChild("open_sternum",CubeListBuilder.create()
    .texOffs(114,54).addBox(-1.3f,1f,-.5f,2.6f,12f,1f),
    PartPose.offsetAndRotation(.1f,1f,-2.5f,0f,0f,.18f));
  chest.addOrReplaceChild("shoulder_bone",CubeListBuilder.create()
    .texOffs(142,51).addBox(-6f,-.8f,-.7f,12f,1.6f,1.4f),
    PartPose.offsetAndRotation(0f,-1.7f,0f,-.1f,0f,-.12f));

  PartDefinition right=root.addOrReplaceChild("right_arm",CubeListBuilder.create()
    .texOffs(58,0).addBox(-1.7f,-2f,-1.6f,3.4f,20f,3.2f),
    PartPose.offsetAndRotation(-5.4f,0f,0f,-.18f,0f,.23f));
  PartDefinition left=root.addOrReplaceChild("left_arm",CubeListBuilder.create()
    .texOffs(82,0).addBox(-1.6f,-2f,-1.5f,3.1f,18f,3.1f),
    PartPose.offsetAndRotation(5.2f,1.5f,0f,.20f,0f,-.16f));

  right.addOrReplaceChild("elbow",CubeListBuilder.create()
    .texOffs(146,51).addBox(-1f,-.8f,-.8f,2f,9f,2f),
    PartPose.offsetAndRotation(0f,10f,-.5f,-.45f,0f,-.2f));
  left.addOrReplaceChild("elbow",CubeListBuilder.create()
    .texOffs(157,51).addBox(-.9f,-.8f,-.8f,1.8f,10f,1.8f),
    PartPose.offsetAndRotation(0f,8f,-.7f,-.60f,0f,.16f));

  PartDefinition rc=right.addOrReplaceChild("claws",CubeListBuilder.create(),
    PartPose.offset(0f,20f,-1f));
  PartDefinition lc=left.addOrReplaceChild("claws",CubeListBuilder.create(),
    PartPose.offset(0f,18f,-1f));
  for(int i=0;i<4;i++){
   float xx=-1.6f+i*1.05f;
   rc.addOrReplaceChild("finger_"+i,CubeListBuilder.create()
    .texOffs(182+i*6,0).addBox(-.45f,0f,-.45f,.9f,7.5f+(i%2)*2f,.85f),
    PartPose.offsetAndRotation(xx,0f,0f,.22f+(i%2)*.13f,0f,(i-1.5f)*.16f));
   lc.addOrReplaceChild("finger_"+i,CubeListBuilder.create()
    .texOffs(182+i*6,20).addBox(-.40f,0f,-.45f,.85f,6.5f+(i%2)*2f,.85f),
    PartPose.offsetAndRotation(xx,0f,0f,.30f+(i%2)*.13f,0f,(i-1.5f)*.17f));
  }

  PartDefinition rleg=root.addOrReplaceChild("right_leg",CubeListBuilder.create()
    .texOffs(48,52).addBox(-1.5f,0f,-1.5f,3f,14f,3f),
    PartPose.offsetAndRotation(-2.1f,12f,0f,.08f,0f,.12f));
  PartDefinition lleg=root.addOrReplaceChild("left_leg",CubeListBuilder.create()
    .texOffs(70,52).addBox(-1.5f,0f,-1.5f,3f,14f,3f),
    PartPose.offsetAndRotation(2.0f,12f,0f,-.09f,0f,-.11f));
  rleg.addOrReplaceChild("reversed_joint",CubeListBuilder.create()
    .texOffs(215,0).addBox(-1.1f,-1.0f,-1.3f,2.2f,7f,2.4f),
    PartPose.offsetAndRotation(0f,9f,1.4f,-.54f,0f,0f));
  lleg.addOrReplaceChild("reversed_joint",CubeListBuilder.create()
    .texOffs(229,0).addBox(-1.1f,-1.0f,-1.3f,2.2f,6f,2.4f),
    PartPose.offsetAndRotation(0f,9f,1.2f,-.50f,0f,0f));
  return LayerDefinition.create(mesh,256,128);
 }
 @Override
 public void setupAnim(SleeplessEntity mob,float limbSwing,float limbSwingAmount,
   float ageInTicks,float netHeadYaw,float headPitch){
  super.setupAnim(mob,limbSwing,limbSwingAmount,ageInTicks,netHeadYaw,headPitch);
  // Deliberately inhuman: breathing is not synchronized with locomotion.
  float breath=Mth.sin(ageInTicks*.086f);
  float stutter=Mth.sin(ageInTicks*1.48f)*Mth.sin(ageInTicks*.147f);
  head.zRot=.18f+breath*.09f+stutter*.035f;
  head.xRot+=.12f+Mth.sin(ageInTicks*.042f)*.08f;
  body.zRot=.06f+breath*.04f;
  body.xRot=-.12f+breath*.055f;
  jaw.xRot=.15f+Math.max(0,Mth.sin(ageInTicks*.128f))*.22f;
  leftRibs.zRot=-.22f+breath*.06f;
  rightRibs.zRot=.25f-breath*.06f;
  backSpine.xRot=.12f+breath*.03f;
  // Crawling fingers continue to flex even when the creature stands still.
  leftClaws.xRot=.30f+Mth.sin(ageInTicks*.17f)*.18f;
  rightClaws.xRot=.16f+Mth.sin(ageInTicks*.14f+1.8f)*.24f;
  rightArm.xRot=rightArm.xRot*.72f-.14f;
  leftArm.xRot=leftArm.xRot*.80f+.18f;
 }
}
