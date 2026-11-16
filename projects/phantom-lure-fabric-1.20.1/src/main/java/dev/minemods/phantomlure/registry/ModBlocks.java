package dev.minemods.phantomlure.registry;
import dev.minemods.phantomlure.block.PhantomLureBlock;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
public final class ModBlocks {
 private static final ResourceLocation ID=new ResourceLocation("phantomlure","phantom_lure");
 public static final PhantomLureBlock PHANTOM_LURE=Registry.register(BuiltInRegistries.BLOCK,ID,
     new PhantomLureBlock(BlockBehaviour.Properties.of().strength(1.8F)
       .sound(SoundType.AMETHYST).noOcclusion().lightLevel(s->s.getValue(PhantomLureBlock.CHARGED)?6:2)));
 public static final Item LURE_ITEM=Registry.register(BuiltInRegistries.ITEM,ID,
     new BlockItem(PHANTOM_LURE,new Item.Properties()));
 public static void initialize() {}
 private ModBlocks() {}
}
