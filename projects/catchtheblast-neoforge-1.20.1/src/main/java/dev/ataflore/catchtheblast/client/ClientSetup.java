package dev.ataflore.catchtheblast.client;
import dev.ataflore.catchtheblast.CatchTheBlast;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;
@Mod.EventBusSubscriber(modid=CatchTheBlast.ID,value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
public final class ClientSetup {
 @SubscribeEvent public static void layer(EntityRenderersEvent.RegisterLayerDefinitions event){event.registerLayerDefinition(BlastShieldGeometry.LAYER,BlastShieldGeometry::createLayer);}
 @SubscribeEvent public static void setup(FMLClientSetupEvent event){event.enqueueWork(()->ItemProperties.register(CatchTheBlast.BLAST_SHIELD.get(),new ResourceLocation(CatchTheBlast.ID,"blocking"),(stack,level,entity,seed)->entity!=null&&entity.isUsingItem()&&entity.getUseItem()==stack?1f:0f));}
}