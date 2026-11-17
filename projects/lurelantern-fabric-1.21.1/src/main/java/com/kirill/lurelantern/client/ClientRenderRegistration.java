package com.kirill.lurelantern.client;
import com.kirill.lurelantern.registry.ModBlocks;
import com.kirill.lurelantern.registry.ModBlockEntities;
import com.kirill.lurelantern.registry.ModItems;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
public final class ClientRenderRegistration implements ClientModInitializer {
 private AdvancedLanternItemRenderer renderer;
 @Override public void onInitializeClient(){
   BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.LURE_LANTERN.get(),RenderType.translucent());
   BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.ADVANCED_LURE_LANTERN.get(),RenderType.translucent());
   BlockEntityRenderers.register(ModBlockEntities.LURE_LANTERN.get(),CapturedMobRenderer::new);
   BuiltinItemRendererRegistry.INSTANCE.register(ModItems.ADVANCED_LURE_LANTERN.get(),
     (stack,context,pose,buffers,light,overlay)->{
       if(renderer==null) renderer=new AdvancedLanternItemRenderer();
       renderer.renderByItem(stack,context,pose,buffers,light,overlay);
     });
 }
}
