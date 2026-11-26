package dev.minemods.fragileglass.gameplay;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;

final class GlassRules {
    private static TagKey<Block> tag(String path) {
        return TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("fragileglass", path));
    }
    static final TagKey<Block> FRAGILE = tag("fragile_blocks");
    static final TagKey<Block> PANES = tag("fragile_panes");
    static final TagKey<Block> REINFORCED = tag("reinforced");

    private GlassRules() {}

    static boolean fragile(BlockState state) {
        return state.is(FRAGILE) && !state.is(REINFORCED);
    }

    static boolean shatter(ServerLevel level, BlockPos pos, Entity source) {
        if (!level.hasChunkAt(pos)) return false;
        BlockState state = level.getBlockState(pos);
        if (!fragile(state) || state.getDestroySpeed(level, pos) < 0 || state.hasBlockEntity()) return false;

        if (source instanceof Player player) {
            if (player.isSpectator() || !player.getAbilities().mayBuild || !level.mayInteract(player, pos)) return false;
            if (NeoForge.EVENT_BUS.post(new BlockEvent.BreakEvent(level, pos, state, player)).isCanceled()) return false;
        } else if (!EventHooks.canEntityGrief(level, source)) {
            return false;
        }
        if (!level.getBlockState(pos).equals(state)) return false;
        if (!level.destroyBlock(pos, false, source)) return false;
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state),
            pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
            22, 0.28, 0.28, 0.28, 0.12);
        return true;
    }
}
