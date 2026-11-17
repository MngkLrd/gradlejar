package com.kirill.lurelantern.client;
import com.kirill.lurelantern.registry.ModBlockEntities;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
public final class ClientRenderRegistration {
 private ClientRenderRegistration(){}
 public static void register(IEventBus bus){bus.addListener(ClientRenderRegistration::registerBlockEntityRenderers);}
 private static void registerBlockEntityRenderers(EntityRenderersEvent.RegisterRenderers event){
  event.registerBlockEntityRenderer(ModBlockEntities.LURE.get(),CapturedMobRenderer::new);
 }
}
