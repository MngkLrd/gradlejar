package dev.minemods.campfirebatch;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.UseItemOnBlockEvent;

final class InteractionHandler {
    private InteractionHandler() {}

    // Both sides: permit vanilla's block phase despite sneaking, only for a valid input.
    static void allowSneakingUse(PlayerInteractEvent.RightClickBlock event) {
        if (event.isCanceled() || event.getUseBlock() == TriState.FALSE) return;
        Player player = event.getEntity();
        ItemStack held = player.getItemInHand(event.getHand());
        CampfireBlockEntity fire = eligible(player, event.getLevel(), event.getPos(), held);
        if (fire != null && RecipeValidator.cookingTime(fire, held).isPresent()) {
            event.setUseBlock(TriState.TRUE);
        }
    }

    // Server only: the normal item-before-block hook has already had its opportunity.
    // Let the client follow vanilla's ordinary successful block interaction.
    static void loadBatch(UseItemOnBlockEvent event) {
        if (event.isCanceled() || event.getUsePhase() != UseItemOnBlockEvent.UsePhase.BLOCK
                || !(event.getLevel() instanceof ServerLevel level)) return;
        Player player = event.getPlayer();
        if (player == null) return;
        ItemStack held = player.getItemInHand(event.getHand());
        CampfireBlockEntity fire = eligible(player, level, event.getPos(), held);
        if (fire == null) return;
        int placed = BatchLoader.load(level, event.getPos(), fire, player, event.getHand());
        if (placed > 0) {
            player.awardStat(Stats.INTERACT_WITH_CAMPFIRE);
            event.cancelWithResult(ItemInteractionResult.SUCCESS);
        }
    }

    static boolean isLitVanillaCampfire(BlockState state) {
        return (state.is(Blocks.CAMPFIRE) || state.is(Blocks.SOUL_CAMPFIRE))
                && state.getValue(CampfireBlock.LIT)
                && !state.getValue(CampfireBlock.WATERLOGGED);
    }

    private static CampfireBlockEntity eligible(Player player, Level level, BlockPos pos, ItemStack held) {
        if (!player.isSecondaryUseActive() || player.isSpectator() || held.isEmpty()
                || !isLitVanillaCampfire(level.getBlockState(pos))) return null;
        if (level.getBlockEntity(pos) instanceof CampfireBlockEntity fire
                && SlotScanner.freeSlots(fire) > 0) return fire;
        return null;
    }
}
