package dev.anvilrightclickrepair;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

final class RepairInteraction {
    private RepairInteraction() {}

    static void onRightClick(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND
                || !(event.getEntity() instanceof ServerPlayer player)
                || !(event.getLevel() instanceof ServerLevel level)
                || player.isSpectator() || player.isShiftKeyDown()) return;

        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);
        if (!state.is(Blocks.ANVIL) && !state.is(Blocks.CHIPPED_ANVIL)
                && !state.is(Blocks.DAMAGED_ANVIL)) return;
        if (!level.mayInteract(player, pos)) return;

        ItemStack tool = player.getMainHandItem();
        ItemStack material = player.getOffhandItem();
        if (tool.isEmpty() || material.isEmpty() || !tool.isDamaged()
                || !tool.getItem().isValidRepairItem(tool, material)) return;
        int restored = Math.min(tool.getDamageValue(), Math.max(1, tool.getMaxDamage() / 4));
        if (restored <= 0) return;

        // Only the server changes stacks, XP and block state.
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
        boolean creative = player.getAbilities().instabuild;
        if (!creative && player.experienceLevel < 1) {
            player.displayClientMessage(Component.translatable("message.anvilrightclickrepair.no_xp"), true);
            return;
        }

        tool.setDamageValue(tool.getDamageValue() - restored);
        if (!creative) {
            material.shrink(1);
            player.giveExperienceLevels(-1);
        }
        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();

        boolean destroyed = false;
        if (!creative && level.random.nextFloat() < 0.12F) {
            BlockState next = AnvilBlock.damage(state);
            if (next == null) {
                level.removeBlock(pos, false);
                destroyed = true;
            } else {
                level.setBlock(pos, next, 3);
            }
        }
        level.levelEvent(destroyed ? 1029 : 1030, pos, 0);
        level.sendParticles(ParticleTypes.CRIT, pos.getX()+0.5, pos.getY()+1.05,
                pos.getZ()+0.5, 10, 0.22, 0.12, 0.22, 0.08);
    }
}
