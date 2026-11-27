package dev.minemods.traumacraft;
import dev.minemods.traumacraft.client.ClientEvents;
import dev.minemods.traumacraft.command.TraumaCommands;
import dev.minemods.traumacraft.event.TraumaEvents;
import dev.minemods.traumacraft.health.VitalityBridge;
import dev.minemods.traumacraft.network.ModNetwork;
import dev.minemods.traumacraft.registry.*;
import dev.minemods.traumacraft.qa.RegressionChecks;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.EventPriority;
@Mod(TraumaCraft.MOD_ID)
public final class TraumaCraft {
    public static final String MOD_ID="traumacraft";
    public TraumaCraft(FMLJavaModLoadingContext context) {
        var modBus=context.getModEventBus();
        if(FMLEnvironment.dist==Dist.CLIENT) ClientEvents.register(modBus);
        ModItems.register(modBus); ModMenus.register(modBus); ModCreativeTabs.register(modBus);
        ModNetwork.init();
        var bus=MinecraftForge.EVENT_BUS;
        bus.addListener(EventPriority.LOWEST,TraumaEvents::onDamage);
        bus.addListener(EventPriority.LOWEST,VitalityBridge::onHeal);
        bus.addListener(TraumaEvents::onPlayerTick);
        bus.addListener(TraumaEvents::onLogin);
        bus.addListener(TraumaEvents::onLogout);
        bus.addListener(TraumaEvents::onClone);
        bus.addListener(TraumaEvents::onRespawn);
        bus.addListener(TraumaEvents::onDimension);
        bus.addListener(TraumaCommands::register);
        bus.addListener(RegressionChecks::register);
    }
}
