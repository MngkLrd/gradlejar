package dev.minemods.traumacraft;

import dev.minemods.traumacraft.client.ClientEvents;
import dev.minemods.traumacraft.command.TraumaCommands;
import dev.minemods.traumacraft.event.TraumaEvents;
import dev.minemods.traumacraft.network.ModNetwork;
import dev.minemods.traumacraft.registry.ModAttachments;
import dev.minemods.traumacraft.registry.ModCreativeTabs;
import dev.minemods.traumacraft.registry.ModItems;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;

@Mod(TraumaCraft.MOD_ID)
public final class TraumaCraft {
    public static final String MOD_ID = "traumacraft";
    public TraumaCraft(IEventBus modBus) {
        if (FMLEnvironment.dist == Dist.CLIENT) ClientEvents.register(modBus);
        ModAttachments.register(modBus);
        ModItems.register(modBus);
        ModCreativeTabs.register(modBus);
        modBus.addListener(ModNetwork::register);
        NeoForge.EVENT_BUS.addListener(TraumaEvents::onDamage);
        NeoForge.EVENT_BUS.addListener(TraumaEvents::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(TraumaEvents::onLogin);
        NeoForge.EVENT_BUS.addListener(TraumaEvents::onRespawn);
        NeoForge.EVENT_BUS.addListener(TraumaCommands::register);
    }
}
