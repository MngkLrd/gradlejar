package dev.minemods.worldlogic;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.*;
public final class Content {
 public static final DeferredRegister<net.minecraft.world.level.block.Block> BLOCKS=DeferredRegister.create(ForgeRegistries.BLOCKS,"worldlogic");
 public static final RegistryObject<net.minecraft.world.level.block.Block> TORCH=BLOCKS.register("extinguished_torch",()->new net.minecraft.world.level.block.TorchBlock(net.minecraft.core.particles.ParticleTypes.SMOKE,net.minecraft.world.level.block.state.BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.TORCH).lightLevel(s->0)));
 public static final RegistryObject<net.minecraft.world.level.block.Block> WALL_TORCH=BLOCKS.register("extinguished_wall_torch",()->new net.minecraft.world.level.block.WallTorchBlock(net.minecraft.core.particles.ParticleTypes.SMOKE,net.minecraft.world.level.block.state.BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.WALL_TORCH).lightLevel(s->0)));
 public static final DeferredRegister<net.minecraft.core.particles.ParticleType<?>> PARTICLES=DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES,"worldlogic");
 public static final RegistryObject<net.minecraft.core.particles.SimpleParticleType> FOOTPRINT=PARTICLES.register("footprint",()->new net.minecraft.core.particles.SimpleParticleType(false));
 public static final DeferredRegister<Item> ITEMS=DeferredRegister.create(ForgeRegistries.ITEMS,"worldlogic");
 public static final RegistryObject<Item> SPOILED=ITEMS.register("spoiled_food",()->new Item(new Item.Properties().food(new FoodProperties.Builder().nutrition(1).saturationModifier(.1f).build())));
}
