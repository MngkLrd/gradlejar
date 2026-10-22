package dev.minemods.sleepdebt.client;
import dev.minemods.sleepdebt.SleepDebtMod;
import dev.minemods.sleepdebt.entity.SleeplessEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.resources.ResourceLocation;

/** Non-standard slender humanoid with hood, elongated arms and narrow chest. */
public final class SleeplessModel extends HumanoidModel<SleeplessEntity> {
 public static final ModelLayerLocation LAYER=new ModelLayerLocation(
  ResourceLocation.fromNamespaceAndPath(SleepDebtMod.MOD_ID,"sleepless"),"main");
 public SleeplessModel(ModelPart root){super(root);}
 public static LayerDefinition createLayer(){
  MeshDefinition mesh=HumanoidModel.createMesh(CubeDeformation.NONE,0.0f);
  PartDefinition root=mesh.getRoot();
  root.addOrReplaceChild("head",CubeListBuilder.create().texOffs(0,0)
    .addBox(-4f,-10f,-4f,8f,11f,8f),PartPose.ZERO);
  root.addOrReplaceChild("hat",CubeListBuilder.create().texOffs(32,0)
    .addBox(-4.5f,-10.5f,-4.5f,9f,12f,9f),PartPose.ZERO);
  root.addOrReplaceChild("body",CubeListBuilder.create().texOffs(16,16)
    .addBox(-3.5f,0f,-2f,7f,13f,4f),PartPose.ZERO);
  root.addOrReplaceChild("right_arm",CubeListBuilder.create().texOffs(40,16)
    .addBox(-1.5f,-2f,-1.5f,3f,16f,3f),PartPose.offset(-4.5f,2f,0f));
  root.addOrReplaceChild("left_arm",CubeListBuilder.create().texOffs(40,36)
    .addBox(-1.5f,-2f,-1.5f,3f,16f,3f),PartPose.offset(4.5f,2f,0f));
  root.addOrReplaceChild("right_leg",CubeListBuilder.create().texOffs(0,20)
    .addBox(-1.5f,0f,-1.5f,3f,12f,3f),PartPose.offset(-1.9f,12f,0f));
  root.addOrReplaceChild("left_leg",CubeListBuilder.create().texOffs(14,40)
    .addBox(-1.5f,0f,-1.5f,3f,12f,3f),PartPose.offset(1.9f,12f,0f));
  return LayerDefinition.create(mesh,64,64);
 }
}

