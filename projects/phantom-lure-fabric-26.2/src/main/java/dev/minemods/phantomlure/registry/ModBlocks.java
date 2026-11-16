package dev.minemods.phantomlure.registry;
import dev.minemods.phantomlure.block.PhantomLureBlock;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
public final class ModBlocks {
 private static final Identifier ID=Identifier.fromNamespaceAndPath("phantomlure","phantom_lure");
 private static final ResourceKey<Block> BLOCK_KEY=ResourceKey.create(Registries.BLOCK,ID);
 private static final ResourceKey<Item> ITEM_KEY=ResourceKey.create(Registries.ITEM,ID);
 public static final PhantomLureBlock PHANTOM_LURE=Registry.register(BuiltInRegistries.BLOCK,BLOCK_KEY,
     new PhantomLureBlock(BlockBehaviour.Properties.of().setId(BLOCK_KEY).strength(1.8F)
       .sound(SoundType.AMETHYST).noOcclusion()
       .lightLevel(s->s.getValue(PhantomLureBlock.CHARGED)?6:2)));
 public static final Item LURE_ITEM=Registry.register(BuiltInRegistries.ITEM,ITEM_KEY,
     new BlockItem(PHANTOM_LURE,new Item.Properties().setId(ITEM_KEY).useBlockDescriptionPrefix()));
 public static void initialize() {}
 private ModBlocks() {}
}
