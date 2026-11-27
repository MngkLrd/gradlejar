package dev.minemods.worldlogic.client;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterDimensionSpecialEffectsEvent;
@EventBusSubscriber(modid="worldlogic",bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class ExtendedWorldEffects {
 private ExtendedWorldEffects(){}
 @SubscribeEvent public static void register(RegisterDimensionSpecialEffectsEvent e){
  e.register(ResourceLocation.fromNamespaceAndPath("worldlogic","extended_overworld"),new DimensionSpecialEffects.OverworldEffects(){
   @Override public float getCloudHeight(){return 440.0F;}
  });
 }
}
