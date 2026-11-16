package dev.minemods.snowleopard;
import dev.minemods.snowleopard.entity.SnowLeopardEntity;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.Heightmap;

public final class SnowLeopardMod implements ModInitializer {
    public static final String MOD_ID = "snowleopard";
    private static ResourceLocation id(String name) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, name);
    }
    public static final EntityType<SnowLeopardEntity> SNOW_LEOPARD =
        Registry.register(BuiltInRegistries.ENTITY_TYPE, id("snow_leopard"),
            EntityType.Builder.of(SnowLeopardEntity::new, MobCategory.CREATURE)
              .sized(1.2f, 1.05f).clientTrackingRange(8).build("snowleopard:snow_leopard"));
    public static final Item SPAWN_EGG =
        Registry.register(BuiltInRegistries.ITEM, id("snow_leopard_spawn_egg"),
            new SpawnEggItem(SNOW_LEOPARD, 0xD6DADE, 0x545E68, new Item.Properties()));
    @Override public void onInitialize() {
        FabricDefaultAttributeRegistry.register(SNOW_LEOPARD, SnowLeopardEntity.createAttributes());
        SpawnPlacements.register(SNOW_LEOPARD, SpawnPlacementTypes.ON_GROUND,
            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, SnowLeopardEntity::canSpawn);
        BiomeModifications.addSpawn(BiomeSelectors.includeByKey(
            Biomes.SNOWY_SLOPES, Biomes.JAGGED_PEAKS,
            Biomes.FROZEN_PEAKS, Biomes.GROVE),
            MobCategory.CREATURE, SNOW_LEOPARD, 5, 1, 1);
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.SPAWN_EGGS)
            .register(entries -> entries.accept(SPAWN_EGG));
    }
}
