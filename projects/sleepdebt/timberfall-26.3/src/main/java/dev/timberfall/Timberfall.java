package dev.timberfall;
import dev.timberfall.client.ClientSetup;
import dev.timberfall.registry.TimberfallBlocks;
import dev.timberfall.registry.TimberfallEntities;
import dev.timberfall.tree.ChopEvents;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.fml.loading.FMLEnvironment;
@Mod(Timberfall.MOD_ID)
public final class Timberfall {
    public static final String MOD_ID = "timberfall";
    public Timberfall(IEventBus modBus) {
        TimberfallBlocks.REGISTRY.register(modBus);
        TimberfallEntities.REGISTRY.register(modBus);
        NeoForge.EVENT_BUS.addListener(ChopEvents::onRightClickBlock);
        if (FMLEnvironment.getDist() == Dist.CLIENT) ClientSetup.register(modBus);
    }
}
