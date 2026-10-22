package dev.kirill.embershelter.util;
import dev.kirill.embershelter.config.ServerConfig;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
public final class BlockReplaceRules {
    private BlockReplaceRules(){}
    public static boolean replaceable(BlockState s) {
        if(s.isAir()) return true;
        if(s.hasBlockEntity() || !s.getFluidState().isEmpty()) return false;
        if(ServerConfig.CLEAR_LOGS.get() && s.is(BlockTags.LOGS)) return true;
        if(ServerConfig.CLEAR_LEAVES.get() && s.is(BlockTags.LEAVES)) return true;
        if(ServerConfig.ALLOW_GRASS.get() && (
            s.is(Blocks.SHORT_GRASS) || s.is(Blocks.TALL_GRASS) ||
            s.is(Blocks.FERN) || s.is(Blocks.LARGE_FERN) ||
            s.is(Blocks.DEAD_BUSH) || s.is(Blocks.SNOW) ||
            s.is(Blocks.VINE) || s.is(BlockTags.SAPLINGS))) return true;
        return ServerConfig.ALLOW_FLOWERS.get() && s.is(BlockTags.FLOWERS);
    }
}
