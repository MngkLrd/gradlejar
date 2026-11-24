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
import com.kirill.simplesorter.sorting.StrictStackClassifier;
import com.kirill.simplesorter.sorting.SemanticItemProfile;
import com.kirill.simplesorter.sorting.SemanticChestQuality;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
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
        if (containers.isEmpty()) return 0;
        if (style == 0) return clearDisabledLabels(level, player, wand, containers);
        if (style < 1 || style > 2) return 0;
        WarehouseLabelData data = WarehouseLabelData.get(level);
        Set<ChunkPos> changed = new HashSet<>();
        int created = 0;
        int missingSupplies = 0;

        // Labels represent this individual chest, not a six-role warehouse
        // template and not a vertical column. One column can have ores above,
        // tools in the middle and clay below. A mixed chest is explicitly
        // marked mixed instead of being mislabeled "Crafting & Tools".
        for (int index = 0; index < containers.size() && created < 160; index++) {
            ContainerRef ref = containers.get(index);
            BlockPos pos = ref.pos();
            if (!level.mayInteract(player, pos)
                    || !player.mayUseItemAt(pos, Direction.UP, wand)) continue;

            if(!ref.isValid())continue;
            List<SemanticItemProfile> profiles=new ArrayList<>();
            List<ItemStack> stacks=new ArrayList<>();
            IItemHandler inventory=ref.handler();
            for(int slot=0;slot<inventory.getSlots();slot++){
                ItemStack stack=inventory.getStackInSlot(slot);
                if(stack.isEmpty())continue;
                var profile=StrictStackClassifier.profile(stack);
                profiles.add(settings.mode()==com.kirill.simplesorter.settings.SortingMode.BY_MOD
                        ?com.kirill.simplesorter.sorting.ModOriginProfile.of(profile):profile);
                stacks.add(stack);
            }
            var quality=SemanticChestQuality.evaluate(profiles);
            String family=quality.label();
            if (family.isEmpty()) {
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
            // Never show an item icon from another chest, or use an
            // arbitrary representative item to disguise a mixed chest.
            ItemStack icon=quality.representative()<0?ItemStack.EMPTY:
                    new ItemStack(stacks.get(quality.representative()).getItem());

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
            Direction facing = resolveFacing(level, player, pos, prior,
                    data.needsFacingMigration(pos));
            if (data.put(new WarehouseLabelData.Label(
                    pos, style, family, icon.copyWithCount(1),
                    material.copyWithCount(1), facing))) {
                changed.add(new ChunkPos(pos));
                created++;
            }
        }

        // Important: unchanged labels were NOT re-sent in 0.9.7/0.9.8.
        // If the initial ChunkWatch packet arrived before the client world
        // existed, subsequent sorts performed zero NBT changes and the
        // screen remained completely empty forever. Always re-send the
        // current chunk snapshots to the sorting player.
        for (ChunkPos chunk : changed) broadcast(level, chunk);
        Set<ChunkPos> scannedChunks = new HashSet<>();
        for (ContainerRef ref : containers) {
            scannedChunks.add(new ChunkPos(ref.pos()));
        }
        for (ChunkPos chunk : scannedChunks) {
            if (!changed.contains(chunk)) sendChunk(player, level, chunk);
        }
        if (missingSupplies > 0) {
            player.displayClientMessage(Component.literal("Simple Sorter: "
                    + missingSupplies + " chests need "
                    + (style == 1 ? "signs" : "item frames")
                    + ". Add materials to inventory, then press Load Labels."), false);
        }
        return created;
    }

    /**
     * The label's plane is chosen only when created, never on each render
     * frame. Blockstate-facing is authoritative when available (vanilla chest,
     * TFC crates with a horizontal facing property). Nondirectional modded
     * storage receives a permanent front side facing the creator.
     */
    private static Direction resolveFacing(ServerLevel level, ServerPlayer player,
                                           BlockPos pos, WarehouseLabelData.Label existing,
                                           boolean legacyWithoutFacing) {
        BlockState state = level.getBlockState(pos);
        // Vanilla chest FACING corresponds to its physical front. Other
        // modded blockstates may expose a different model orientation, so
        // use the player's side on *first creation/migration* for them.
        if (state.getBlock() instanceof net.minecraft.world.level.block.ChestBlock) {
            for (Property<?> property : state.getProperties()) {
                if (!"facing".equals(property.getName())) continue;
                Comparable<?> value = state.getValue(property);
                if (value instanceof Direction direction && direction.getAxis().isHorizontal()) {
                    return direction;
                }
            }
        }
        if (existing != null && !legacyWithoutFacing
                && existing.facing().getAxis().isHorizontal()) {
            return existing.facing();
        }
        double x = player.getX() - (pos.getX() + 0.5D);
        double z = player.getZ() - (pos.getZ() + 0.5D);
        return Math.abs(x) >= Math.abs(z)
                ? (x >= 0D ? Direction.EAST : Direction.WEST)
                : (z >= 0D ? Direction.SOUTH : Direction.NORTH);
    }


    /** Labels OFF really hides old decals. Consumed sign/frame materials are
     * returned to the wand (or dropped safely if its supply store is full).
     * This also makes OFF/Text/Icons FPS comparisons meaningful. */
    private static int clearDisabledLabels(ServerLevel level, ServerPlayer player,
                                           ItemStack wand, List<ContainerRef> containers) {
        WarehouseLabelData data = WarehouseLabelData.get(level);
        Set<ChunkPos> changed = new HashSet<>();
        int cleared = 0;
        for (ContainerRef ref : containers) {
            BlockPos pos = ref.pos();
            if (!level.mayInteract(player, pos)
                    || !player.mayUseItemAt(pos, Direction.UP, wand)) continue;
            WarehouseLabelData.Label old = data.remove(pos);
            if (old == null) continue;
            cleared++;
            changed.add(new ChunkPos(pos));
            if (!old.material().isEmpty()
                    && !WandLabelSupplies.refund(wand, old.material())) {
                Containers.dropItemStack(level, pos.getX() + 0.5D,
                        pos.getY() + 0.7D, pos.getZ() + 0.5D, old.material().copy());
            }
        }
        for (ChunkPos chunk : changed) broadcast(level, chunk);
        return cleared;
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
