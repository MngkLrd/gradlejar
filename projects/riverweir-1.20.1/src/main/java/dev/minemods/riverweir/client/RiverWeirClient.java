package dev.minemods.riverweir.client;

import dev.minemods.riverweir.RiverWeirMod;
import dev.minemods.riverweir.menu.WeirScreen;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid=RiverWeirMod.ID, bus=Mod.EventBusSubscriber.Bus.MOD, value=Dist.CLIENT)
public final class RiverWeirClient {
    @SubscribeEvent public static void init(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ItemBlockRenderTypes.setRenderLayer(RiverWeirMod.NET.get(),RenderType.cutout());
            MenuScreens.register(RiverWeirMod.WEIR_MENU.get(),WeirScreen::new);
        });
    }
}
