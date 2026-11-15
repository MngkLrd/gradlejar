package dev.bearback;

import com.mojang.serialization.Codec;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.PolarBear;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.common.NeoForge;

import java.util.function.Supplier;

@Mod(Bearback.MOD_ID)
public class Bearback {
    public static final String MOD_ID = "bearback";
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, MOD_ID);
    public static final DeferredHolder<EntityType<?>, EntityType<BlackBear>> BLACK_BEAR = ENTITIES.register(
            "black_bear", () -> EntityType.Builder.of(BlackBear::new, MobCategory.CREATURE)
                    .sized(1.4F, 1.4F).clientTrackingRange(8).build("bearback:black_bear"));
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD_ID);
    public static final Supplier<Item> BLACK_BEAR_EGG = ITEMS.register("black_bear_spawn_egg",
            () -> new DeferredSpawnEggItem(BLACK_BEAR, 0x26201c, 0x84684e, new Item.Properties()));
    public static final Supplier<Item> SADDLEBAGS = ITEMS.register("saddlebags",
            () -> new Item(new Item.Properties().stacksTo(16)));
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, MOD_ID);
    public static final Supplier<AttachmentType<Boolean>> SADDLED = ATTACHMENTS.register("saddled",
            () -> AttachmentType.builder(() -> false)
                    .serialize(Codec.BOOL)
                    .sync(ByteBufCodecs.BOOL).build());

    public static final Supplier<AttachmentType<Boolean>> BAGS = ATTACHMENTS.register("bags",
            () -> AttachmentType.builder(() -> false).serialize(Codec.BOOL)
                    .sync(ByteBufCodecs.BOOL).build());

    public Bearback(IEventBus bus) {
        ENTITIES.register(bus);
        ITEMS.register(bus);
        ATTACHMENTS.register(bus);
        bus.addListener(this::attributes);
        bus.addListener(this::spawns);
        bus.addListener(this::creativeTab);
        bus.addListener(this::network);
        NeoForge.EVENT_BUS.addListener(BearEvents::interact);
        NeoForge.EVENT_BUS.addListener(BearEvents::tick);
        NeoForge.EVENT_BUS.addListener(BearEvents::changeTarget);
        NeoForge.EVENT_BUS.addListener(BearCargo::onDrops);
    }

    private void attributes(EntityAttributeCreationEvent event) {
        event.put(BLACK_BEAR.get(), PolarBear.createAttributes().build());
    }

    private void spawns(RegisterSpawnPlacementsEvent event) {
        event.register(BLACK_BEAR.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Animal::checkAnimalSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    private void creativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().equals(CreativeModeTabs.SPAWN_EGGS)) {
            event.accept(BLACK_BEAR_EGG.get());
        }
    }

    private void network(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");
        registrar.playToServer(BearRidePacket.TYPE, BearRidePacket.CODEC, BearRidePacket::receive);
        registrar.playToServer(OpenCargoPacket.TYPE, OpenCargoPacket.CODEC, OpenCargoPacket::receive);
    }
}
