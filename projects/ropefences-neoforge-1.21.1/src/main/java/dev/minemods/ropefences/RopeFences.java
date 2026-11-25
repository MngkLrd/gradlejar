package dev.minemods.ropefences;

import dev.minemods.ropefences.block.RopeSpanBlock;
import dev.minemods.ropefences.logic.RopeInteractions;
import dev.minemods.ropefences.blockentity.RopeSpanBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(RopeFences.MOD_ID)
public final class RopeFences {
    public static final String MOD_ID = "ropefences";
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MOD_ID);
    public static final DeferredBlock<RopeSpanBlock> ROPE_SPAN = BLOCKS.register("rope_span", RopeSpanBlock::new);

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MOD_ID);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RopeSpanBlockEntity>> ROPE_ENTITY =
            BLOCK_ENTITIES.register("rope_span", () ->
                    BlockEntityType.Builder.of(RopeSpanBlockEntity::new, ROPE_SPAN.get()).build(null));

    public RopeFences(IEventBus modBus) {
        BLOCKS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        NeoForge.EVENT_BUS.register(new RopeInteractions());
    }
}
