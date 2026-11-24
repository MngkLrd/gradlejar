package dev.ataflore.catchtheblast.client;
import dev.ataflore.catchtheblast.CatchTheBlast;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
public final class ClientSetup implements ClientModInitializer {
 public void onInitializeClient(){
  EntityModelLayerRegistry.registerModelLayer(BlastShieldGeometry.LAYER,BlastShieldGeometry::createLayer);
  BuiltinItemRendererRegistry.INSTANCE.register(CatchTheBlast.BLAST_SHIELD,new BuiltinItemRendererRegistry.DynamicItemRenderer(){private BlastShieldRenderer renderer; public void render(net.minecraft.world.item.ItemStack stack,net.minecraft.world.item.ItemDisplayContext context,com.mojang.blaze3d.vertex.PoseStack pose,net.minecraft.client.renderer.MultiBufferSource buffers,int light,int overlay){if(renderer==null)renderer=new BlastShieldRenderer();renderer.renderByItem(stack,context,pose,buffers,light,overlay);}});
  ItemProperties.register(CatchTheBlast.BLAST_SHIELD,new ResourceLocation(CatchTheBlast.ID,"blocking"),(stack,level,entity,seed)->entity!=null&&entity.isUsingItem()&&entity.getUseItem()==stack?1f:0f);
  HudRenderCallback.EVENT.register((graphics,tick)->ChargeHud.draw(graphics));
 }
}