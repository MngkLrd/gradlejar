package com.kirill.lurelantern.client;
import com.kirill.lurelantern.registry.ModBlockEntities;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
public final class ClientRenderRegistration implements ClientModInitializer {
 @Override public void onInitializeClient(){
  BlockEntityRenderers.register(ModBlockEntities.LURE.get(),CapturedMobRenderer::new);
 }
}
