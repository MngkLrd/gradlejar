package dev.minemods.phantomlure;

import dev.minemods.phantomlure.gameplay.PhantomCleanup;
import dev.minemods.phantomlure.registry.ModBlockEntities;
import dev.minemods.phantomlure.registry.ModBlocks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(PhantomLure.MOD_ID)
public final class PhantomLure {
    public static final String MOD_ID = "phantomlure";
    public PhantomLure() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModBlocks.register(modBus);
        ModBlockEntities.register(modBus);
        MinecraftForge.EVENT_BUS.addListener(PhantomCleanup::onTick);
    }
}
