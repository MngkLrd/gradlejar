package dev.minemods.worldlogicterrain.worldgen;

import java.util.Set;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class TerrainWorldgenChecks {
    private TerrainWorldgenChecks() {}
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("worldlogicterrain").requires(s->s.hasPermission(2))
          .then(Commands.literal("worldgen_check").executes(c->{
            try {
                verify(c.getSource().getLevel());
                c.getSource().sendSuccess(()->Component.literal("Terrain worldgen: 3 core presets, 18 plant features, biome attachment verified"),false);
                return 1;
            } catch (IllegalStateException ex) {
                c.getSource().sendFailure(Component.literal(ex.getMessage())); return 0;
            }
          })));
    }
    private static void verify(ServerLevel level) {
        for(String presetId : new String[]{"realistic","earthlike","earthlike_climate"}) {
            var id=ResourceLocation.fromNamespaceAndPath("worldlogic",presetId);
            var preset=level.registryAccess().lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(ResourceKey.create(Registries.WORLD_PRESET,id)).value();
            var generator=preset.createWorldDimensions().overworld();
            if (!(generator instanceof NoiseBasedChunkGenerator gen) || !gen.generatorSettings().unwrapKey().map(k->k.location().equals(id)).orElse(false))
                throw new IllegalStateException("Wrong WorldLogic generator: "+presetId);
        checkPreset(gen, "meadow_grass", Set.of("plains","sunflower_plains","meadow"));
        checkPreset(gen, "dense_meadow_grass", Set.of("plains","sunflower_plains","meadow"));
        checkPreset(gen, "dry_grass", Set.of("desert","savanna","savanna_plateau","windswept_savanna","badlands","wooded_badlands","eroded_badlands"));
        checkPreset(gen, "tall_dry_grass", Set.of("desert","savanna","savanna_plateau","windswept_savanna","badlands","wooded_badlands","eroded_badlands"));
        checkPreset(gen, "desert_shrub", Set.of("desert","badlands","wooded_badlands","eroded_badlands","savanna"));
        checkPreset(gen, "forest_shrub", Set.of("forest","flower_forest","birch_forest","old_growth_birch_forest","dark_forest","taiga","old_growth_pine_taiga","old_growth_spruce_taiga","jungle","sparse_jungle","bamboo_jungle","swamp","mangrove_swamp"));
        checkPreset(gen, "dense_underbrush", Set.of("dark_forest","old_growth_spruce_taiga","jungle","bamboo_jungle","mangrove_swamp","swamp"));
        checkPreset(gen, "low_fern", Set.of("forest","flower_forest","birch_forest","old_growth_birch_forest","dark_forest","taiga","old_growth_pine_taiga","old_growth_spruce_taiga","jungle","sparse_jungle","bamboo_jungle","swamp","mangrove_swamp"));
        checkPreset(gen, "wild_heather", Set.of("meadow","windswept_hills","windswept_forest","taiga","snowy_taiga"));
        checkPreset(gen, "sedge", Set.of("swamp","mangrove_swamp","river","jungle","sparse_jungle"));
        checkPreset(gen, "reed", Set.of("swamp","mangrove_swamp","river"));
        checkPreset(gen, "cattail", Set.of("swamp","mangrove_swamp","river"));
        checkPreset(gen, "dwarf_shrub", Set.of("snowy_plains","snowy_taiga","grove","windswept_hills","windswept_forest","taiga"));
        checkPreset(gen, "forest_litter", Set.of("forest","flower_forest","birch_forest","old_growth_birch_forest","dark_forest","taiga","old_growth_pine_taiga","old_growth_spruce_taiga","jungle","sparse_jungle","bamboo_jungle","mangrove_swamp","swamp"));
        checkPreset(gen, "fallen_leaves", Set.of("forest","flower_forest","birch_forest","old_growth_birch_forest","dark_forest","jungle","sparse_jungle","bamboo_jungle","swamp","mangrove_swamp"));
        checkPreset(gen, "wild_chamomile", Set.of("plains","sunflower_plains","meadow","flower_forest","birch_forest","old_growth_birch_forest"));
        checkPreset(gen, "wild_cornflower", Set.of("plains","sunflower_plains","meadow","flower_forest","birch_forest","old_growth_birch_forest"));
        checkPreset(gen, "wild_clover", Set.of("plains","sunflower_plains","meadow"));
        }
    }
    private static void checkPreset(NoiseBasedChunkGenerator gen,String plant,Set<String> expected) {
        var key=ResourceKey.create(Registries.PLACED_FEATURE,ResourceLocation.fromNamespaceAndPath("worldlogicterrain","patch_"+plant));
        int step=GenerationStep.Decoration.VEGETAL_DECORATION.ordinal();
        int positives=0;
        for(var biome:gen.getBiomeSource().possibleBiomes()) {
            var biomeId=biome.unwrapKey().orElseThrow().location();
            // The test covers vanilla biome holders used by all three core presets.
            if(!biomeId.getNamespace().equals("minecraft"))continue;
            boolean wanted=expected.contains(biomeId.getPath());
            var features=biome.value().getGenerationSettings().features();
            boolean present=features.size()>step && features.get(step).stream().anyMatch(h->h.is(key));
            if(present!=wanted)throw new IllegalStateException("Wrong plant biome attachment: "+plant+" / "+biomeId);
            if(wanted)positives++;
        }
        if(positives!=expected.size())throw new IllegalStateException("Missing expected core biome for "+plant+": "+positives);
    }
}
