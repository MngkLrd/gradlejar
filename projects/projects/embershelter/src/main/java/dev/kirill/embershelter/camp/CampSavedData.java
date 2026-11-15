package dev.kirill.embershelter.camp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.server.level.ServerLevel;

/** Remembers completed Camp Kit campfires between world saves. */
public final class CampSavedData extends SavedData {
    private static final Factory<CampSavedData> FACTORY = new Factory<>(CampSavedData::new, CampSavedData::load);
    private final Set<BlockPos> camps = new HashSet<>();

    public static CampSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(FACTORY, "embershelter_camps");
    }

    public static CampSavedData load(CompoundTag tag, HolderLookup.Provider lookup) {
        CampSavedData data = new CampSavedData();
        for (long packed : tag.getLongArray("Centers")) data.camps.add(BlockPos.of(packed));
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider lookup) {
        tag.putLongArray("Centers", camps.stream().mapToLong(BlockPos::asLong).toArray());
        return tag;
    }

    public void add(BlockPos pos) {
        if (camps.add(pos.immutable())) setDirty();
    }

    public void remove(BlockPos pos) {
        if (camps.remove(pos)) setDirty();
    }

    public List<BlockPos> positions() { return List.copyOf(camps); }
}
