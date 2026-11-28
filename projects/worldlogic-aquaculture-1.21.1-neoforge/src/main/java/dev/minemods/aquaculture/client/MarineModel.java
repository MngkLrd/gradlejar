package dev.minemods.aquaculture.client;
import dev.minemods.aquaculture.*;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import java.util.*;

final class MarineModel extends HierarchicalModel<MarineAnimal>{
 private final ModelPart root;
 private final Map<String,ModelPart> parts=new HashMap<>();
 private final MarineSpecies species;
 MarineModel(ModelPart root,MarineSpecies s){
  this.root=root;species=s;parts.put("root",root);
  for(var p:MarineGeometry.ALL.get(s.id).parts())parts.put(p.name(),parts.get(p.parent()).getChild(p.name()));
 }
 static LayerDefinition layer(MarineSpecies s){
  MeshDefinition mesh=new MeshDefinition();Map<String,PartDefinition> nodes=new HashMap<>();nodes.put("root",mesh.getRoot());
  for(var p:MarineGeometry.ALL.get(s.id).parts()){
   var cube=CubeListBuilder.create().texOffs(p.u(),p.v()).addBox(p.x(),p.y(),p.z(),p.w(),p.h(),p.d());
   nodes.put(p.name(),nodes.get(p.parent()).addOrReplaceChild(p.name(),cube,PartPose.offset(p.ox(),p.oy(),p.oz())));
  }
  return LayerDefinition.create(mesh,128,128);
 }
 @Override public ModelPart root(){return root;}
 @Override public void setupAnim(MarineAnimal e,float stride,float amount,float age,float yaw,float pitch){
  root.getAllParts().forEach(ModelPart::resetPose);
  float activity=Math.max(.15f,Math.min(1,(float)e.getDeltaMovement().length()*5));
  float wave=(float)Math.sin(age*(species.whale()?.12f:species.ray()?.23f:species.shape.equals("snake")?.39f:.28f));
  float late=(float)Math.sin(age*(species.whale()?.12f:.28f)-.8f);
  if(species.whale()){
   parts.get("rear").xRot+=wave*.12f*activity;parts.get("tail").xRot+=late*.22f*activity;
   part("left_fin",wave*.08f);part("right_fin",-wave*.08f);
   parts.get("body").xRot+=(float)Math.max(-.25,Math.min(.25,-e.getDeltaMovement().y));
  }else if(species.ray()){
   part("left_fin",wave*.40f*activity);part("right_fin",-wave*.40f*activity);
   part("left_tip",late*.32f*activity);part("right_tip",-late*.32f*activity);
   parts.get("tail").yRot+=late*.10f;
  }else{
   float a=species.shape.equals("snake")?.55f:.28f;
   parts.get("body").yRot+=wave*a*.16f*activity;
   parts.get("head").yRot-=wave*a*.18f*activity;
   parts.get("rear").yRot+=wave*a*.6f*activity;parts.get("tail").yRot+=late*a*activity;
   if(parts.containsKey("tail_tip"))parts.get("tail_tip").yRot+=(float)Math.sin(age*.39f-1.6f)*a;
  }
  if(!e.isInWater())parts.get("body").zRot+=.25f*wave;
 }
 private void part(String name,float angle){var p=parts.get(name);if(p!=null)p.zRot+=angle;}
}
