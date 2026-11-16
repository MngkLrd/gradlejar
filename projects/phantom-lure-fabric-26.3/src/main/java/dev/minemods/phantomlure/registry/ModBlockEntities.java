package dev.minemods.phantomlure.registry;
import dev.minemods.phantomlure.blockentity.PhantomLureBlockEntity;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntityType;
import java.util.Set;
public final class ModBlockEntities {
 public static final BlockEntityType<PhantomLureBlockEntity> LURE=Registry.register(
     BuiltInRegistries.BLOCK_ENTITY_TYPE,
     ResourceKey.create(Registries.BLOCK_ENTITY_TYPE,Identifier.fromNamespaceAndPath("phantomlure","phantom_lure")),
     new BlockEntityType<>(PhantomLureBlockEntity::new,Set.of(ModBlocks.PHANTOM_LURE)));
 public static void initialize() {}
 private ModBlockEntities() {}
}
