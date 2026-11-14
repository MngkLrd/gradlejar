package dev.minemods.halohaven;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.npc.Villager;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;

public final class HaloEntities {
    private HaloEntities() {}

    public static final DeferredRegister<EntityType<?>> ENTITIES =
        DeferredRegister.create(Registries.ENTITY_TYPE, HaloHaven.ID);

    public static final DeferredHolder<EntityType<?>, EntityType<Skyfolk>> SKYFOLK =
        ENTITIES.register("skyfolk", () -> EntityType.Builder.of(Skyfolk::new, MobCategory.MISC)
            .sized(0.6f, 1.95f).clientTrackingRange(10).build("halohaven:skyfolk"));

    public static final DeferredHolder<EntityType<?>, EntityType<Rabbit>> CLOUDHARE =
        ENTITIES.register("cloudhare", () -> EntityType.Builder.of(Rabbit::new, MobCategory.CREATURE)
            .sized(0.4f, 0.5f).clientTrackingRange(8).build("halohaven:cloudhare"));

    public static final DeferredHolder<EntityType<?>, EntityType<Allay>> LUMEN =
        ENTITIES.register("lumen", () -> EntityType.Builder.of(Allay::new, MobCategory.CREATURE)
            .sized(0.35f, 0.6f).clientTrackingRange(10).build("halohaven:lumen"));

    public static final DeferredHolder<EntityType<?>, EntityType<IronGolem>> DAWN_GUARDIAN =
        ENTITIES.register("dawn_guardian", () -> EntityType.Builder.of(IronGolem::new, MobCategory.MISC)
            .sized(1.4f, 2.7f).clientTrackingRange(10).build("halohaven:dawn_guardian"));

    public static final DeferredHolder<EntityType<?>, EntityType<Vex>> DUSK_WRAITH =
        ENTITIES.register("dusk_wraith", () -> EntityType.Builder.of(Vex::new, MobCategory.MONSTER)
            .sized(0.4f, 0.8f).clientTrackingRange(8).build("halohaven:dusk_wraith"));

    public static void registerAttributes(EntityAttributeCreationEvent e) {
        e.put(SKYFOLK.get(), Villager.createAttributes().build());
        e.put(CLOUDHARE.get(), Rabbit.createAttributes().build());
        e.put(LUMEN.get(), Allay.createAttributes().build());
        e.put(DAWN_GUARDIAN.get(), IronGolem.createAttributes().build());
        e.put(DUSK_WRAITH.get(), Vex.createAttributes().build());
    }
}
