package dev.minemods.traumacraft;
import dev.minemods.traumacraft.registry.ModItems;
import dev.minemods.traumacraft.registry.ModCreativeTabs;
import dev.minemods.traumacraft.event.TraumaEvents;
import dev.minemods.traumacraft.command.TraumaCommands;
import dev.minemods.traumacraft.client.ClientEvents;
import dev.minemods.traumacraft.network.ModNetwork;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

@Mod(TraumaCraft.MOD_ID)
public final class TraumaCraft {
    public static final String MOD_ID="traumacraft";
    public TraumaCraft(){
        IEventBus modBus=FMLJavaModLoadingContext.get().getModEventBus();
        ModItems.ITEMS.register(modBus);
        ModCreativeTabs.TABS.register(modBus);
        ModNetwork.register();
        MinecraftForge.EVENT_BUS.addListener(TraumaEvents::onDamage);
        MinecraftForge.EVENT_BUS.addListener(TraumaEvents::onTick);
        MinecraftForge.EVENT_BUS.addListener(TraumaEvents::onLogin);
        MinecraftForge.EVENT_BUS.addListener(TraumaEvents::onRespawn);
        MinecraftForge.EVENT_BUS.addListener(TraumaEvents::onClone);
        MinecraftForge.EVENT_BUS.addListener(TraumaCommands::register);
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->ClientEvents.register(modBus));
    }
}
