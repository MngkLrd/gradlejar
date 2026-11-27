package dev.minemods.worldlogicterrain.worldgen;

import java.util.Set;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
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
                c.getSource().sendSuccess(()->Component.literal("Terrain worldgen: legacy and available unified core presets, 56 plant features and ecological trees, biome attachment verified"),false);
                return 1;
            } catch (IllegalStateException ex) {
                c.getSource().sendFailure(Component.literal(ex.getMessage())); return 0;
            }
          })));
    }
    private static void verify(ServerLevel level) {
        for(String presetId : new String[]{"realistic","earthlike","earthlike_climate","earthlike_extended","realistic_unified"}) {
            var id=ResourceLocation.fromNamespaceAndPath("worldlogic",presetId);
            var entry=level.registryAccess().lookupOrThrow(Registries.WORLD_PRESET).get(ResourceKey.create(Registries.WORLD_PRESET,id));
            if(entry.isEmpty()){
                if(presetId.equals("earthlike_extended")||presetId.equals("realistic_unified"))continue;
                throw new IllegalStateException("Missing core preset "+presetId);
            }
            var preset=entry.orElseThrow().value();
            var generator=preset.createWorldDimensions().overworld();
            if (!(generator instanceof NoiseBasedChunkGenerator gen) || !gen.generatorSettings().unwrapKey().map(k->k.location().equals(id)).orElse(false))
                throw new IllegalStateException("Wrong WorldLogic generator: "+presetId);
        checkPreset(gen, "tall_grass_plains", "tall_grass_plains", Set.of("plains","sunflower_plains","meadow"));
        checkPreset(gen, "meadow_grass", "meadow", Set.of("plains","sunflower_plains","meadow"));
        checkPreset(gen, "dense_meadow_grass", "meadow", Set.of("plains","sunflower_plains","meadow"));
        checkPreset(gen, "dry_grass", "dry", Set.of("desert","savanna","savanna_plateau","windswept_savanna","badlands","wooded_badlands","eroded_badlands","beach"));
        checkPreset(gen, "tall_dry_grass", "dry", Set.of("desert","savanna","savanna_plateau","windswept_savanna","badlands","wooded_badlands","eroded_badlands","beach"));
        checkPreset(gen, "desert_shrub", "desert", Set.of("desert","badlands","wooded_badlands","eroded_badlands","savanna"));
        checkPreset(gen, "forest_shrub", "forest", Set.of("forest","flower_forest","birch_forest","old_growth_birch_forest","dark_forest","taiga","old_growth_pine_taiga","old_growth_spruce_taiga","jungle","sparse_jungle","bamboo_jungle","swamp","mangrove_swamp"));
        checkPreset(gen, "dense_underbrush", "lush", Set.of("dark_forest","old_growth_spruce_taiga","jungle","bamboo_jungle","mangrove_swamp","swamp"));
        checkPreset(gen, "low_fern", "forest", Set.of("forest","flower_forest","birch_forest","old_growth_birch_forest","dark_forest","taiga","old_growth_pine_taiga","old_growth_spruce_taiga","jungle","sparse_jungle","bamboo_jungle","swamp","mangrove_swamp"));
        checkPreset(gen, "wild_heather", "heather", Set.of("meadow","windswept_hills","windswept_forest","taiga","snowy_taiga"));
        checkPreset(gen, "sedge", "wet", Set.of("swamp","mangrove_swamp","river","jungle","sparse_jungle"));
        checkPreset(gen, "reed", "marsh", Set.of("swamp","mangrove_swamp","river"));
        checkPreset(gen, "cattail", "marsh", Set.of("swamp","mangrove_swamp","river"));
        checkPreset(gen, "dwarf_shrub", "cold", Set.of("snowy_plains","snowy_taiga","grove","windswept_hills","windswept_forest","taiga"));
        checkPreset(gen, "forest_litter", "litter", Set.of("forest","flower_forest","birch_forest","old_growth_birch_forest","dark_forest","taiga","old_growth_pine_taiga","old_growth_spruce_taiga","jungle","sparse_jungle","bamboo_jungle","mangrove_swamp","swamp"));
        checkPreset(gen, "fallen_leaves", "leaves", Set.of("forest","flower_forest","birch_forest","old_growth_birch_forest","dark_forest","jungle","sparse_jungle","bamboo_jungle","swamp","mangrove_swamp"));
        checkPreset(gen, "wild_chamomile", "flower", Set.of("plains","sunflower_plains","meadow","flower_forest","birch_forest","old_growth_birch_forest"));
        checkPreset(gen, "wild_cornflower", "flower", Set.of("plains","sunflower_plains","meadow","flower_forest","birch_forest","old_growth_birch_forest"));
        checkPreset(gen, "wild_clover", "meadow", Set.of("plains","sunflower_plains","meadow"));
        checkPreset(gen, "wild_strawberry", "temperate_meadow", Set.of("plains","sunflower_plains","meadow","flower_forest","birch_forest"));
        checkPreset(gen, "wild_blueberry", "cool_woodland", Set.of("taiga","snowy_taiga","old_growth_pine_taiga","old_growth_spruce_taiga","forest","birch_forest","old_growth_birch_forest"));
        checkPreset(gen, "wild_raspberry", "temperate_woodland", Set.of("forest","birch_forest","old_growth_birch_forest","flower_forest","dark_forest","taiga"));
        checkPreset(gen, "wild_blackberry", "temperate_woodland", Set.of("forest","birch_forest","old_growth_birch_forest","flower_forest","dark_forest","taiga"));
        checkPreset(gen, "wild_grape", "warm_woodland", Set.of("jungle","sparse_jungle","bamboo_jungle","forest","savanna","wooded_badlands"));
        checkPreset(gen, "wild_pineapple", "tropical", Set.of("jungle","sparse_jungle","bamboo_jungle","mangrove_swamp"));
        checkPreset(gen, "wild_tomato", "warm_field", Set.of("plains","savanna","savanna_plateau","windswept_savanna","jungle","sparse_jungle"));
        checkPreset(gen, "wild_cucumber", "warm_field", Set.of("plains","savanna","savanna_plateau","windswept_savanna","jungle","sparse_jungle"));
        checkPreset(gen, "wild_radish", "temperate_meadow", Set.of("plains","sunflower_plains","meadow","flower_forest","birch_forest"));
        checkPreset(gen, "wild_onion", "open_field", Set.of("plains","sunflower_plains","meadow","savanna","savanna_plateau","windswept_savanna"));
        checkPreset(gen, "wild_cabbage", "cool_field", Set.of("plains","meadow","sunflower_plains","birch_forest","flower_forest"));
        checkPreset(gen, "wild_bell_pepper", "warm_field", Set.of("plains","savanna","savanna_plateau","windswept_savanna","jungle","sparse_jungle"));
        checkPreset(gen, "bracken", "bracken", Set.of("forest","dark_forest","birch_forest","old_growth_birch_forest","taiga","old_growth_pine_taiga","old_growth_spruce_taiga","jungle","sparse_jungle"));
        checkPreset(gen, "marsh_marigold", "marsh_flower", Set.of("swamp","mangrove_swamp","river"));
        checkPreset(gen, "short_grass", "grass_fill", Set.of("plains","sunflower_plains","meadow","forest","flower_forest","birch_forest","old_growth_birch_forest","dark_forest","taiga","old_growth_pine_taiga","old_growth_spruce_taiga","jungle","sparse_jungle","bamboo_jungle","savanna","savanna_plateau","windswept_savanna","windswept_hills","windswept_forest","swamp","mangrove_swamp","river","wooded_badlands","windswept_gravelly_hills"));
        checkPreset(gen, "open_meadow", "open_meadow", Set.of("plains","sunflower_plains","meadow","windswept_hills","windswept_forest","windswept_gravelly_hills"));
        checkPreset(gen, "frosted_grass", "frosted_ground", Set.of("snowy_plains","snowy_taiga","grove","snowy_slopes","frozen_river","snowy_beach"));
            checkTrees(gen);
            checkAquatic(gen);
            for(var p:dev.minemods.worldlogicterrain.growth.PlantSpecies.newSpecies())checkSeasonal(gen,p.id(),p.habitat());
        }
    }
    private static void checkPreset(NoiseBasedChunkGenerator gen,String plant,String group,Set<String> expected) {
        var key=ResourceKey.create(Registries.PLACED_FEATURE,ResourceLocation.fromNamespaceAndPath("worldlogicterrain","patch_"+plant));
        int step=GenerationStep.Decoration.VEGETAL_DECORATION.ordinal();
        int positives=0,vanillaCount=0;
        var habitat=TagKey.create(Registries.BIOME,ResourceLocation.fromNamespaceAndPath("worldlogicterrain",group));
        for(var biome:gen.getBiomeSource().possibleBiomes()) {
            var biomeId=biome.unwrapKey().orElseThrow().location();
            boolean vanilla=biomeId.getNamespace().equals("minecraft");
            if(vanilla)vanillaCount++;
            boolean wanted=vanilla?expected.contains(biomeId.getPath()):biome.is(habitat);
            var features=biome.value().getGenerationSettings().features();
            boolean present=features.size()>step && features.get(step).stream().anyMatch(h->h.is(key));
            if(present!=wanted)throw new IllegalStateException("Wrong plant biome attachment: "+plant+" / "+biomeId);
            if(vanilla&&wanted)positives++;
        }
        if(vanillaCount>0&&positives!=expected.size())throw new IllegalStateException("Missing expected core biome for "+plant+": "+positives);
    }
    private static void checkSeasonal(NoiseBasedChunkGenerator gen,String plant,String group){
        var key=ResourceKey.create(Registries.PLACED_FEATURE,ResourceLocation.fromNamespaceAndPath("worldlogicterrain","patch_wild_"+plant));
        var habitat=TagKey.create(Registries.BIOME,ResourceLocation.fromNamespaceAndPath("worldlogicterrain",group));
        int step=GenerationStep.Decoration.VEGETAL_DECORATION.ordinal();
        for(var biome:gen.getBiomeSource().possibleBiomes()){
            var settings=biome.value().getGenerationSettings().features();
            boolean present=settings.size()>step&&settings.get(step).stream().anyMatch(h->h.is(key));
            if(present!=biome.is(habitat))throw new IllegalStateException("Wrong seasonal plant habitat "+plant+" / "+biome.unwrapKey());
        }
    }
    private static void checkAquatic(NoiseBasedChunkGenerator gen){
        var key=ResourceKey.create(Registries.PLACED_FEATURE,ResourceLocation.fromNamespaceAndPath("worldlogicterrain","aquatic_ecology"));
        var habitat=TagKey.create(Registries.BIOME,ResourceLocation.fromNamespaceAndPath("worldlogicterrain","aquatic_habitat"));
        int step=GenerationStep.Decoration.VEGETAL_DECORATION.ordinal();
        int positives=0;
        for(var biome:gen.getBiomeSource().possibleBiomes()){
            var settings=biome.value().getGenerationSettings().features();
            boolean present=settings.size()>step&&settings.get(step).stream().anyMatch(h->h.is(key));
            if(present!=biome.is(habitat))throw new IllegalStateException("Incorrect aquatic habitat "+biome.unwrapKey());
            if(present)positives++;
        }
        if(positives==0)throw new IllegalStateException("Aquatic feature has no attached biome");
    }
    private static void checkTrees(NoiseBasedChunkGenerator gen){
        var key=ResourceKey.create(Registries.PLACED_FEATURE,ResourceLocation.fromNamespaceAndPath("worldlogicterrain","ecological_trees"));
        var habitat=TagKey.create(Registries.BIOME,ResourceLocation.fromNamespaceAndPath("worldlogicterrain","tree_habitat"));
        var removed=TagKey.create(Registries.PLACED_FEATURE,ResourceLocation.fromNamespaceAndPath("worldlogicterrain","replaced_vanilla_trees"));
        int step=GenerationStep.Decoration.VEGETAL_DECORATION.ordinal();
        int positives=0,negatives=0;
        for(var biome:gen.getBiomeSource().possibleBiomes()){
            var settings=biome.value().getGenerationSettings().features();
            boolean present=settings.size()>step&&settings.get(step).stream().anyMatch(h->h.is(key));
            boolean wanted=biome.is(habitat);
            if(present!=wanted)throw new IllegalStateException("Incorrect tree habitat "+biome.unwrapKey());
            if(wanted){
                positives++;
                if(settings.get(step).stream().anyMatch(h->h.is(removed)))throw new IllegalStateException("Old tree feature not removed "+biome.unwrapKey());
            }else negatives++;
        }
        if(positives==0||negatives==0)throw new IllegalStateException("Tree biome coverage missing positive or negative control");
    }
}
