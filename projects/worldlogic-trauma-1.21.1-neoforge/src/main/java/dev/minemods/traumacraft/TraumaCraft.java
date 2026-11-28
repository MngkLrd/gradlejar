package dev.minemods.traumacraft;
import dev.minemods.traumacraft.client.ClientEvents;
import dev.minemods.traumacraft.command.TraumaCommands;
import dev.minemods.traumacraft.event.TraumaEvents;
import dev.minemods.traumacraft.event.FunctionEvents;
import dev.minemods.traumacraft.health.VitalityBridge;
import dev.minemods.traumacraft.network.ModNetwork;
import dev.minemods.traumacraft.registry.*;
import dev.minemods.traumacraft.qa.RegressionChecks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
@Mod(TraumaCraft.LOADER_MOD_ID)
public final class TraumaCraft {
    public static final String LOADER_MOD_ID="worldlogic_trauma";
    // Preserve all established registry, asset and network identifiers across the branding change.
    public static final String MOD_ID="traumacraft";
    public TraumaCraft(IEventBus modBus) {
        if(FMLEnvironment.dist==Dist.CLIENT) ClientEvents.register(modBus);
        dev.minemods.traumacraft.content.HerbContent.register(modBus);
        ModItems.register(modBus); ModMenus.register(modBus); ModCreativeTabs.register(modBus);
        modBus.addListener(ModNetwork::register);
        modBus.addListener((net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent e)->e.enqueueWork(dev.minemods.traumacraft.integration.CorePresentation::register));
        var bus=NeoForge.EVENT_BUS;
        bus.addListener(EventPriority.LOWEST,TraumaEvents::onDamage);
        bus.addListener(EventPriority.LOWEST,dev.minemods.traumacraft.event.DeathReports::onDeath);
        bus.addListener(EventPriority.LOWEST,VitalityBridge::onHeal);
        bus.addListener(TraumaEvents::onPlayerTick);
        bus.addListener(EventPriority.HIGHEST,FunctionEvents::attack);
        bus.addListener(EventPriority.HIGHEST,FunctionEvents::breakBlock);
        bus.addListener(EventPriority.LOWEST,FunctionEvents::jump);
        bus.addListener(EventPriority.HIGHEST,FunctionEvents::rightClickItem);
        bus.addListener(EventPriority.HIGHEST,FunctionEvents::rightClickBlock);
        bus.addListener(EventPriority.LOWEST,dev.minemods.traumacraft.content.HerbInteraction::right);
        bus.addListener(EventPriority.LOWEST,dev.minemods.traumacraft.content.HerbInteraction::left);
        bus.addListener(TraumaEvents::onLogin);
        bus.addListener(TraumaEvents::onLogout);
        bus.addListener(EventPriority.LOWEST,TraumaEvents::onClone);
        bus.addListener(EventPriority.LOWEST,TraumaEvents::onRespawn);
        bus.addListener(TraumaEvents::onDimension);
        bus.addListener(TraumaCommands::register);
        bus.addListener(RegressionChecks::register);
    }
}
