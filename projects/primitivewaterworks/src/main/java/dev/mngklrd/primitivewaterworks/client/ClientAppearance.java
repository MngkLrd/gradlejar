package dev.mngklrd.primitivewaterworks.client;

import dev.mngklrd.primitivewaterworks.PrimitiveWaterworks;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.registries.RegistryObject;

@Mod.EventBusSubscriber(modid=PrimitiveWaterworks.MOD_ID,bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class ClientAppearance {
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            for(RegistryObject<Block> block:PrimitiveWaterworks.blocks())
                ItemBlockRenderTypes.setRenderLayer(block.get(),RenderType.translucent());
        });
    }
    @SubscribeEvent
    public static void onBlockColors(RegisterColorHandlersEvent.Block event) {
        Block[] blocks=PrimitiveWaterworks.blocks().stream().map(RegistryObject::get).toArray(Block[]::new);
        event.register((state,world,pos,index) -> 0x3F76E4,blocks);
    }
}
