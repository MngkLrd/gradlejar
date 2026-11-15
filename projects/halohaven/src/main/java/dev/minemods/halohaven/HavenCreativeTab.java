package dev.minemods.halohaven;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** All Halo Haven blocks, resources and spawners live in their own Creative tab. */
public final class HavenCreativeTab {
    private HavenCreativeTab(){}

    private static final DeferredRegister<CreativeModeTab> TABS =
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, HaloHaven.ID);

    public static final DeferredHolder<CreativeModeTab,CreativeModeTab> CELESTIAL =
        TABS.register("heaven", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.halohaven.heaven"))
            .withTabsBefore(CreativeModeTabs.SPAWN_EGGS)
            .icon(() -> HaloHaven.SKYSEED.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                // Entry, transport and progression first.
                output.accept(HaloHaven.SKYSEED.get());
                output.accept(HaloHaven.ASCENSION_SIGIL.get());
                output.accept(HaloHaven.SKY_COMPASS.get());
                output.accept(HaloHaven.AETHER_FEATHER.get());
                output.accept(HaloHaven.AETHER_TONIC.get());
                output.accept(HaloHaven.SUNWARD_RELIC.get());
                output.accept(HaloHaven.SKYBERRY.get());
                output.accept(HaloHaven.STARDUST.get());
                output.accept(HaloHaven.SUNSHARD.get());
                output.accept(HaloHaven.ECLIPSE_SEAL.get());
                // Building families.
                output.accept(HaloHaven.CLOUDSTONE_ITEM.get());
                output.accept(HaloHaven.SKYGRASS_ITEM.get());
                output.accept(HaloHaven.PEARL_BRICKS_ITEM.get());
                output.accept(HaloHaven.PEARL_STAIRS_ITEM.get());
                output.accept(HaloHaven.PEARL_SLAB_ITEM.get());
                output.accept(HaloHaven.SUNGOLD_TILES_ITEM.get());
                output.accept(HaloHaven.SUNGOLD_STAIRS_ITEM.get());
                output.accept(HaloHaven.SUNGOLD_SLAB_ITEM.get());
                output.accept(HaloHaven.AZURE_PLANKS_ITEM.get());
                output.accept(HaloHaven.AZURE_FENCE_ITEM.get());
                output.accept(HaloHaven.GILDED_PILLAR_ITEM.get());
                output.accept(HaloHaven.CELESTIAL_MOSAIC_ITEM.get());
                output.accept(HaloHaven.MOONSTONE_ITEM.get());
                output.accept(HaloHaven.AURORA_GLASS_ITEM.get());
                output.accept(HaloHaven.DAWN_GLASS_ITEM.get());
                output.accept(HaloHaven.STARSHADE_ITEM.get());
                output.accept(HaloHaven.CLOUD_LANTERN_ITEM.get());
                output.accept(HaloHaven.PILGRIM_ALTAR_ITEM.get());
                output.accept(HaloHaven.WAYSTONE_ITEM.get());
                output.accept(HaloHaven.STAR_ORE_ITEM.get());
                output.accept(HaloHaven.SKY_LEAVES_ITEM.get());
                output.accept(HaloHaven.CELESTIAL_BLOOM_ITEM.get());
                // Biome Art Expansion, six material families and their flora.
                output.accept(HavenBiomeBlocks.DAWNSTONE_ITEM.get());
                output.accept(HavenBiomeBlocks.SUNLIT_LOAM_ITEM.get());
                output.accept(HavenBiomeBlocks.SUNPETAL_ITEM.get());
                output.accept(HavenBiomeBlocks.CHALKSTONE_ITEM.get());
                output.accept(HavenBiomeBlocks.CHALK_STRATA_ITEM.get());
                output.accept(HavenBiomeBlocks.CHALK_BLOSSOM_ITEM.get());
                output.accept(HavenBiomeBlocks.CANOPY_LOAM_ITEM.get());
                output.accept(HavenBiomeBlocks.ANCIENT_BARK_ITEM.get());
                output.accept(HavenBiomeBlocks.CANOPY_FERN_ITEM.get());
                output.accept(HavenBiomeBlocks.RESONANT_SHALE_ITEM.get());
                output.accept(HavenBiomeBlocks.ECHO_CRYSTAL_ITEM.get());
                output.accept(HavenBiomeBlocks.GORGE_THISTLE_ITEM.get());
                output.accept(HavenBiomeBlocks.CHOIRSTONE_ITEM.get());
                output.accept(HavenBiomeBlocks.CHOIR_TILES_ITEM.get());
                output.accept(HavenBiomeBlocks.MOURNING_BLOOM_ITEM.get());
                output.accept(HavenBiomeBlocks.VEILSTONE_ITEM.get());
                output.accept(HavenBiomeBlocks.UMBRAL_SILT_ITEM.get());
                output.accept(HavenBiomeBlocks.VEIL_LICHEN_ITEM.get());
                // Each new creature is independently summonable from the same tab.
                output.accept(HaloHaven.SKYFOLK_EGG.get());
                output.accept(HaloHaven.CLOUDHARE_EGG.get());
                output.accept(HaloHaven.LUMEN_EGG.get());
                output.accept(HaloHaven.GUARDIAN_EGG.get());
                output.accept(HaloHaven.WRAITH_EGG.get());
                output.accept(HaloHaven.ECLIPSE_EGG.get());
            }).build());

    public static void register(IEventBus bus){HavenBiomeBlocks.register(bus);TABS.register(bus);}
}
