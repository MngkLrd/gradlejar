package dev.voidchisel;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

/** Server-side removal of vanilla bedrock only; no item drops. */
public final class BedrockMiningEvents {
    private BedrockMiningEvents() {}

    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        // Do not cancel the client event: it must send START_DESTROY_BLOCK to the server.
        if (event.getLevel().isClientSide()
                || event.getAction() != PlayerInteractEvent.LeftClickBlock.Action.START
                || event.getHand() != InteractionHand.MAIN_HAND
                || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        ItemStack tool = player.getMainHandItem();
        if (!tool.is(VoidChisel.VOID_CHISEL.get())) return;

        ServerLevel level = player.serverLevel();
        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);
        if (!state.is(Blocks.BEDROCK)) return;

        // Prevent ordinary attack processing after handling the server action.
        event.setCanceled(true);
        Direction face = event.getFace();
        if (player.isSpectator() || player.isCreative() || !player.getAbilities().mayBuild
                || face == null || !level.getWorldBorder().isWithinBounds(pos)
                || !level.mayInteract(player, pos)
                || !player.mayUseItemAt(pos, face, tool)) {
            return;
        }

        double range = player.blockInteractionRange() + 0.5D;
        if (player.getEyePosition().distanceToSqr(Vec3.atCenterOf(pos)) > range * range) return;

        // Let claim/protection integrations reject the attempted block break.
        BlockEvent.BreakEvent breakEvent = new BlockEvent.BreakEvent(level, pos, state, player);
        NeoForge.EVENT_BUS.post(breakEvent);
        if (breakEvent.isCanceled()) return;

        if (!level.getBlockState(pos).is(Blocks.BEDROCK)) return;
        if (!level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL)) return;

        level.levelEvent(2001, pos, Block.getId(state));
        int used = tool.getDamageValue() + 1;
        if (used >= tool.getMaxDamage()) {
            player.broadcastBreakEvent(InteractionHand.MAIN_HAND);
            tool.shrink(1);
        } else {
            tool.setDamageValue(used);
        }
    }
}
