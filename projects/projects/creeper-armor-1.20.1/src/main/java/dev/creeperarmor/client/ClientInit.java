package dev.creeperarmor.client;
import dev.creeperarmor.CreeperArmorMod;
import dev.creeperarmor.registry.ModItems;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;
@Mod.EventBusSubscriber(modid=CreeperArmorMod.MOD_ID,bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class ClientInit {
    @SubscribeEvent public static void registerLayer(EntityRenderersEvent.RegisterLayerDefinitions e){
        e.registerLayerDefinition(CreeperShieldGeometry.LAYER,CreeperShieldGeometry::createLayer);
    }
    @SubscribeEvent public static void setup(FMLClientSetupEvent event){
        event.enqueueWork(()->ItemProperties.register(ModItems.CREEPER_SHIELD.get(),new ResourceLocation(CreeperArmorMod.MOD_ID,"blocking"),
            (s,l,e,id)->e!=null && e.isUsingItem() && e.getUseItem()==s ? 1F:0F));
    }
}
