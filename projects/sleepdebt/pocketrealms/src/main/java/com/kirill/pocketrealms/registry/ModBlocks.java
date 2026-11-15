package com.kirill.pocketrealms.registry;

import com.kirill.pocketrealms.PocketRealms;
import com.kirill.pocketrealms.block.PocketDoorBlock;
import com.kirill.pocketrealms.block.ReturnPortalBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    private ModBlocks() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(PocketRealms.MOD_ID);

    public static final DeferredBlock<Block> POCKET_DOOR = BLOCKS.register("pocket_door",
            () -> new PocketDoorBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OBSIDIAN)
                    .strength(8.0F, 1200.0F)
                    .sound(SoundType.NETHERITE_BLOCK)
                    .noOcclusion()));

    public static final DeferredBlock<Block> RETURN_PORTAL = BLOCKS.register("return_portal",
            () -> new ReturnPortalBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.CRYING_OBSIDIAN)
                    .strength(-1.0F, 3600000.0F)
                    .lightLevel(state -> 8)
                    .sound(SoundType.AMETHYST)
                    .noOcclusion()));
}
