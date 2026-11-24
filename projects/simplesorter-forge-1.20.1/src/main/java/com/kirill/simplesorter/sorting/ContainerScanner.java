package com.kirill.simplesorter.sorting;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.chunk.LevelChunk;
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
import java.util.Map;
import java.util.LinkedHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Discovers only loaded, player-usable storage block entities.
 * A double chest is one logical inventory of its actual physical size.
 * Some capability implementations already expose both halves, even when
 * neither half is the vanilla size. Never wrap two combined views together.
 */
public final class ContainerScanner {
    private static final Logger LOG = LoggerFactory.getLogger("SimpleSorter/Scanner");
    private ContainerScanner() {}
    public record ScanReport(List<ContainerRef> containers, int physicalBlocks, int emptyInventories,
                             int totalSlots, int unloadedChunks, Map<String,Integer> skipped) {
        public ScanReport { containers = List.copyOf(containers); skipped = Map.copyOf(skipped); }
        public String summary() {
            return containers.size() + " logical inventories / " + physicalBlocks + " physical storage blocks; "
                    + emptyInventories + " empty; " + totalSlots + " slots; unloaded chunks: " + unloadedChunks
                    + "; skipped: " + (skipped.isEmpty() ? "none" : skipped);
        }
    }
    private static void skip(Map<String,Integer> reasons,String reason,BlockPos pos,BlockState state) {
        reasons.merge(reason,1,Integer::sum);
        LOG.info("Scan skipped {} {}: {}",pos,BuiltInRegistries.BLOCK.getKey(state.getBlock()),reason);
    }

    public static List<ContainerRef> scanRadius(ServerLevel level, Player player,
                                                BlockPos center, int radius) {
        return scanDetailed(level,player,center,radius).containers();
    }

    public static ScanReport scanDetailed(ServerLevel level,Player player,BlockPos center,int radius) {
        int safe = Math.max(1, Math.min(radius, 64));
        return scanBox(level, player, center.offset(-safe, -safe, -safe),
                center.offset(safe, safe, safe), center, safe);
    }

    public static List<ContainerRef> scanBoxInRadius(ServerLevel level, Player player,
            BlockPos min, BlockPos max, BlockPos center, int radius) {
        return scanBox(level,player,min,max,center,radius).containers();
    }

