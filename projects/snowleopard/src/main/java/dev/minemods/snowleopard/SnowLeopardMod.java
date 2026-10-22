package dev.minemods.snowleopard;

import dev.minemods.snowleopard.entity.SnowLeopardEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import java.util.function.Supplier;

@Mod(SnowLeopardMod.MOD_ID)
public final class SnowLeopardMod {
    public static final String MOD_ID = "snowleopard";
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, MOD_ID);
    public static final Supplier<EntityType<SnowLeopardEntity>> SNOW_LEOPARD = ENTITIES.register(
        "snow_leopard",
        () -> EntityType.Builder.of(SnowLeopardEntity::new, MobCategory.CREATURE)
            .sized(1.2f, 1.05f).clientTrackingRange(8).build("snowleopard:snow_leopard")
    );
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD_ID);
    public static final Supplier<DeferredSpawnEggItem> SPAWN_EGG = ITEMS.register(
        "snow_leopard_spawn_egg",
        () -> new DeferredSpawnEggItem(SNOW_LEOPARD, 0xD6DADE, 0x545E68, new Item.Properties())
    );

    public SnowLeopardMod(IEventBus bus) {
        ENTITIES.register(bus);
        ITEMS.register(bus);
        bus.addListener((EntityAttributeCreationEvent event) ->
            event.put(SNOW_LEOPARD.get(), SnowLeopardEntity.createAttributes().build()));
        bus.addListener((RegisterSpawnPlacementsEvent event) ->
            event.register(SNOW_LEOPARD.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                SnowLeopardEntity::canSpawn, RegisterSpawnPlacementsEvent.Operation.REPLACE));
        bus.addListener((BuildCreativeModeTabContentsEvent event) -> {
            if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
                event.accept(SPAWN_EGG.get());
            }
        });
    }
}
