package dev.minemods.worldlogic.client;
import dev.minemods.worldlogic.NaturalFluids;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
@EventBusSubscriber(modid="worldlogic",bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class NaturalFluidRendering{
 @SubscribeEvent public static void register(RegisterClientExtensionsEvent e){
  for(var f:NaturalFluids.families()){
   e.registerFluidType(new IClientFluidTypeExtensions(){
    @Override public ResourceLocation getStillTexture(){return ResourceLocation.withDefaultNamespace("block/water_still");}
    @Override public ResourceLocation getFlowingTexture(){return ResourceLocation.withDefaultNamespace("block/water_flow");}
    @Override public ResourceLocation getOverlayTexture(){return ResourceLocation.withDefaultNamespace("block/water_overlay");}
    @Override public int getTintColor(){return f.tint;}
    @Override public int getTintColor(net.minecraft.world.level.material.FluidState state,net.minecraft.world.level.BlockAndTintGetter level,net.minecraft.core.BlockPos pos){
     // Same biome-blended surface colour across all native/vanilla water.
     // Bucket/GUI tint remains the family-specific getTintColor() above.
     return 0xff000000|net.minecraft.client.renderer.BiomeColors.getAverageWaterColor(level,pos);
    }
   },f.type.get());
  }
 }
 @SubscribeEvent public static void layers(FMLClientSetupEvent e){
  e.enqueueWork(()->{for(var f:NaturalFluids.families()){
   ItemBlockRenderTypes.setRenderLayer(f.source.get(),RenderType.translucent());
   ItemBlockRenderTypes.setRenderLayer(f.flowing.get(),RenderType.translucent());
  }for(var b:dev.minemods.worldlogic.MarinePlants.blocks())ItemBlockRenderTypes.setRenderLayer(b,RenderType.cutout());});
 }
}
