package dev.minemods.traumacraft;
import dev.minemods.traumacraft.registry.ModItems;
import dev.minemods.traumacraft.registry.ModCreativeTabs;
import dev.minemods.traumacraft.command.TraumaCommands;
import dev.minemods.traumacraft.event.TraumaEvents;
import dev.minemods.traumacraft.network.ModNetwork;
import dev.minemods.traumacraft.client.ClientEvents;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;

@Mod(TraumaCraft.MOD_ID)
public final class TraumaCraft {
    public static final String MOD_ID="traumacraft";
    public TraumaCraft(IEventBus bus){
        ModItems.ITEMS.register(bus);
        ModCreativeTabs.TABS.register(bus);
        bus.addListener(ModNetwork::register);
        NeoForge.EVENT_BUS.addListener(TraumaEvents::onDamage);
        NeoForge.EVENT_BUS.addListener(TraumaEvents::onTick);
        NeoForge.EVENT_BUS.addListener(TraumaEvents::onLogin);
        NeoForge.EVENT_BUS.addListener(TraumaEvents::onRespawn);
        NeoForge.EVENT_BUS.addListener(TraumaEvents::onClone);
        NeoForge.EVENT_BUS.addListener(TraumaCommands::register);
        if(FMLEnvironment.getDist()==Dist.CLIENT)ClientEvents.register(bus);
    }
}
