package dev.voidchisel;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/** Server-authoritative bedrock break, with progressive cracks and zero drops. */
public final class BedrockMiningServer {
    public static final int REQUIRED_TICKS = 8; // ~0.4 seconds, iron pickaxe on stone
    private static final int HEARTBEAT_TIMEOUT = 12;
    private static final Map<UUID, Session> SESSIONS = new HashMap<>();

    private BedrockMiningServer() {}

    private static final class Session {
        final BlockPos pos;
        final ServerLevel level;
        final int crackId;
        int progress;
        long lastInputTick;

        Session(ServerPlayer player, BlockPos pos) {
            this.pos = pos.immutable();
            this.level = player.serverLevel();
            this.crackId = -(100_000 + player.getId());
            this.lastInputTick = level.getGameTime();
        }

        void stop() {
            level.destroyBlockProgress(crackId, pos, -1);
        }
    }

    public static void receive(ServerPlayer player, BlockPos pos) {
        UUID id = player.getUUID();
        Session old = SESSIONS.get(id);

        if (pos == null || !valid(player, pos)) {
            if (old != null) {
                old.stop();
                SESSIONS.remove(id);
            }
            return;
        }

        if (old != null && old.pos.equals(pos) && old.level == player.serverLevel()) {
            old.lastInputTick = player.level().getGameTime();
        } else {
            if (old != null) old.stop();
            SESSIONS.put(id, new Session(player, pos));
        }
    }

    public static void tick(ServerTickEvent.Post event) {
        Iterator<Map.Entry<UUID, Session>> iterator = SESSIONS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Session> entry = iterator.next();
            ServerPlayer player = event.getServer().getPlayerList().getPlayer(entry.getKey());
            Session session = entry.getValue();

            if (player == null || player.serverLevel() != session.level
                    || session.level.getGameTime() - session.lastInputTick > HEARTBEAT_TIMEOUT
                    || !valid(player, session.pos)) {
                session.stop();
                iterator.remove();
                continue;
            }

            session.progress++;
            if (session.progress >= REQUIRED_TICKS) {
                tryBreak(player, session);
                session.stop();
                iterator.remove();
            } else {
                int stage = Math.min(9, session.progress * 10 / REQUIRED_TICKS);
                session.level.destroyBlockProgress(session.crackId, session.pos, stage);
            }
        }
    }

    private static boolean valid(ServerPlayer player, BlockPos pos) {
        ServerLevel level = player.serverLevel();
        ItemStack stack = player.getMainHandItem();
        if (!stack.is(VoidChisel.VOID_CHISEL.get())
                || player.isCreative() || player.isSpectator()
                || !player.getAbilities().mayBuild
                || !level.getWorldBorder().isWithinBounds(pos)
                || !level.hasChunkAt(pos)
                || !level.getBlockState(pos).is(Blocks.BEDROCK)
                || !level.mayInteract(player, pos)
                || !player.mayUseItemAt(pos, net.minecraft.core.Direction.UP, stack)) {
            return false;
        }
        // Line-of-sight validation prevents sending arbitrary target coordinates.
        HitResult hit = player.pick(player.blockInteractionRange(), 1.0F, false);
        return hit instanceof BlockHitResult blockHit
                && blockHit.getBlockPos().equals(pos)
                && player.mayUseItemAt(pos, blockHit.getDirection(), stack);
    }

    private static void tryBreak(ServerPlayer player, Session session) {
        ServerLevel level = session.level;
        BlockPos pos = session.pos;
        if (!valid(player, pos)) return;

        BlockState old = level.getBlockState(pos);
        // Protection mods may veto the break. Do not spend charges if blocked.
        BlockEvent.BreakEvent breakEvent = new BlockEvent.BreakEvent(level, pos, old, player);
        NeoForge.EVENT_BUS.post(breakEvent);
        if (breakEvent.isCanceled() || !level.getBlockState(pos).is(Blocks.BEDROCK)) return;

        if (!level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL)) return;
        level.levelEvent(2001, pos, Block.getId(old));
        // Exactly one use per successfully removed bedrock block, with no drop.
        ItemStack tool = player.getMainHandItem();
        int damage = tool.getDamageValue() + 1;
        if (damage >= tool.getMaxDamage()) {
            tool.shrink(1);
        } else {
            tool.setDamageValue(damage);
        }
    }
}
