package dev.minemods.ropefences;

import dev.minemods.ropefences.block.RopeSpanBlock;
import dev.minemods.ropefences.logic.RopeInteractions;
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

    public RopeFences(IEventBus modBus) {
        BLOCKS.register(modBus);
        NeoForge.EVENT_BUS.register(new RopeInteractions());
    }
}
