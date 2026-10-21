package dev.minemods.traumacraft;

import dev.minemods.traumacraft.registry.ModAttachments;
import dev.minemods.traumacraft.registry.ModItems;
import dev.minemods.traumacraft.event.TraumaEvents;
import dev.minemods.traumacraft.command.TraumaCommands;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

@Mod(TraumaCraft.MOD_ID)
public final class TraumaCraft {
    public static final String MOD_ID = "traumacraft";

    public TraumaCraft(IEventBus modBus) {
        ModAttachments.register(modBus);
        ModItems.register(modBus);
        NeoForge.EVENT_BUS.addListener(TraumaEvents::onDamage);
        NeoForge.EVENT_BUS.addListener(TraumaEvents::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(TraumaCommands::register);
    }
}
