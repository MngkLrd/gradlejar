package dev.minemods.worldlogicterrain;
import net.neoforged.fml.common.Mod;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import dev.minemods.worldlogicterrain.registry.TerrainContent;
import dev.minemods.worldlogicterrain.mobility.TerrainMobility;
import dev.minemods.worldlogicterrain.worldgen.TerrainWorldgenChecks;
import dev.minemods.worldlogicterrain.worldgen.TerrainPatchChecks;
import dev.minemods.worldlogicterrain.food.ForageChecks;
import dev.minemods.worldlogicterrain.tree.EcologicalTreeFeature;
import dev.minemods.worldlogicterrain.tree.TreeChecks;
@Mod(WorldLogicTerrain.MOD_ID)
public final class WorldLogicTerrain {
    public static final String MOD_ID = "worldlogicterrain";
    public WorldLogicTerrain(IEventBus bus) {
        TerrainContent.BLOCKS.register(bus);
        TerrainContent.ITEMS.register(bus);
        EcologicalTreeFeature.FEATURES.register(bus);
        bus.addListener(TerrainContent::creative);
        NeoForge.EVENT_BUS.addListener(TerrainMobility::tick);
        NeoForge.EVENT_BUS.addListener(TerrainMobility::register);
        NeoForge.EVENT_BUS.addListener(TerrainWorldgenChecks::register);
        NeoForge.EVENT_BUS.addListener(TerrainPatchChecks::register);
        NeoForge.EVENT_BUS.addListener(TreeChecks::register);
        NeoForge.EVENT_BUS.addListener(ForageChecks::register);
        NeoForge.EVENT_BUS.addListener(TerrainMobility::logout);
    }
}
