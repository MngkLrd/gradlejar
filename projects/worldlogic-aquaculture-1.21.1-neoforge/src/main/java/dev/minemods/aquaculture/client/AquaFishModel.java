package dev.minemods.aquaculture.client;
import dev.minemods.aquaculture.*;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.world.entity.animal.Cod;
import java.util.*;

final class AquaFishModel extends HierarchicalModel<Cod>{
 private final ModelPart root;
 private final FishVisualGeometry.Shape shape;
 private final Map<String,ModelPart> parts=new HashMap<>();
 AquaFishModel(ModelPart root,FishVisualGeometry.Shape shape){
  this.root=root;this.shape=shape;parts.put("root",root);
  for(var p:shape.parts())parts.put(p.name(),parts.get(p.parent()).getChild(p.name()));
 }
 static LayerDefinition layer(FishVisualGeometry.Shape shape){
  MeshDefinition mesh=new MeshDefinition();Map<String,PartDefinition> nodes=new HashMap<>();nodes.put("root",mesh.getRoot());
  for(var p:shape.parts()){
   var cube=CubeListBuilder.create().texOffs(p.u(),p.v()).addBox(p.bx(),p.by(),p.bz(),p.w(),p.h(),p.d());
   nodes.put(p.name(),nodes.get(p.parent()).addOrReplaceChild(p.name(),cube,PartPose.offsetAndRotation(p.ox(),p.oy(),p.oz(),p.rx(),p.ry(),p.rz())));
  }
  return LayerDefinition.create(mesh,64,64);
 }
 @Override public ModelPart root(){return root;}
 @Override public void setupAnim(Cod fish,float stride,float activity,float age,float yaw,float pitch){
  root.getAllParts().forEach(ModelPart::resetPose);
  boolean wet=fish.isInWaterOrBubble()||FishWater.contact(fish);
  float speed=Math.max(activity,Math.min(1,(float)fish.getDeltaMovement().horizontalDistance()*6));
  var pose=FishMotion.sample(shape.motion(),age,stride,speed,(float)fish.getDeltaMovement().y,wet,fish.hurtTime>0,fish.getId()*.37f);
  var body=parts.get("body");body.yRot+=pose.body();body.zRot+=pose.roll();body.xRot+=pose.pitch();body.y+=pose.lift();
  parts.get("head").yRot+=pose.head();parts.get("rear").yRot+=pose.rear();parts.get("tail").yRot+=pose.tail();
  rotateFin("left_fin",pose.fins());rotateFin("right_fin",-pose.fins());
  if(parts.containsKey("dorsal"))parts.get("dorsal").zRot+=pose.fins()*.15f;
  if(parts.containsKey("anal"))parts.get("anal").zRot-=pose.fins()*.18f;
 }
 private void rotateFin(String name,float roll){var part=parts.get(name);if(part!=null)part.zRot+=roll;}
}
