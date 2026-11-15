package dev.witchhunted;

import dev.witchhunted.event.HollowEvents;
import dev.witchhunted.registry.ModContent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

@Mod(WitchHunted.MOD_ID)
public final class WitchHunted {
    public static final String MOD_ID = "witchhunted";
    public static final ResourceKey<Level> THE_HOLLOW =
        ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath(MOD_ID, "the_hollow"));

    public WitchHunted(IEventBus modBus) {
        ModContent.BLOCKS.register(modBus);
        ModContent.ITEMS.register(modBus);
        modBus.addListener(ModContent::populateCreative);
        NeoForge.EVENT_BUS.addListener(HollowEvents::onDrops);
        NeoForge.EVENT_BUS.addListener(HollowEvents::onPlayerTick);
    }
}
