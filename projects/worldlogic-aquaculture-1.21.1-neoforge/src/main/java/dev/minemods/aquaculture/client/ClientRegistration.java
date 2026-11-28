package dev.minemods.aquaculture.client;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.minemods.aquaculture.*;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Cod;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid=Aquaculture.MOD_ID,bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class ClientRegistration {
 private static ModelLayerLocation layer(Species s){return new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(Aquaculture.ID,s.id()),"main");}
 @SubscribeEvent public static void layers(EntityRenderersEvent.RegisterLayerDefinitions e){
  for(String style:RoeRenderer.STYLES)e.registerLayerDefinition(RoeRenderer.location(style),()->RoeRenderer.layer(style));
  for(var s:Species.ALL)e.registerLayerDefinition(layer(s),()->AquaFishModel.layer(FishVisualGeometry.get(s.id())));
 }
 @SubscribeEvent public static void register(EntityRenderersEvent.RegisterRenderers e){
  e.registerEntityRenderer(Aquaculture.ROE.get(),RoeRenderer::new);
  for(var s:Species.ALL)e.registerEntityRenderer(Aquaculture.TYPES.get(s.id()).get(),ctx->new RegionalFishRenderer(ctx,s));
 }
 private static final class RegionalFishRenderer extends MobRenderer<Cod,AquaFishModel>{
  private final Species s;private final FishVisualGeometry.Shape shape;private final ResourceLocation texture;
  RegionalFishRenderer(EntityRendererProvider.Context ctx,Species species){
   super(ctx,new AquaFishModel(ctx.bakeLayer(layer(species)),FishVisualGeometry.get(species.id())),.15f);
   s=species;shape=FishVisualGeometry.get(s.id());
   texture=ResourceLocation.fromNamespaceAndPath(Aquaculture.ID,"textures/entity/fish/"+s.id()+".png");
  }
  @Override public ResourceLocation getTextureLocation(Cod fish){return texture;}
  @Override protected void scale(Cod fish,PoseStack pose,float partial){
   float length=fish instanceof Fish f?f.visualLength(s):Fish.visualLengthOf(s,s.adultLength()*.8);
   float scale=length*16f/shape.pixels();pose.scale(scale,scale,scale);
  }
 }
}
