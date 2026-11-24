package com.kirill.simplesorter.sorting;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraftforge.common.capabilities.ForgeCapabilities;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Discovers only loaded, player-usable storage block entities.
 * A double chest is one logical inventory of its actual physical size.
 * Some capability implementations already expose both halves, even when
 * neither half is the vanilla size. Never wrap two combined views together.
 */
public final class ContainerScanner {
    private ContainerScanner() {}

    public static List<ContainerRef> scanRadius(ServerLevel level, Player player,
                                                BlockPos center, int radius) {
        int safe = Math.max(1, Math.min(radius, 64));
        return scanBoxInRadius(level, player, center.offset(-safe, -safe, -safe),
                center.offset(safe, safe, safe), center, safe);
    }

    public static List<ContainerRef> scanBoxInRadius(ServerLevel level, Player player,
            BlockPos min, BlockPos max, BlockPos center, int radius) {
        List<BlockPos> candidates = new ArrayList<>();
        int radiusSq = radius * radius;
        for (int cx = Math.floorDiv(min.getX(), 16); cx <= Math.floorDiv(max.getX(), 16); cx++) {
            for (int cz = Math.floorDiv(min.getZ(), 16); cz <= Math.floorDiv(max.getZ(), 16); cz++) {
                if (!level.hasChunk(cx, cz)) continue;
                for (BlockPos pos : level.getChunk(cx, cz).getBlockEntitiesPos()) {
                    if (pos.getX() < min.getX() || pos.getX() > max.getX()
                            || pos.getY() < min.getY() || pos.getY() > max.getY()
                            || pos.getZ() < min.getZ() || pos.getZ() > max.getZ()) continue;
                    int dx = pos.getX() - center.getX();
                    int dy = pos.getY() - center.getY();
                    int dz = pos.getZ() - center.getZ();
                    if (dx * dx + dy * dy + dz * dz <= radiusSq) candidates.add(pos.immutable());
                }
            }
        }
        candidates.sort(Comparator.comparingDouble(p -> p.distSqr(center)));
        List<ContainerRef> result = new ArrayList<>();
        Set<BlockPos> visited = new HashSet<>();
        Set<Object> claimedHandlers = Collections.newSetFromMap(new IdentityHashMap<>());

        for (BlockPos pos : candidates) {
            if (result.size() >= 384) break;
            if (visited.contains(pos)) continue;
            BlockEntity be = level.getBlockEntity(pos);
            if (be == null) continue;
            BlockState state = level.getBlockState(pos);
            if (!StorageFilter.isEligible(state) || !accessible(player, be)) continue;

            if (state.getBlock() instanceof ChestBlock
                    && state.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
                BlockPos other = pos.relative(ChestBlock.getConnectedDirection(state));
                if (!level.hasChunk(other.getX() >> 4, other.getZ() >> 4)) continue;
                BlockState partnerState = level.getBlockState(other);
                if (partnerState.getBlock() != state.getBlock()
                        || partnerState.getValue(ChestBlock.TYPE) == ChestType.SINGLE
                        || !other.relative(ChestBlock.getConnectedDirection(partnerState)).equals(pos))
                    continue;

                BlockEntity partner = level.getBlockEntity(other);
                if (partner == null || !accessible(player, partner)) continue;

                visited.add(pos);
                visited.add(other);

                // ChestBlock subclasses expose their physical half through
                // Container, while Forge capabilities may expose either half
                // or the entire combined inventory. Sizes must be measured.
                if (!(be instanceof Container first) || !(partner instanceof Container second))
                    continue;
                IItemHandler a = itemHandler(be);
                IItemHandler b = itemHandler(partner);
                if (a == null || b == null) continue;

                BlockPos canonical = pos.asLong() <= other.asLong() ? pos : other;
                boolean firstIsCanonical = canonical.equals(pos);
                Object aIdentity = handlerIdentity(a);
                Object bIdentity = handlerIdentity(b);
                if (claimedHandlers.contains(aIdentity) || claimedHandlers.contains(bIdentity))
                    continue;
                // Select the canonical combined view when both are available.
                IItemHandler canonicalHandler = firstIsCanonical ? a : b;
                IItemHandler partnerHandler = firstIsCanonical ? b : a;
                int canonicalSize = firstIsCanonical ? first.getContainerSize() : second.getContainerSize();
                int partnerSize = firstIsCanonical ? second.getContainerSize() : first.getContainerSize();
                ChestInventoryShape.Mode mode = ChestInventoryShape.resolve(
                        canonicalSize, partnerSize, canonicalHandler.getSlots(),
                        partnerHandler.getSlots(), aIdentity == bIdentity);
                IItemHandler logical = switch (mode) {
                    case FIRST_COMBINED -> canonicalHandler;
                    case SECOND_COMBINED -> partnerHandler;
                    case TWO_HALVES -> new DoubleChestItemHandler(canonicalHandler, partnerHandler);
                    case UNSAFE -> null;
                };
                if (logical == null) {
                    org.slf4j.LoggerFactory.getLogger("SimpleSorter/Scanner").debug(
                            "Skipped ambiguous chest {}: physical {}+{}, capability {}+{}",
                            canonical, canonicalSize, partnerSize,
                            canonicalHandler.getSlots(), partnerHandler.getSlots());
                    continue;
                }
                claimedHandlers.add(aIdentity);
                claimedHandlers.add(bIdentity);
                result.add(new ContainerRef(canonical, logical,
                        firstIsCanonical ? be : partner, level,
                        firstIsCanonical ? other : pos,
                        firstIsCanonical ? partner : be));
                continue;
            }

            IItemHandler handler = itemHandler(be);
            if (handler != null && handler.getSlots() > 0
                    && claimedHandlers.add(handlerIdentity(handler))) {
                visited.add(pos);
                result.add(new ContainerRef(pos, handler, be, level));
            }
        }
        return result;
    }

    private static Object handlerIdentity(IItemHandler handler) {
        if (handler instanceof ForgeItemHandlerAdapter forge) return forge.delegate();
        if (handler instanceof ContainerItemHandler container) return container.container();
        return handler;
    }

    private static boolean accessible(Player player, BlockEntity be) {
        return !(be instanceof Container container) || container.stillValid(player);
    }

    private static IItemHandler itemHandler(BlockEntity be) {
        var capability = be.getCapability(ForgeCapabilities.ITEM_HANDLER, null).orElse(null);
        if (capability != null && capability.getSlots() > 0) {
            return new ForgeItemHandlerAdapter(capability);
        }
        if (be instanceof Container container && container.getContainerSize() > 0) {
            return new ContainerItemHandler(container);
        }
        return null;
    }
}
