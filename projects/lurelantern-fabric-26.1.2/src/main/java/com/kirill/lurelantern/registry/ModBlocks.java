package com.kirill.lurelantern.registry;
import com.kirill.lurelantern.block.LureLanternBlock;
import com.kirill.lurelantern.item.LureLanternBlockItem;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import java.util.function.Supplier;
public final class ModBlocks {
 private static final Identifier BASIC_ID=Identifier.fromNamespaceAndPath("lurelantern","lure_lantern");
 private static final Identifier ADV_ID=Identifier.fromNamespaceAndPath("lurelantern","advanced_lure_lantern");
 private static final ResourceKey<Block> BK=ResourceKey.create(Registries.BLOCK,BASIC_ID),AK=ResourceKey.create(Registries.BLOCK,ADV_ID);
 private static final ResourceKey<Item> BIK=ResourceKey.create(Registries.ITEM,BASIC_ID),AIK=ResourceKey.create(Registries.ITEM,ADV_ID);
 private static final LureLanternBlock BASIC_BLOCK=Registry.register(BuiltInRegistries.BLOCK,BK,
    new LureLanternBlock(BlockBehaviour.Properties.of().setId(BK).strength(.8f).sound(SoundType.LANTERN)
     .noOcclusion().lightLevel(s->s.getValue(LureLanternBlock.ACTIVE)?15:12)));
 private static final LureLanternBlock ADV_BLOCK=Registry.register(BuiltInRegistries.BLOCK,AK,
    new LureLanternBlock(BlockBehaviour.Properties.of().setId(AK).strength(.8f).sound(SoundType.LANTERN)
     .noOcclusion().lightLevel(s->s.getValue(LureLanternBlock.ACTIVE)?15:13)));
 private static final LureLanternBlockItem BASIC_I=Registry.register(BuiltInRegistries.ITEM,BIK,
    new LureLanternBlockItem(BASIC_BLOCK,new Item.Properties().setId(BIK).stacksTo(1).useBlockDescriptionPrefix(),false));
 private static final LureLanternBlockItem ADV_I=Registry.register(BuiltInRegistries.ITEM,AIK,
    new LureLanternBlockItem(ADV_BLOCK,new Item.Properties().setId(AIK).stacksTo(1).useBlockDescriptionPrefix(),true));
 public static final Supplier<LureLanternBlock> BASIC=()->BASIC_BLOCK,ADVANCED=()->ADV_BLOCK;
 public static final Supplier<LureLanternBlockItem> BASIC_ITEM=()->BASIC_I,ADVANCED_ITEM=()->ADV_I;
 public static void initialize(){}
}
