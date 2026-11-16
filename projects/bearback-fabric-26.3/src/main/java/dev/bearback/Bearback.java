package dev.bearback;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.polarbear.PolarBear;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.Heightmap;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

public class Bearback implements ModInitializer {
 public static final String MOD_ID="bearback";
 private static final ResourceKey<EntityType<?>> BEAR_KEY=ResourceKey.create(
         Registries.ENTITY_TYPE,Identifier.fromNamespaceAndPath(MOD_ID,"black_bear"));
 public static final EntityType<BlackBear> BLACK_BEAR=Registry.register(
         BuiltInRegistries.ENTITY_TYPE,BEAR_KEY,
         EntityType.Builder.<BlackBear>of(BlackBear::new,MobCategory.CREATURE)
             .sized(1.4F,1.4F).clientTrackingRange(8).build(BEAR_KEY));
 private static final ResourceKey<Item> BAGS_KEY=ResourceKey.create(
         Registries.ITEM,Identifier.fromNamespaceAndPath(MOD_ID,"saddlebags"));
 public static final Item SADDLEBAGS=Registry.register(BuiltInRegistries.ITEM,BAGS_KEY,
         new Item(new Item.Properties().setId(BAGS_KEY).stacksTo(16)));
 private static final ResourceKey<Item> EGG_KEY=ResourceKey.create(
         Registries.ITEM,Identifier.fromNamespaceAndPath(MOD_ID,"black_bear_spawn_egg"));
 public static final Item EGG=Registry.register(BuiltInRegistries.ITEM,EGG_KEY,
         new SpawnEggItem(new Item.Properties().setId(EGG_KEY).spawnEgg(BLACK_BEAR)));
 private static final Set<PolarBear> LOADED=Collections.newSetFromMap(new WeakHashMap<>());
 @Override public void onInitialize(){
    FabricDefaultAttributeRegistry.register(BLACK_BEAR,PolarBear.createAttributes());
    SpawnPlacements.register(BLACK_BEAR,SpawnPlacementTypes.ON_GROUND,Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,Animal::checkAnimalSpawnRules);
    BiomeModifications.addSpawn(BiomeSelectors.includeByKey(Biomes.FOREST,Biomes.BIRCH_FOREST,Biomes.DARK_FOREST,
            Biomes.TAIGA,Biomes.OLD_GROWTH_PINE_TAIGA,Biomes.OLD_GROWTH_SPRUCE_TAIGA),
            MobCategory.CREATURE,BLACK_BEAR,6,1,2);
    CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.SPAWN_EGGS).register(entries->entries.accept(EGG));
    CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(entries->entries.accept(SADDLEBAGS));
    UseEntityCallback.EVENT.register((player,world,hand,entity,hit)->BearEvents.interact(player,hand,entity));
    ServerEntityEvents.ENTITY_LOAD.register((entity,world)->{if(entity instanceof PolarBear b)LOADED.add(b);});
    ServerEntityEvents.ENTITY_UNLOAD.register((entity,world)->{if(entity instanceof PolarBear b)LOADED.remove(b);});
    ServerTickEvents.END_LEVEL_TICK.register(world->{
        for(PolarBear bear:LOADED.toArray(PolarBear[]::new))
            if(bear.level()==world&&bear.isAlive())BearEvents.tick(bear);
    });
    ServerLivingEntityEvents.AFTER_DEATH.register((entity,damage)->{if(entity instanceof PolarBear b)BearCargo.drop(b);});
    EntityTrackingEvents.START_TRACKING.register((entity,player)->{if(entity instanceof PolarBear b)BearNetwork.syncTo(b,player);});
    BearNetwork.register();
 }
}
