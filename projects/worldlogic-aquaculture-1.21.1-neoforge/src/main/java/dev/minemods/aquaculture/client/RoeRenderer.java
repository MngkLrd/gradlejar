package dev.minemods.aquaculture.client;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.minemods.aquaculture.*;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import java.util.*;

final class RoeRenderer extends EntityRenderer<RoeClutch>{
 static final List<String> STYLES=List.of("gravel","vegetation","open_water","sheltered_bed");
 private static final int[][][] SHAPES={
  {{-5,0,-4},{-2,0,-5},{1,0,-3},{-4,0,0},{-1,1,0},{2,0,1},{-1,0,3}},
  {{-1,3,-4},{-4,1,-1},{-1,2,-1},{2,1,0},{-2,0,2},{1,2,3}},
  {{-6,1,-3},{-1,4,-5},{3,0,-2},{-3,-2,2},{2,2,3}},
  {{-6,0,-4},{-3,0,-5},{0,1,-4},{3,0,-3},{-5,0,-1},{-2,2,-1},{1,1,0},{-2,0,3}}
 };
 static ModelLayerLocation location(String s){return new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(Aquaculture.ID,"roe/"+s),"main");}
 static LayerDefinition layer(String s){
  MeshDefinition mesh=new MeshDefinition();var root=mesh.getRoot();
  int[][] offsets=SHAPES[STYLES.indexOf(s)];
  for(int i=0;i<offsets.length;i++){
   int[] o=offsets[i];root.addOrReplaceChild("egg"+i,CubeListBuilder.create().texOffs(0,0).addBox(o[0],-o[1]-3,o[2],3,3,3),PartPose.ZERO);
  }
  return LayerDefinition.create(mesh,16,16);
 }
 private final Map<String,ModelPart> models=new HashMap<>();
 RoeRenderer(EntityRendererProvider.Context c){super(c);for(String s:STYLES)models.put(s,c.bakeLayer(location(s)));shadowRadius=0;}
 @Override public ResourceLocation getTextureLocation(RoeClutch e){return ResourceLocation.fromNamespaceAndPath(Aquaculture.ID,"textures/entity/roe/"+e.nursery()+".png");}
 @Override public void render(RoeClutch e,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light){
  pose.pushPose();
  float size=.78f+Math.min(4,e.eggs())*.055f;
  pose.scale(size,-size,size);
  models.getOrDefault(e.nursery(),models.get("gravel")).render(pose,buffers.getBuffer(RenderType.entityTranslucent(getTextureLocation(e))),light,OverlayTexture.NO_OVERLAY);
  pose.popPose();super.render(e,yaw,partial,pose,buffers,light);
 }
}
