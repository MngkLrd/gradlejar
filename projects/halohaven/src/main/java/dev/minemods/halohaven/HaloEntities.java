package dev.minemods.halohaven;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.Difficulty;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.ai.attributes.Attributes;
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

    public static final DeferredHolder<EntityType<?>,EntityType<EclipseRevenant>> ECLIPSE_REVENANT=
        ENTITIES.register("eclipse_revenant",()->EntityType.Builder.of(
            EclipseRevenant::new,MobCategory.MONSTER).sized(0.8f,1.45f)
            .clientTrackingRange(12).build("halohaven:eclipse_revenant"));


    /**
     * Natural biome spawns must never appear in the void or embedded in a cliff.
     * These three entities are explicitly referenced by Halo Haven biomes.
     * Direct spawn eggs and structure-set spawns remain unaffected.
     */
    public static void registerSpawnPlacements(RegisterSpawnPlacementsEvent e) {
        e.register(CLOUDHARE.get(),
            SpawnPlacementTypes.ON_GROUND,
            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            (type, level, reason, pos, random) ->
                level.getLevel().dimension().equals(HaloHaven.HEAVEN)
                && level.getBlockState(pos).isAir()
                && (level.getBlockState(pos.below()).is(HaloHaven.SKYGRASS.get())
                    || level.getBlockState(pos.below()).is(Blocks.MOSS_BLOCK)),
            RegisterSpawnPlacementsEvent.Operation.REPLACE);

        e.register(LUMEN.get(),
            SpawnPlacementTypes.ON_GROUND,
            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            (type, level, reason, pos, random) ->
                level.getLevel().dimension().equals(HaloHaven.HEAVEN)
                && level.getBlockState(pos).isAir()
                && (level.getBlockState(pos.below()).is(HaloHaven.SKYGRASS.get())
                    || level.getBlockState(pos.below()).is(Blocks.MOSS_BLOCK)
                    || level.getBlockState(pos.below()).is(Blocks.CALCITE)),
            RegisterSpawnPlacementsEvent.Operation.REPLACE);

        e.register(DUSK_WRAITH.get(),
            SpawnPlacementTypes.ON_GROUND,
            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            (type, level, reason, pos, random) ->
                level.getLevel().dimension().equals(HaloHaven.HEAVEN)
                && level.getDifficulty()!=Difficulty.PEACEFUL
                && level.getBlockState(pos).isAir()
                && (level.getBlockState(pos.below()).is(HaloHaven.MOONSTONE.get())
                    || level.getBlockState(pos.below()).is(Blocks.DEEPSLATE)
                    || level.getBlockState(pos.below()).is(Blocks.DEEPSLATE_TILES)),
            RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    public static void registerAttributes(EntityAttributeCreationEvent e) {
        e.put(SKYFOLK.get(), Villager.createAttributes().build());
        e.put(CLOUDHARE.get(), Rabbit.createAttributes().build());
        e.put(LUMEN.get(), Allay.createAttributes().build());
        e.put(DAWN_GUARDIAN.get(), IronGolem.createAttributes().build());
        e.put(DUSK_WRAITH.get(), Vex.createAttributes().build());
        e.put(ECLIPSE_REVENANT.get(),
            Vex.createAttributes()
                .add(Attributes.MAX_HEALTH,80.0)
                .add(Attributes.ATTACK_DAMAGE,8.0)
                .add(Attributes.MOVEMENT_SPEED,0.42)
                .build());
    }
}
