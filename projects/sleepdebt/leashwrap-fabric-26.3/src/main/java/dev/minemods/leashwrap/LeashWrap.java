package dev.minemods.leashwrap;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Items;

public final class LeashWrap implements ModInitializer {
    @Override
    public void onInitialize() {
        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            if (!player.getItemInHand(hand).is(Items.LEAD)) return InteractionResult.PASS;
            if (!AnchorLogic.canAnchor(world, hit.getBlockPos())) return InteractionResult.PASS;
            if (world instanceof ServerLevel level
                && !AnchorLogic.interact(level, hit.getBlockPos(), player)) {
                return InteractionResult.PASS;
            }
            return InteractionResult.SUCCESS;
        });
    }
}
