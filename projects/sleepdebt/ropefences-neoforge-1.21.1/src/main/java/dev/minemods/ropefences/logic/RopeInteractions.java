package dev.minemods.ropefences.logic;

import dev.minemods.ropefences.RopeFences;
import dev.minemods.ropefences.block.RopeSpanBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class RopeInteractions {
    private static final Map<UUID, Anchor> FIRST = new ConcurrentHashMap<>();

    private record Anchor(ResourceKey<Level> dimension, BlockPos pos, long selectedAt) { }

    @SubscribeEvent
    public void useLeadOnFence(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        if (event.getHand() != InteractionHand.MAIN_HAND || !player.isShiftKeyDown()
                || !event.getItemStack().is(Items.LEAD)) return;
        if (!(event.getLevel().getBlockState(event.getPos()).getBlock() instanceof FenceBlock)) return;
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        BlockPos clicked = event.getPos().immutable();
        Anchor previous = FIRST.remove(player.getUUID());
        if (previous == null || !previous.dimension().equals(level.dimension())
                || level.getGameTime() - previous.selectedAt() > 20L * 60L
                || !isFence(level, previous.pos())) {
            FIRST.put(player.getUUID(), new Anchor(level.dimension(), clicked, level.getGameTime()));
            say(player, "message.ropefences.first");
            return;
        }
        if (clicked.equals(previous.pos())) {
            say(player, "message.ropefences.canceled");
            return;
        }
        BlockPos start = previous.pos();
        int dx = clicked.getX() - start.getX();
        int dy = clicked.getY() - start.getY();
        int dz = clicked.getZ() - start.getZ();
        int length = Math.abs(dx) + Math.abs(dz);
        if (dy != 0 || (dx != 0 && dz != 0) || length < 2 || length > RopeSpanBlock.MAX_SPAN) {
            say(player, "message.ropefences.distance");
            return;
        }
        Direction direction = dx > 0 ? Direction.EAST : dx < 0 ? Direction.WEST : dz > 0 ? Direction.SOUTH : Direction.NORTH;
        for (int i = 1; i < length; i++) {
            BlockPos gap = start.relative(direction, i);
            if (!level.getBlockState(gap).isAir()) {
                say(player, "message.ropefences.obstructed");
                return;
            }
        }
        if (!player.isCreative() && !player.getMainHandItem().is(Items.LEAD)) return;
        RopeSpanBlock.placeSpan(level, start, clicked);
        if (!player.isCreative()) player.getMainHandItem().shrink(1);
        say(player, "message.ropefences.created");
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void whenFenceMined(BlockEvent.BreakEvent event) {
        if (event.isCanceled() || !(event.getLevel() instanceof ServerLevel level)) return;
        if (!(event.getState().getBlock() instanceof FenceBlock)) return;
        BlockPos fence = event.getPos();
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos neighbor = fence.relative(direction);
            BlockState state = level.getBlockState(neighbor);
            if (state.is(RopeFences.ROPE_SPAN.get()) && state.getValue(RopeSpanBlock.AXIS) == direction.getAxis()) {
                RopeSpanBlock.dismantle(level, neighbor, direction.getAxis(), !event.getPlayer().isCreative());
            }
        }
    }

    private static boolean isFence(Level level, BlockPos pos) {
        return level.getBlockState(pos).getBlock() instanceof FenceBlock;
    }

    private static void say(Player player, String key) {
        player.displayClientMessage(Component.translatable(key), true);
    }
}
