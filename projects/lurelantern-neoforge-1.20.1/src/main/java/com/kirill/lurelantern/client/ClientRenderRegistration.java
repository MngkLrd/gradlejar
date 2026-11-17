package com.kirill.lurelantern.client;
import com.kirill.lurelantern.registry.*;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
@Mod.EventBusSubscriber(modid="lurelantern",bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class ClientRenderRegistration {
 @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers e){
  e.registerBlockEntityRenderer(ModBlockEntities.LURE_LANTERN.get(),CapturedMobRenderer::new);
 }
 @SubscribeEvent public static void setup(FMLClientSetupEvent e){
  e.enqueueWork(()->{
    ItemBlockRenderTypes.setRenderLayer(ModBlocks.LURE_LANTERN.get(),RenderType.translucent());
    ItemBlockRenderTypes.setRenderLayer(ModBlocks.ADVANCED_LURE_LANTERN.get(),RenderType.translucent());
  });
 }
}
