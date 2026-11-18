package com.kirill.towerveil.registry;

import com.kirill.towerveil.TowerVeil;
import com.kirill.towerveil.block.EndlessFarmlandBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(TowerVeil.MOD_ID);

    // Visually vanilla farmland, but it will never dry out.
    public static final DeferredBlock<EndlessFarmlandBlock> ENDLESS_FARMLAND = BLOCKS.register(
            "endless_farmland",
            () -> new EndlessFarmlandBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.FARMLAND))
    );

    private ModBlocks() { }
}
