package dev.bearback;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.PolarBear;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

@Mod(Bearback.MOD_ID)
public class Bearback {
    public static final String MOD_ID = "bearback";
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MOD_ID);
    public static final RegistryObject<EntityType<BlackBear>> BLACK_BEAR = ENTITIES.register(
            "black_bear", () -> EntityType.Builder.of(BlackBear::new, MobCategory.CREATURE)
                    .sized(1.4F, 1.4F).clientTrackingRange(8).build("bearback:black_bear"));
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID);
    public static final RegistryObject<Item> BLACK_BEAR_EGG = ITEMS.register("black_bear_spawn_egg",
            () -> new ForgeSpawnEggItem(BLACK_BEAR, 0x242528, 0xb9a184, new Item.Properties()));

    public static final RegistryObject<Item> SADDLEBAGS = ITEMS.register("saddlebags",
            () -> new Item(new Item.Properties().stacksTo(16)));

    public Bearback() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ENTITIES.register(bus);
        ITEMS.register(bus);
        bus.addListener(Bearback::attributes);
        bus.addListener(Bearback::spawns);
        bus.addListener(Bearback::creative);
        BearNetwork.init();
        MinecraftForge.EVENT_BUS.addListener(BearEvents::interact);
        MinecraftForge.EVENT_BUS.addListener(BearEvents::livingTick);
        MinecraftForge.EVENT_BUS.addListener(BearEvents::changeTarget);
        MinecraftForge.EVENT_BUS.addListener(BearEvents::track);
        MinecraftForge.EVENT_BUS.addListener(BearCargo::onDrops);
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> dev.bearback.client.ClientHooks.init(bus));
    }

    private static void attributes(EntityAttributeCreationEvent e) {
        e.put(BLACK_BEAR.get(), PolarBear.createAttributes().build());
    }

    private static void spawns(SpawnPlacementRegisterEvent e) {
        e.register(BLACK_BEAR.get(), SpawnPlacements.Type.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Animal::checkAnimalSpawnRules,
                SpawnPlacementRegisterEvent.Operation.REPLACE);
    }

    private static void creative(BuildCreativeModeTabContentsEvent e) {
        if (e.getTabKey().equals(CreativeModeTabs.SPAWN_EGGS)) e.accept(BLACK_BEAR_EGG.get());
        if (e.getTabKey().equals(CreativeModeTabs.TOOLS_AND_UTILITIES)) e.accept(SADDLEBAGS.get());
    }
}
