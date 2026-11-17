package com.kirill.lurelantern.registry;

import com.kirill.lurelantern.block.LureLanternBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Real loot tables and mineable lanterns. Never use noLootTable() here. */
public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, "lurelantern");
    public static final DeferredHolder<Block, LureLanternBlock> LURE_LANTERN =
            BLOCKS.register("lure_lantern", () -> new LureLanternBlock(lanternProperties(12)));
    public static final DeferredHolder<Block, LureLanternBlock> ADVANCED_LURE_LANTERN =
            BLOCKS.register("advanced_lure_lantern", () -> new LureLanternBlock(lanternProperties(13)));

    private ModBlocks() {}

    private static BlockBehaviour.Properties lanternProperties(int idleLight) {
        // .of() creates a loot table for our registry ID. Copying a vanilla lantern's
        // loot supplier or using .noLootTable() makes placed lanterns undroppable.
        return BlockBehaviour.Properties.of()
                .strength(0.8F)
                .sound(SoundType.LANTERN)
                .noOcclusion()
                .pushReaction(PushReaction.DESTROY)
                .lightLevel(state -> state.getValue(LureLanternBlock.ACTIVE) ? 15 : idleLight);
    }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
    }
}
