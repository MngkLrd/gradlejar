package dev.norsegear.registry;

import dev.norsegear.NorseGear;
import dev.norsegear.entity.ThrownWeaponEntity;
import dev.norsegear.entity.SlingStoneEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
        DeferredRegister.create(Registries.ENTITY_TYPE, NorseGear.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<ThrownWeaponEntity>> JAVELIN =
        ENTITIES.register("javelin", () -> EntityType.Builder.<ThrownWeaponEntity>of(
            ThrownWeaponEntity::new, MobCategory.MISC).sized(0.25F, 0.25F)
            .clientTrackingRange(4).updateInterval(10).build("norsegear:javelin"));
    public static final DeferredHolder<EntityType<?>, EntityType<ThrownWeaponEntity>> THROWING_AXE =
        ENTITIES.register("throwing_axe", () -> EntityType.Builder.<ThrownWeaponEntity>of(
            ThrownWeaponEntity::new, MobCategory.MISC).sized(0.25F, 0.25F)
            .clientTrackingRange(4).updateInterval(10).build("norsegear:throwing_axe"));

    public static final DeferredHolder<EntityType<?>, EntityType<SlingStoneEntity>> SLING_STONE =
        ENTITIES.register("sling_stone", () -> EntityType.Builder.<SlingStoneEntity>of(
            SlingStoneEntity::new, MobCategory.MISC).sized(0.19F, 0.19F)
            .clientTrackingRange(4).updateInterval(10).build("norsegear:sling_stone"));

    private ModEntities() {}
}
