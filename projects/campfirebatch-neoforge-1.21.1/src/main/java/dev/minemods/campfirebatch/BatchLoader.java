package dev.minemods.campfirebatch;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

final class BatchLoader {
    private BatchLoader() {}

    private static int occupied(CampfireBlockEntity fire) {
        int n = 0;
        for (ItemStack stack : fire.getItems()) if (!stack.isEmpty()) n++;
        return n;
    }

    static InteractionResult interact(Player player, Level level, InteractionHand hand, BlockHitResult hit) {
        if (player.isSpectator() || !player.isSecondaryUseActive()) return InteractionResult.PASS;
        ItemStack held = player.getItemInHand(hand);
        if (held.isEmpty()) return InteractionResult.PASS;
        BlockPos pos = hit.getBlockPos();
        BlockState state = level.getBlockState(pos);
        if (!state.is(Blocks.CAMPFIRE) && !state.is(Blocks.SOUL_CAMPFIRE)) return InteractionResult.PASS;
        if (!(level.getBlockEntity(pos) instanceof CampfireBlockEntity fire)) return InteractionResult.PASS;
        int limit = Math.min(4 - occupied(fire), held.getCount());
        if (limit <= 0) return InteractionResult.PASS;

        // Vanilla's client-side interaction checks its own recipe data and does not insert.
        if (!(level instanceof ServerLevel)) {
            var result = state.useItemOn(held, level, player, hand, hit);
            return result.consumesAction() ? InteractionResult.SUCCESS : InteractionResult.PASS;
        }

        int inserted = 0;
        for (int i = 0; i < limit; i++) {
            if (held.isEmpty() || player.getItemInHand(hand) != held
                    || fire.isRemoved() || level.getBlockEntity(pos) != fire
                    || level.getBlockState(pos) != state) break;
            int before = occupied(fire);
            // Vanilla decides recipe validity, fire state, consumption and cook time.
            // It synchronizes and persists the slots itself; never shrink a second time.
            state.useItemOn(held, level, player, hand, hit);
            if (occupied(fire) != before + 1) break;
            inserted++;
        }
        return inserted > 0 ? InteractionResult.CONSUME : InteractionResult.PASS;
    }
}
