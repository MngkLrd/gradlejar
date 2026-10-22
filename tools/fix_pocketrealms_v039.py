"""NeoForge 1.21.1 compatibility fixes applied after restoring the archived sources."""
from pathlib import Path

base = Path(__file__).resolve().parents[1] / "projects/pocketrealms/src/main/java/com/kirill/pocketrealms"
entities = base / "registry/ModEntities.java"
entities.write_text("""package com.kirill.pocketrealms.registry;

import com.kirill.pocketrealms.PocketRealms;
import com.kirill.pocketrealms.entity.CorruptedSkeleton;
import com.kirill.pocketrealms.entity.CorruptedZombie;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredRegister;
import java.util.function.Supplier;

public final class ModEntities {
    private ModEntities() {}
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(Registries.ENTITY_TYPE, PocketRealms.MOD_ID);

    public static final Supplier<EntityType<CorruptedZombie>> CORRUPTED_ZOMBIE = ENTITIES.register(
            "corrupted_zombie", () -> EntityType.Builder.of(CorruptedZombie::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.95F).clientTrackingRange(8).build("corrupted_zombie"));

    public static final Supplier<EntityType<CorruptedSkeleton>> CORRUPTED_SKELETON = ENTITIES.register(
            "corrupted_skeleton", () -> EntityType.Builder.of(CorruptedSkeleton::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.99F).clientTrackingRange(8).build("corrupted_skeleton"));
}
""", encoding="utf8")

living = base / "world/LivingLabyrinth.java"
s = living.read_text(encoding="utf8")
s = s.replace("SoundEvents.SCULK_SENSOR_CLICKING", "SoundEvents.SCULK_BLOCK_BREAK")
living.write_text(s, encoding="utf8")
print("Applied Minecraft 1.21.1 entity registry and sound fixes")
