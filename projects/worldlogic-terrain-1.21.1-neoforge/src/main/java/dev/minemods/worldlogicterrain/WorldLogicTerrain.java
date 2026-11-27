package dev.minemods.worldlogicterrain;
import net.neoforged.fml.common.Mod;
import net.neoforged.bus.api.IEventBus;
import dev.minemods.worldlogicterrain.registry.TerrainContent;
@Mod(WorldLogicTerrain.MOD_ID)
public final class WorldLogicTerrain {
    public static final String MOD_ID = "worldlogicterrain";
    public WorldLogicTerrain(IEventBus bus) {
        TerrainContent.BLOCKS.register(bus);
        TerrainContent.ITEMS.register(bus);
        bus.addListener(TerrainContent::creative);
    }
}
