package dev.minemods.snowleopard;
import dev.minemods.snowleopard.entity.SnowLeopardEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

@Mod(SnowLeopardMod.MOD_ID)
public final class SnowLeopardMod {
    public static final String MOD_ID = "snowleopard";
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MOD_ID);
    public static final RegistryObject<EntityType<SnowLeopardEntity>> SNOW_LEOPARD = ENTITIES.register("snow_leopard",
        () -> EntityType.Builder.of(SnowLeopardEntity::new, MobCategory.CREATURE)
            .sized(1.2f, 1.05f).clientTrackingRange(8).build("snowleopard:snow_leopard"));
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID);
    public static final RegistryObject<Item> SPAWN_EGG = ITEMS.register("snow_leopard_spawn_egg",
        () -> new ForgeSpawnEggItem(SNOW_LEOPARD, 0xD6DADE, 0x545E68, new Item.Properties()));

    public SnowLeopardMod(){
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ENTITIES.register(bus);
        ITEMS.register(bus);
        bus.addListener((EntityAttributeCreationEvent e) ->
            e.put(SNOW_LEOPARD.get(), SnowLeopardEntity.createAttributes().build()));
        bus.addListener((SpawnPlacementRegisterEvent e) ->
            e.register(SNOW_LEOPARD.get(), SpawnPlacements.Type.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                SnowLeopardEntity::canSpawn, SpawnPlacementRegisterEvent.Operation.REPLACE));
        bus.addListener((BuildCreativeModeTabContentsEvent e) -> {
            if(e.getTabKey().equals(CreativeModeTabs.SPAWN_EGGS)) e.accept(SPAWN_EGG.get());
        });
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
            dev.minemods.snowleopard.client.SnowLeopardClient.register(bus));
    }
}
