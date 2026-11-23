package com.kirill.simplesorter.sorting;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.ForgeCapabilities;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Only general-purpose player storage is eligible, never processing machinery. */
public final class ContainerScanner {
    private static final int MAX_CONTAINERS = 160;
    private static final TagKey<Block> ALLOW = TagKey.create(Registries.BLOCK,
            new ResourceLocation("simplesorter", "sortable_storage"));
    private static final TagKey<Block> DENY = TagKey.create(Registries.BLOCK,
            new ResourceLocation("simplesorter", "unsortable_storage"));

    private ContainerScanner() {}

    public static List<ContainerRef> scanRadius(ServerLevel level, Player player, BlockPos center, int radius) {
        int r = Math.max(1, Math.min(radius, 32));
        return scanBoxInRadius(level, player, center.offset(-r, -r, -r),
                center.offset(r, r, r), center, r);
    }

    public static List<ContainerRef> scanBoxInRadius(ServerLevel level, Player player,
                            BlockPos min, BlockPos max, BlockPos center, int radius) {
        List<BlockPos> candidates = new ArrayList<>();
        int r2 = radius * radius;
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
                    if (dx * dx + dy * dy + dz * dz <= r2) candidates.add(pos.immutable());
                }
            }
        }
        candidates.sort(Comparator.comparingDouble(p -> p.distSqr(center)));
        List<ContainerRef> result = new ArrayList<>();
        for (BlockPos pos : candidates) {
            if (result.size() >= MAX_CONTAINERS) break;
            BlockEntity be = level.getBlockEntity(pos);
            if (be == null || !isEligibleStorage(level.getBlockState(pos))) continue;
            if (be instanceof Container container && !container.stillValid(player)) continue;

            var itemHandler = be.getCapability(ForgeCapabilities.ITEM_HANDLER, null).orElse(null);
            if (itemHandler != null && itemHandler.getSlots() > 0) {
                result.add(new ContainerRef(pos, new ForgeItemHandlerAdapter(itemHandler), be, level));
            } else if (be instanceof Container container && container.getContainerSize() > 0) {
                result.add(new ContainerRef(pos, new ContainerItemHandler(container), be, level));
            }
        }
        return result;
    }

    private static boolean isEligibleStorage(BlockState state) {
        if (state.is(DENY)) return false;
        if (state.is(ALLOW)) return true;

        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        String ns = id.getNamespace();
        String path = id.getPath();

        // Industrial machines, ME networks, interfaces and pipes may expose
        // input/output automation capabilities; never touch them by default.
        if (ns.equals("gtceu") || ns.equals("gregtech") || ns.equals("ae2")
                || ns.equals("appliedenergistics2") || ns.equals("mekanism")
                || ns.equals("create") || ns.equals("thermal")) return false;

        if (ns.equals("minecraft")) {
            return path.equals("chest") || path.equals("trapped_chest")
                    || path.equals("barrel") || path.endsWith("shulker_box");
        }

        // TFC wood chests/crates are general purpose storage, while sealed
        // vessels and TFC barrels have chemistry/food rules and are excluded.
        if (ns.equals("tfc")) {
            return path.contains("/chest/") || path.contains("/crate/")
                    || path.endsWith("_chest") || path.endsWith("_crate");
        }

        // For other mods favour explicit storage blocks, not generic
        // BlockEntities with ITEM_HANDLER.
        return path.equals("chest") || path.endsWith("_chest")
                || path.contains("/chest/") || path.endsWith("_crate")
                || path.contains("/crate/") || path.endsWith("_drawer")
                || path.endsWith("_cabinet") || path.endsWith("_storage_box")
                || path.endsWith("_shulker_box");
    }
}
