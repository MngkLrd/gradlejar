package dev.minemods.snowleopard;

import dev.minemods.snowleopard.entity.SnowLeopardEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import java.util.function.Supplier;

@Mod(SnowLeopardMod.MOD_ID)
public final class SnowLeopardMod {
    public static final String MOD_ID = "snowleopard";
    public static final DeferredRegister.Entities ENTITIES = DeferredRegister.createEntities(MOD_ID);
    public static final DeferredHolder<EntityType<?>, EntityType<SnowLeopardEntity>> SNOW_LEOPARD =
        ENTITIES.registerEntityType("snow_leopard", SnowLeopardEntity::new, MobCategory.CREATURE,
            b -> b.sized(1.2f, 1.05f).clientTrackingRange(8));

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD_ID);
    public static final Supplier<Item> SPAWN_EGG = ITEMS.register("snow_leopard_spawn_egg",
        () -> new SpawnEggItem(new Item.Properties().spawnEgg(SNOW_LEOPARD.get())));

    public SnowLeopardMod(IEventBus bus) {
        ENTITIES.register(bus);
        ITEMS.register(bus);
        bus.addListener((EntityAttributeCreationEvent event) ->
            event.put(SNOW_LEOPARD.get(), SnowLeopardEntity.createAttributes().build()));
        bus.addListener((RegisterSpawnPlacementsEvent event) ->
            event.register(SNOW_LEOPARD.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Animal::checkAnimalSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE));
        bus.addListener((BuildCreativeModeTabContentsEvent event) -> {
            if (event.getTabKey().equals(CreativeModeTabs.SPAWN_EGGS)) event.accept(SPAWN_EGG.get());
        });
        if (net.neoforged.fml.loading.FMLEnvironment.getDist() == net.neoforged.api.distmarker.Dist.CLIENT)
            dev.minemods.snowleopard.client.SnowLeopardClient.register(bus);
    }
}
