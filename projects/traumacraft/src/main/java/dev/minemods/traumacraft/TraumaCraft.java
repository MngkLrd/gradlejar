package dev.minemods.traumacraft;

import dev.minemods.traumacraft.registry.ModAttachments;
import dev.minemods.traumacraft.registry.ModItems;
import dev.minemods.traumacraft.event.TraumaEvents;
import dev.minemods.traumacraft.command.TraumaCommands;
import dev.minemods.traumacraft.network.MedicalNetworking;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import dev.minemods.traumacraft.client.ClientEvents;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;

@Mod(TraumaCraft.MOD_ID)
public final class TraumaCraft {
    public static final String MOD_ID = "traumacraft";
    public TraumaCraft(IEventBus modBus) {
        if (FMLEnvironment.dist == Dist.CLIENT) ClientEvents.register(modBus);
        ModAttachments.register(modBus);
        ModItems.register(modBus);
        modBus.addListener(MedicalNetworking::register);
        NeoForge.EVENT_BUS.addListener(TraumaEvents::onDamage);
        NeoForge.EVENT_BUS.addListener(TraumaEvents::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(TraumaEvents::onLogin);
        NeoForge.EVENT_BUS.addListener(TraumaEvents::onRespawn);
        NeoForge.EVENT_BUS.addListener(TraumaCommands::register);
    }
}
