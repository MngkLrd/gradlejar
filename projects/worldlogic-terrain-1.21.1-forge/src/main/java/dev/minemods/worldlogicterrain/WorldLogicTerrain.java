package dev.minemods.worldlogicterrain;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import dev.minemods.worldlogicterrain.registry.TerrainContent;

@Mod(WorldLogicTerrain.MOD_ID)
public final class WorldLogicTerrain {
    public static final String MOD_ID = "worldlogicterrain";
    public WorldLogicTerrain(FMLJavaModLoadingContext context) {
        var bus = context.getModEventBus();
        TerrainContent.BLOCKS.register(bus);
        TerrainContent.ITEMS.register(bus);
        bus.addListener(TerrainContent::creative);
    }
}
