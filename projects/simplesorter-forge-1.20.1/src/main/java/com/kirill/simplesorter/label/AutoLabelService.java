package com.kirill.simplesorter.label;

import com.kirill.simplesorter.item.WandLabelSupplies;
import com.kirill.simplesorter.network.ModNetworking;
import com.kirill.simplesorter.network.WarehouseLabelsPayload;
import com.kirill.simplesorter.settings.SortingWandSettings;
import com.kirill.simplesorter.sorting.ContainerRef;
import com.kirill.simplesorter.sorting.IItemHandler;
import com.kirill.simplesorter.sorting.ItemCategoryResolver;
import com.kirill.simplesorter.sorting.SortCategory;
import com.kirill.simplesorter.sorting.WarehouseLayout;
import com.kirill.simplesorter.sorting.WarehouseColumns;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.Containers;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Server-only label generation from the actual post-sort contents of each chest. */
public final class AutoLabelService {
    private AutoLabelService() {}

    public static int refresh(ServerLevel level, ServerPlayer player, ItemStack wand,
                              SortingWandSettings settings, List<ContainerRef> containers) {
        int style = settings.labelMode();
        if (style < 1 || style > 2 || containers.isEmpty()) return 0;
        WarehouseLabelData data = WarehouseLabelData.get(level);
        Set<ChunkPos> changed = new HashSet<>();
        int created = 0;
        int missingSupplies = 0;

        // First build column semantics once. A mostly-empty chest below
        // an established ore/plant/metal column inherits that column's label.
        List<WarehouseColumns.Node> columnNodes = new ArrayList<>(containers.size());
        List<Map<String, Integer>> chestVotes = new ArrayList<>(containers.size());
        Map<String, ItemStack> representative = new HashMap<>();
        for (ContainerRef ref : containers) {
            Map<String, Integer> votes = new HashMap<>();
            IItemHandler inventory = ref.handler();
            int occupied = 0;
            for (int slot = 0; slot < inventory.getSlots(); slot++) {
                ItemStack stack = inventory.getStackInSlot(slot);
                if (stack.isEmpty()) continue;
                occupied++;
                SortCategory category = ItemCategoryResolver.resolve(stack);
                String family = category.moveAllowed()
                        ? WarehouseLayout.family(category.id()) : "misc";
                votes.merge(family, 1, Integer::sum);
                representative.putIfAbsent(family, new ItemStack(stack.getItem()));
            }
            BlockPos p = ref.pos();
            chestVotes.add(votes);
            columnNodes.add(new WarehouseColumns.Node(p.getX(), p.getY(), p.getZ(),
                    occupied, votes));
        }
        WarehouseColumns columns = WarehouseColumns.analyze(columnNodes);

        for (int index = 0; index < containers.size() && created < 160; index++) {
            ContainerRef ref = containers.get(index);
            BlockPos pos = ref.pos();
            if (!level.mayInteract(player, pos)
                    || !player.mayUseItemAt(pos, Direction.UP, wand)) continue;

            Map<String, Integer> votes = chestVotes.get(index);
            String family = columns.role(index);
            if (family.isEmpty() && !votes.isEmpty()) {
                family = votes.entrySet().stream()
                        .max(Comparator.comparingInt((Map.Entry<String, Integer> e) -> e.getValue())
                                .thenComparing(Map.Entry::getKey))
                        .map(Map.Entry::getKey).orElse("misc");
            }
            if (family.isEmpty()) {
                // Never leave a stale ore sign hanging above an empty chest
                // whose column no longer has a stable role.
                WarehouseLabelData.Label removed = data.remove(pos);
                if (removed != null) {
                    if (!WandLabelSupplies.refund(wand, removed.material())) {
                        Containers.dropItemStack(level, pos.getX() + 0.5D,
                                pos.getY() + 0.6D, pos.getZ() + 0.5D,
                                removed.material().copy());
                    }
                    changed.add(new ChunkPos(pos));
                }
                continue;
            }
            ItemStack icon = representative.getOrDefault(family, ItemStack.EMPTY);
            if (icon.isEmpty()) continue;

            WarehouseLabelData.Label prior = data.get(pos);
            ItemStack material = prior == null ? ItemStack.EMPTY : prior.material();
            if (prior == null || prior.style() != style) {
                ItemStack newMaterial = WandLabelSupplies.consume(wand, style);
                if (newMaterial.isEmpty()) {
                    missingSupplies++;
                    continue; // no fabricated labels
                }
                if (prior != null && !prior.material().isEmpty()) {
                    if (!WandLabelSupplies.refund(wand, prior.material())) {
                        // Never lose an already-paid material if the wand supply
                        // list happens to be full when the label style changes.
                        Containers.dropItemStack(level, pos.getX() + 0.5D,
                                pos.getY() + 0.6D, pos.getZ() + 0.5D,
                                prior.material().copy());
                    }
                }
                material = newMaterial;
            }
            if (data.put(new WarehouseLabelData.Label(
                    pos, style, family, icon.copyWithCount(1), material.copyWithCount(1)))) {
                changed.add(new ChunkPos(pos));
                created++;
            }
        }

        for (ChunkPos chunk : changed) broadcast(level, chunk);
        if (missingSupplies > 0) {
            player.displayClientMessage(Component.literal("Simple Sorter: "
                    + missingSupplies + " chests need "
                    + (style == 1 ? "signs" : "item frames")
                    + ". Add materials to inventory, then press Load Labels."), true);
        }
        return created;
    }

    public static void sendChunk(ServerPlayer player, ServerLevel level, ChunkPos chunk) {
        WarehouseLabelsPayload packet = WarehouseLabelsPayload.of(
                chunk, WarehouseLabelData.get(level).inChunk(chunk));
        ModNetworking.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static void broadcast(ServerLevel level, ChunkPos chunk) {
        double x = (chunk.x << 4) + 8.0D;
        double z = (chunk.z << 4) + 8.0D;
        for (ServerPlayer player : level.players()) {
            double dx = player.getX() - x;
            double dz = player.getZ() - z;
            if (dx * dx + dz * dz < 96D * 96D) sendChunk(player, level, chunk);
        }
    }
}