    private static ScanReport scanBox(ServerLevel level,Player player,BlockPos min,BlockPos max,BlockPos center,int radius) {
        // Always enumerate live loaded chunk contents; never use a chest cache.
        List<BlockPos> candidates = new ArrayList<>();
        Map<String,Integer> skipped = new LinkedHashMap<>();
        int unloadedChunks = 0;
        for (int cx = Math.floorDiv(min.getX(), 16); cx <= Math.floorDiv(max.getX(), 16); cx++) {
            for (int cz = Math.floorDiv(min.getZ(), 16); cz <= Math.floorDiv(max.getZ(), 16); cz++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(cx,cz);
                if (chunk == null) { unloadedChunks++; continue; }
                for (BlockPos pos : chunk.getBlockEntitiesPos()) {
                    if (pos.getX() < min.getX() || pos.getX() > max.getX()
                            || pos.getY() < min.getY() || pos.getY() > max.getY()
                            || pos.getZ() < min.getZ() || pos.getZ() > max.getZ()) continue;
                    int dx = pos.getX() - center.getX();
                    int dy = pos.getY() - center.getY();
                    int dz = pos.getZ() - center.getZ();
                    if (ScanGeometry.contains(dx,dy,dz,radius)) candidates.add(pos.immutable());
                }
            }
        }
        candidates.sort(Comparator.comparingDouble(p -> p.distSqr(center)));
        List<ContainerRef> result = new ArrayList<>();
        Set<BlockPos> visited = new HashSet<>();
        Set<Object> claimedHandlers = Collections.newSetFromMap(new IdentityHashMap<>());

        for (BlockPos pos : candidates) {
            if (visited.contains(pos)) continue;
            BlockState state = level.getBlockState(pos);
            if (!StorageFilter.isEligible(state)) continue;
            if (result.size() >= 384) { skip(skipped,"scan limit",pos,state); continue; }
            BlockEntity be = level.getBlockEntity(pos);
            if (be == null || be.isRemoved()) { skip(skipped,"missing block entity",pos,state); continue; }
            if (!level.mayInteract(player,pos) || !accessible(player,be)) { skip(skipped,"locked or custom access denied",pos,state); continue; }

            if (state.getBlock() instanceof ChestBlock
                    && state.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
                BlockPos other = pos.relative(ChestBlock.getConnectedDirection(state));
                if (level.getChunkSource().getChunkNow(other.getX() >> 4, other.getZ() >> 4) == null) {
                    skip(skipped,"unloaded partner",pos,state); continue;
                }
                BlockState partnerState = level.getBlockState(other);
                if (partnerState.getBlock() != state.getBlock()
                        || partnerState.getValue(ChestBlock.TYPE) == ChestType.SINGLE
                        || !other.relative(ChestBlock.getConnectedDirection(partnerState)).equals(pos)) {
                    skip(skipped,"invalid chest connection",pos,state); continue;
                }

                BlockEntity partner = level.getBlockEntity(other);
                if (partner == null || partner.isRemoved() || !StorageFilter.isEligible(partnerState)
                        || !level.mayInteract(player,other) || !accessible(player,partner)) {
                    skip(skipped,"partner missing or access denied",pos,state); continue;
                }

                visited.add(pos);
                visited.add(other);

                // ChestBlock subclasses expose their physical half through
                // Container, while Forge capabilities may expose either half
                // or the entire combined inventory. Sizes must be measured.
                if (!(be instanceof Container first) || !(partner instanceof Container second)) {
                    skip(skipped,"unknown double chest shape",pos,state); continue;
                }
                IItemHandler a = itemHandler(be);
                IItemHandler b = itemHandler(partner);
                if (a == null || b == null) { skip(skipped,"no item handler",pos,state); continue; }

                BlockPos canonical = pos.asLong() <= other.asLong() ? pos : other;
                boolean firstIsCanonical = canonical.equals(pos);
                Object aIdentity = handlerIdentity(a);
                Object bIdentity = handlerIdentity(b);
                if (claimedHandlers.contains(aIdentity) || claimedHandlers.contains(bIdentity)) {
                    skip(skipped,"duplicate handler",pos,state); continue;
                }
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
                    skip(skipped,"ambiguous chest shape "+canonicalSize+"+"+partnerSize+" / "
                            +canonicalHandler.getSlots()+"+"+partnerHandler.getSlots(),pos,state);
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
            if (handler == null || handler.getSlots() <= 0) { skip(skipped,"no item handler",pos,state); continue; }
            if (!claimedHandlers.add(handlerIdentity(handler))) { skip(skipped,"duplicate handler",pos,state); continue; }
            visited.add(pos);
            result.add(new ContainerRef(pos,handler,be,level));
        }
        int physical=0,empty=0,slots=0;
        for (ContainerRef ref:result) {
            physical += ref.partnerPos()==null ? 1:2;
            slots += ref.slots();
            boolean isEmpty=true;
            try {
                for (int slot=0;slot<ref.slots();slot++) {
                    if (!ref.handler().getStackInSlot(slot).isEmpty()) { isEmpty=false; break; }
                }
            } catch (RuntimeException ex) { isEmpty=false; LOG.warn("Cannot inspect storage {}",ref.pos(),ex); }
            if (isEmpty) empty++;
        }
        return new ScanReport(result,physical,empty,slots,unloadedChunks,skipped);
    }

    private static Object handlerIdentity(IItemHandler handler) {
        if (handler instanceof ForgeItemHandlerAdapter forge) return forge.delegate();
        if (handler instanceof ContainerItemHandler container) return container.container();
        return handler;
    }

    public static boolean canAccess(ServerLevel level,Player player,ContainerRef ref){
        if(!ref.isValid()||!level.mayInteract(player,ref.pos()))return false;
        BlockEntity first=level.getBlockEntity(ref.pos());
        if(first==null||!accessible(player,first))return false;
        if(ref.partnerPos()!=null){
            if(!level.mayInteract(player,ref.partnerPos()))return false;
            BlockEntity second=level.getBlockEntity(ref.partnerPos());
            if(second==null||!accessible(player,second))return false;
        }
        return true;
    }

    private static boolean accessible(Player player, BlockEntity be) {
        // stillValid is a menu reach check (normally eight blocks). Honor
        // actual vanilla-style locks instead. Unknown custom containers may
        // encode access policy in stillValid, so retain that check for them.
        if (be instanceof BaseContainerBlockEntity storage) return storage.canOpen(player);
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
