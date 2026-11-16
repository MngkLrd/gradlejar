package dev.minemods.phantomlure.registry;
import dev.minemods.phantomlure.blockentity.PhantomLureBlockEntity;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntityType;
public final class ModBlockEntities {
 public static final BlockEntityType<PhantomLureBlockEntity> LURE=Registry.register(
     BuiltInRegistries.BLOCK_ENTITY_TYPE,ResourceLocation.fromNamespaceAndPath("phantomlure","phantom_lure"),
     BlockEntityType.Builder.of(PhantomLureBlockEntity::new,ModBlocks.PHANTOM_LURE).build(null));
 public static void initialize() {}
 private ModBlockEntities() {}
}
