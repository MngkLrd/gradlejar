package dev.norsegear.registry;

import dev.norsegear.NorseGear;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Naturally occurring muddy iron nodules. Not a craftable or placeable item. */
public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(NorseGear.MODID);
    public static final DeferredBlock<Block> BOG_IRON_DEPOSIT = BLOCKS.registerSimpleBlock(
        "bog_iron_deposit",
        BlockBehaviour.Properties.of().strength(1.6F, 2.0F)
            .sound(SoundType.GRAVEL).requiresCorrectToolForDrops());

    private ModBlocks() {}
}
