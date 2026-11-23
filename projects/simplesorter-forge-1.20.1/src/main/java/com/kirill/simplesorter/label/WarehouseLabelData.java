package com.kirill.simplesorter.label;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Persistent non-blocking signs/icons bound to existing warehouse positions. */
public final class WarehouseLabelData extends SavedData {
    private static final String NAME = "simplesorter_warehouse_labels";
    private static final int MAX_TOTAL = 8192;
    private final Map<Long, Label> entries = new HashMap<>();

    public record Label(BlockPos pos, int style, String family,
                        ItemStack icon, ItemStack material) {}

    public static WarehouseLabelData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(WarehouseLabelData::load,
                WarehouseLabelData::new, NAME);
    }

    public static WarehouseLabelData load(CompoundTag compound) {
        WarehouseLabelData data = new WarehouseLabelData();
        ListTag list = compound.getList("labels", Tag.TAG_COMPOUND);
        for (int i = 0; i < Math.min(list.size(), MAX_TOTAL); i++) {
            CompoundTag tag = list.getCompound(i);
            BlockPos pos = BlockPos.of(tag.getLong("pos"));
            int style = tag.getInt("style");
            String family = tag.getString("family");
            if (style < 1 || style > 2 || family.length() > 48) continue;
            ItemStack icon = ItemStack.of(tag.getCompound("icon"));
            ItemStack material = ItemStack.of(tag.getCompound("material"));
            data.entries.put(pos.asLong(), new Label(pos, style, family,
                    icon.copyWithCount(1), material.copyWithCount(1)));
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (Label label : entries.values()) {
            CompoundTag entry = new CompoundTag();
            entry.putLong("pos", label.pos().asLong());
            entry.putInt("style", label.style());
            entry.putString("family", label.family());
            entry.put("icon", label.icon().save(new CompoundTag()));
            entry.put("material", label.material().save(new CompoundTag()));
            list.add(entry);
        }
        tag.put("labels", list);
        return tag;
    }

    public Label get(BlockPos pos) {
        return entries.get(pos.asLong());
    }

    public boolean put(Label label) {
        Label previous = entries.get(label.pos().asLong());
        if (previous == null && entries.size() >= MAX_TOTAL) return false;
        if (previous != null && previous.style() == label.style()
                && previous.family().equals(label.family())
                && ItemStack.isSameItemSameTags(previous.icon(), label.icon())) {
            return false;
        }
        entries.put(label.pos().asLong(), label);
        setDirty();
        return true;
    }

    public Label remove(BlockPos pos) {
        Label removed = entries.remove(pos.asLong());
        if (removed != null) setDirty();
        return removed;
    }

    public List<Label> inChunk(ChunkPos chunk) {
        List<Label> found = new ArrayList<>();
        for (Label label : entries.values()) {
            if (label.pos().getX() >> 4 == chunk.x && label.pos().getZ() >> 4 == chunk.z) {
                found.add(label);
                if (found.size() >= 128) break;
            }
        }
        return found;
    }

    public int total() { return entries.size(); }
}
