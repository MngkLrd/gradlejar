package dev.spidercentaur;

import dev.spidercentaur.entity.SpiderCentaurEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

@Mod(SpiderCentaurMod.MOD_ID)
public final class SpiderCentaurMod {
    public static final String MOD_ID = "spidercentaur";

    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(Registries.ENTITY_TYPE, MOD_ID);
    public static final DeferredHolder<EntityType<?>, EntityType<SpiderCentaurEntity>> SPIDER_CENTAUR =
            ENTITIES.register("spider_centaur",
                    () -> EntityType.Builder.of(SpiderCentaurEntity::new, MobCategory.MONSTER)
                            .sized(1.45F, 1.95F)
                            .clientTrackingRange(8)
                            .build("spidercentaur:spider_centaur"));

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD_ID);
    public static final Supplier<Item> SPAWN_EGG =
            ITEMS.register("spider_centaur_spawn_egg",
                    () -> new DeferredSpawnEggItem(SPIDER_CENTAUR, 0x211B21, 0xC4C0AD, new Item.Properties()));

    public SpiderCentaurMod(IEventBus bus) {
        ENTITIES.register(bus);
        ITEMS.register(bus);
        bus.addListener(this::attributes);
        bus.addListener(this::spawnPlacements);
        bus.addListener(this::creativeTabs);
    }

    private void attributes(EntityAttributeCreationEvent event) {
        event.put(SPIDER_CENTAUR.get(), SpiderCentaurEntity.createAttributes().build());
    }

    private void spawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(
                SPIDER_CENTAUR.get(),
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                SpiderCentaurEntity::checkSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE
        );
    }

    private void creativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().equals(CreativeModeTabs.SPAWN_EGGS)) {
            event.accept(SPAWN_EGG.get());
        }
    }
}
