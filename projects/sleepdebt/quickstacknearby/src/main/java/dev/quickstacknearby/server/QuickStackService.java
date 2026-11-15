package dev.quickstacknearby.server;

import dev.quickstacknearby.config.QuickStackConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.chunk.LevelChunk;
import java.util.*;

public final class QuickStackService {
    private static final int MAX_BLOCK_ENTITIES_SCANNED = 1024;
    private static final int MAX_CHESTS = 32;
    private static final int COOLDOWN_TICKS = 20;
    private static final Map<ServerPlayer, Long> LAST_USE = new WeakHashMap<>();
    private QuickStackService() {}

    public static void execute(ServerPlayer player) {
        if (player.isSpectator() || !player.isAlive() || player.containerMenu != player.inventoryMenu) return;
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        Long last = LAST_USE.get(player);
        if (last != null && now >= last && now - last < COOLDOWN_TICKS) return;
        LAST_USE.put(player, now);

        List<Target> targets = findChests(level, player, QuickStackConfig.RADIUS.get());
        Inventory inventory = player.getInventory();
        int moved = 0;
        int chestsUsed = 0;
        for (Target target : targets) {
            Container chest = target.container();
            int chestMoved = 0;
            for (int c = 0; c < chest.getContainerSize(); c++) {
                ItemStack existing = chest.getItem(c);
                if (existing.isEmpty()) continue;
                int max = Math.min(existing.getMaxStackSize(), chest.getMaxStackSize());
                if (existing.getCount() >= max) continue;
                for (int p = 9; p < 36; p++) {
                    ItemStack source = inventory.getItem(p);
                    if (source.isEmpty() || !ItemStack.isSameItemSameComponents(existing, source)) continue;
                    if (!chest.canPlaceItem(c, source)) continue;
                    int amount = Math.min(max - existing.getCount(), source.getCount());
                    if (amount <= 0) break;
                    ItemStack dest = existing.copy();
                    dest.grow(amount);
                    ItemStack remainder = source.copy();
                    remainder.shrink(amount);
                    chest.setItem(c, dest);
                    inventory.setItem(p, remainder);
                    existing = dest;
                    moved += amount;
                    chestMoved += amount;
                    if (existing.getCount() >= max) break;
                }
            }
            if (chestMoved > 0) {
                chest.setChanged();
                chestsUsed++;
            }
        }
        if (moved > 0) {
            inventory.setChanged();
            player.inventoryMenu.broadcastChanges();
            player.displayClientMessage(Component.translatable("message.quickstacknearby.moved", moved, chestsUsed), true);
        } else {
            player.displayClientMessage(Component.translatable("message.quickstacknearby.none"), true);
        }
    }

    private static List<Target> findChests(ServerLevel level, ServerPlayer player, int radius) {
        double maxDist2 = (double) radius * radius;
        int cx0 = (int)Math.floor(player.getX() - radius) >> 4;
        int cx1 = (int)Math.floor(player.getX() + radius) >> 4;
        int cz0 = (int)Math.floor(player.getZ() - radius) >> 4;
        int cz1 = (int)Math.floor(player.getZ() + radius) >> 4;
        Set<BlockPos> seen = new HashSet<>();
        List<Target> targets = new ArrayList<>();
        int scanned = 0;
        outer:
        for (int cx = cx0; cx <= cx1; cx++) {
            for (int cz = cz0; cz <= cz1; cz++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
                if (chunk == null) continue;
                for (BlockEntity entity : chunk.getBlockEntities().values()) {
                    if (++scanned > MAX_BLOCK_ENTITIES_SCANNED) break outer;
                    if (!(entity instanceof ChestBlockEntity)) continue;
                    BlockPos pos = entity.getBlockPos();
                    if (seen.contains(pos)) continue;
                    double dx = pos.getX() + 0.5 - player.getX();
                    double dy = pos.getY() + 0.5 - player.getY();
                    double dz = pos.getZ() + 0.5 - player.getZ();
                    double distance2 = dx * dx + dy * dy + dz * dz;
                    if (distance2 > maxDist2) continue;
                    BlockState state = entity.getBlockState();
                    if (!(state.getBlock() instanceof ChestBlock chestBlock)) continue;
                    BlockPos partner = null;
                    if (state.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
                        partner = pos.relative(ChestBlock.getConnectedDirection(state));
                        if (!level.hasChunkAt(partner)) continue;
                    }
                    Container combined = ChestBlock.getContainer(chestBlock, state, level, pos, false);
                    if (combined == null) continue;
                    seen.add(pos);
                    if (partner != null) seen.add(partner);
                    targets.add(new Target(combined, distance2));
                }
            }
        }
        targets.sort(Comparator.comparingDouble(Target::distance));
        if (targets.size() > MAX_CHESTS) return new ArrayList<>(targets.subList(0, MAX_CHESTS));
        return targets;
    }

    private record Target(Container container, double distance) {}
}
