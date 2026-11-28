package dev.minemods.aquaculture.client;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.minemods.aquaculture.*;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;

final class MarineRenderer extends MobRenderer<MarineAnimal,MarineModel>{
 private final MarineSpecies s;
 static ModelLayerLocation layer(MarineSpecies s){return new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(Aquaculture.ID,"marine/"+s.id),"main");}
 MarineRenderer(EntityRendererProvider.Context ctx,MarineSpecies s){super(ctx,new MarineModel(ctx.bakeLayer(layer(s)),s),Math.min(s.width*.4f,1.3f));this.s=s;}
 @Override public ResourceLocation getTextureLocation(MarineAnimal e){return ResourceLocation.fromNamespaceAndPath(Aquaculture.ID,"textures/entity/marine/"+s.id+".png");}
 @Override protected void scale(MarineAnimal e,PoseStack pose,float partial){
  float scale=s.length*16f/MarineGeometry.ALL.get(s.id).pixels();pose.scale(scale,scale,scale);
 }
}
