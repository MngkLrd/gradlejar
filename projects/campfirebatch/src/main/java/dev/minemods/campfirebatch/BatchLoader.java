package dev.minemods.campfirebatch;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;

final class BatchLoader {
    private BatchLoader() {}

    static int load(ServerLevel level, BlockPos pos, CampfireBlockEntity fire,
                    Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        int limit = Math.min(SlotScanner.freeSlots(fire), held.getCount());
        int placed = 0;
        for (int i = 0; i < limit; i++) {
            // Recheck actual state and hand after every insertion: mod callbacks can
            // replace a block or hand stack even within a single server-thread event.
            if (fire.isRemoved() || level.getBlockEntity(pos) != fire
                    || !InteractionHandler.isLitVanillaCampfire(level.getBlockState(pos))
                    || player.getItemInHand(hand) != held || held.isEmpty()) break;
            var cookTime = RecipeValidator.cookingTime(fire, held);
            if (cookTime.isEmpty()) break;
            // Vanilla owns slot selection, stack consumption (including creative),
            // components, independent progress, save/update packets and game events.
            if (!fire.placeFood(player, held, cookTime.getAsInt())) break;
            placed++;
        }
        return placed;
    }
}
