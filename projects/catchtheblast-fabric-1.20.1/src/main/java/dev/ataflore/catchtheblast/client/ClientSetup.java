package dev.ataflore.catchtheblast.client;
import dev.ataflore.catchtheblast.CatchTheBlast;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
public final class ClientSetup implements ClientModInitializer {
 public void onInitialize(){
  EntityModelLayerRegistry.registerModelLayer(BlastShieldGeometry.LAYER,BlastShieldGeometry::createLayer);
  BuiltinItemRendererRegistry.INSTANCE.register(CatchTheBlast.BLAST_SHIELD,(stack,context,pose,buffers,light,overlay)->new BlastShieldRenderer().renderByItem(stack,context,pose,buffers,light,overlay));
  ItemProperties.register(CatchTheBlast.BLAST_SHIELD,new ResourceLocation(CatchTheBlast.ID,"blocking"),(stack,level,entity,seed)->entity!=null&&entity.isUsingItem()&&entity.getUseItem()==stack?1f:0f);
  HudRenderCallback.EVENT.register((graphics,tick)->ChargeHud.draw(graphics));
 }
}