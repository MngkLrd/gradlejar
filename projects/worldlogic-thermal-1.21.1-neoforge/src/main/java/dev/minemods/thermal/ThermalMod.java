package dev.minemods.thermal;
import dev.minemods.thermal.network.ThermalSync;
import dev.minemods.thermal.server.ThermalEvents;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.fml.common.Mod;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
@Mod(ThermalMod.ID)
public final class ThermalMod {
    public static final String ID = "worldlogic_thermal";
    public ThermalMod(IEventBus modBus) {
        dev.minemods.thermal.content.ThermalContent.register(modBus);
        modBus.addListener(ThermalSync::register);
        var bus = NeoForge.EVENT_BUS;
        bus.addListener(EventPriority.LOWEST, ThermalEvents::tick);
        bus.addListener(EventPriority.LOWEST,dev.minemods.thermal.content.FiberHarvest::onClick);
        bus.addListener(EventPriority.LOWEST,dev.minemods.thermal.content.FiberInteraction::right);
        bus.addListener(EventPriority.LOWEST,dev.minemods.thermal.content.FiberInteraction::left);
        bus.addListener(EventPriority.LOWEST,dev.minemods.thermal.content.ThermalPackItem::interact);
        bus.addListener(ThermalEvents::clonePlayer);
        bus.addListener(ThermalEvents::registerCommands);
    }
}
