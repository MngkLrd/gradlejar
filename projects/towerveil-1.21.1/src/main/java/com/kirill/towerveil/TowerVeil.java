package com.kirill.towerveil;

import com.kirill.towerveil.command.TowerCommands;
import com.kirill.towerveil.gameplay.FarmlandTransition;
import com.kirill.towerveil.registry.ModBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

@Mod(TowerVeil.MOD_ID)
public final class TowerVeil {
    public static final String MOD_ID = "towerveil";
    public static final ResourceKey<Level> FIELD_DIMENSION = ResourceKey.create(
            Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "field")
    );

    public TowerVeil(IEventBus modBus) {
        ModBlocks.BLOCKS.register(modBus);
        NeoForge.EVENT_BUS.addListener(FarmlandTransition::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(TowerCommands::onRegister);
    }
}
