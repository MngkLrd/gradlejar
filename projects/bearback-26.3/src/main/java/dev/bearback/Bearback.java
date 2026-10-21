package dev.bearback;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.polarbear.PolarBear;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.minecraft.world.item.SpawnEggItem;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

@Mod(Bearback.MOD_ID)
public class Bearback {
    public static final String MOD_ID="bearback";
    public static final DeferredRegister.Entities ENTITIES=DeferredRegister.createEntities(MOD_ID);
    public static final DeferredHolder<EntityType<?>,EntityType<BlackBear>> BLACK_BEAR=
            ENTITIES.registerEntityType("black_bear",BlackBear::new,MobCategory.CREATURE,
                    builder->builder.sized(1.4F,1.4F).clientTrackingRange(8));
    public static final DeferredRegister.Items ITEMS=DeferredRegister.createItems(MOD_ID);
    public static final Supplier<Item> BLACK_BEAR_EGG=ITEMS.register("black_bear_spawn_egg",
            ()->new SpawnEggItem(new Item.Properties().spawnEgg(BLACK_BEAR.get())));

    public Bearback(IEventBus bus){
        ENTITIES.register(bus);ITEMS.register(bus);
        bus.addListener(Bearback::attributes);
        bus.addListener(Bearback::spawns);
        bus.addListener(Bearback::creative);
        bus.addListener(BearNetwork::register);
        NeoForge.EVENT_BUS.addListener(BearEvents::interact);
        NeoForge.EVENT_BUS.addListener(BearEvents::tick);
        NeoForge.EVENT_BUS.addListener(BearEvents::changeTarget);
        NeoForge.EVENT_BUS.addListener(BearEvents::track);
        if(net.neoforged.fml.loading.FMLEnvironment.getDist()==net.neoforged.api.distmarker.Dist.CLIENT)
            dev.bearback.client.ClientHooks.register(bus);
    }

    private static void attributes(EntityAttributeCreationEvent e){
        e.put(BLACK_BEAR.get(),PolarBear.createAttributes().build());
    }
    private static void spawns(RegisterSpawnPlacementsEvent e){
        e.register(BLACK_BEAR.get(),SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,Animal::checkAnimalSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }
    private static void creative(BuildCreativeModeTabContentsEvent e){
        if(e.getTabKey().equals(CreativeModeTabs.SPAWN_EGGS))e.accept(BLACK_BEAR_EGG.get());
    }
}
