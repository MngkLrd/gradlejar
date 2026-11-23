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
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
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

        for (ContainerRef ref : containers) {
            if (created >= 160) break;
            BlockPos pos = ref.pos();
            // Do not use labels to bypass permissions in protected worlds.
            if (!level.mayInteract(player, pos)
                    || !player.mayUseItemAt(pos, Direction.UP, wand)) continue;
            IItemHandler inventory = ref.handler();
            Map<String, Integer> votes = new HashMap<>();
            Map<String, ItemStack> representatives = new HashMap<>();
            for (int slot = 0; slot < inventory.getSlots(); slot++) {
                ItemStack stack = inventory.getStackInSlot(slot);
                if (stack.isEmpty()) continue;
                SortCategory category = ItemCategoryResolver.resolve(stack);
                String family = category.moveAllowed() ? WarehouseLayout.family(category.id()) : "misc";
                votes.merge(family, 1, Integer::sum);
                representatives.putIfAbsent(family, stack.copyWithCount(1));
            }
            if (votes.isEmpty()) continue;

            String family = votes.entrySet().stream()
                    .max(Comparator.comparingInt((Map.Entry<String, Integer> e) -> e.getValue())
                            .thenComparing(Map.Entry::getKey))
                    .map(Map.Entry::getKey).orElse("misc");
            ItemStack icon = representatives.getOrDefault(family, ItemStack.EMPTY);
            if (icon.isEmpty()) continue;

            WarehouseLabelData.Label prior = data.get(pos);
            ItemStack material = prior == null ? ItemStack.EMPTY : prior.material();
            if (prior == null || prior.style() != style) {
                ItemStack newMaterial = WandLabelSupplies.consume(wand, style);
                if (newMaterial.isEmpty()) continue; // no free labels or fabricated resources
                if (prior != null && !prior.material().isEmpty()) {
                    WandLabelSupplies.refund(wand, prior.material());
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
