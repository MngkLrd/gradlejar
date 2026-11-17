package com.kirill.lurelantern.logic;

import com.kirill.lurelantern.block.LureLanternBlock;
import com.kirill.lurelantern.blockentity.LureLanternBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

@EventBusSubscriber(modid = "lurelantern")
public final class SafeLanternDrops {
    private SafeLanternDrops() { }

    @SubscribeEvent
    public static void onDrops(BlockDropsEvent event) {
        BlockState state = event.getState();
        if (!(state.getBlock() instanceof LureLanternBlock)) return;
        if (event.getBreaker() instanceof Player player && player.isCreative()) return;

        BlockPos pos = event.getPos();
        ItemStack stack = new ItemStack(state.getBlock());
        if (LureLanternBlock.isAdvancedLantern(state)
                && event.getBlockEntity() instanceof LureLanternBlockEntity lantern)
            lantern.saveCapturedMobToStack(stack);

        event.getDrops().removeIf(drop -> drop.getItem().is(state.getBlock().asItem()));
        event.getDrops().add(new ItemEntity(event.getLevel(),
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack));
    }

    @SubscribeEvent
    public static void onCreativeBreak(BlockEvent.BreakEvent event) {
        if (event.isCanceled() || !event.getPlayer().isCreative()) return;
        BlockState state = event.getState();
        if (!(state.getBlock() instanceof LureLanternBlock) || !LureLanternBlock.isAdvancedLantern(state)) return;
        if (!(event.getLevel().getBlockEntity(event.getPos()) instanceof LureLanternBlockEntity lantern)
                || !lantern.hasCapturedMob()) return;
        Player player = event.getPlayer();
        if (!player.level().isClientSide()) {
            ItemStack saved = new ItemStack(state.getBlock());
            lantern.saveCapturedMobToStack(saved);
            if (!player.getInventory().add(saved)) player.drop(saved, false);
        }
    }
}
