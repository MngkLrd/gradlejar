package com.kirill.pocketrealms.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PocketRunEntry {
    public int id;
    public UUID owner;
    public String originDimension;
    public BlockPos originPos;
    public BlockPos spawnPos;
    public BlockPos exitPos;
    public int minX;
    public int minY;
    public int minZ;
    public int maxX;
    public int maxY;
    public int maxZ;
    public long startedAt;
    public boolean generated;
    public boolean completed;
    public int collapseCursor;
    public final List<Long> collapseBlocks = new ArrayList<>();
    public final List<Long> spawnerPositions = new ArrayList<>();
    // Stable reusable sector id. Legacy saves use -1 and are retired separately.
    public int slot = -1;
    public boolean retiring;
    public int cleanupCursor;
    public long abandonedAt = -1L;
    public long nextShift;
    public long nextErosionAt;
    public final List<Long> footsteps = new ArrayList<>();
    public long collapseStartAt;
    public long nextPursuerAt;
    public long nextDarknessAt;
    public long doorKnockAt;
    public boolean doorKnockDone;
    public int doorKnocksRemaining;
    public long nextKnockPulseAt;
    public double doorKnockX;
    public double doorKnockY;
    public double doorKnockZ;
    public long nextPursuerIdleSoundAt;
    public long nextPursuerChaseSoundAt;
    // Artificial shortcuts ONLY. The original guaranteed route stays open.
    public final List<Long> phaseCandidates = new ArrayList<>();
    public final List<Long> phaseOpen = new ArrayList<>();

    public boolean contains(BlockPos pos) {
        return pos.getX() >= minX && pos.getX() <= maxX
                && pos.getY() >= minY && pos.getY() <= maxY
                && pos.getZ() >= minZ && pos.getZ() <= maxZ;
    }

    public ResourceKey<Level> originKey() {
        return ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(originDimension));
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Id", id);
        tag.putInt("SectorSlot", slot);
        tag.putBoolean("Retiring", retiring);
        tag.putInt("CleanupCursor", cleanupCursor);
        tag.putLong("AbandonedAt", abandonedAt);
        tag.putLong("NextShift", nextShift);
        tag.putLong("NextErosionAt", nextErosionAt);
        tag.putLongArray("Footsteps", footsteps.stream().mapToLong(Long::longValue).toArray());
        tag.putLong("CollapseStartAt", collapseStartAt);
        tag.putLong("NextPursuerAt", nextPursuerAt);
        tag.putLong("NextDarknessAt",nextDarknessAt);
        tag.putLong("DoorKnockAt", doorKnockAt);
        tag.putBoolean("DoorKnockDone", doorKnockDone);
        tag.putInt("DoorKnocksRemaining", doorKnocksRemaining);
        tag.putLong("NextKnockPulseAt", nextKnockPulseAt);
        tag.putDouble("DoorKnockX", doorKnockX);
        tag.putDouble("DoorKnockY", doorKnockY);
        tag.putDouble("DoorKnockZ", doorKnockZ);
        tag.putLong("NextPursuerIdleSoundAt", nextPursuerIdleSoundAt);
        tag.putLong("NextPursuerChaseSoundAt", nextPursuerChaseSoundAt);
        tag.putLongArray("PhaseCandidates", phaseCandidates.stream().mapToLong(Long::longValue).toArray());
        tag.putLongArray("PhaseOpen", phaseOpen.stream().mapToLong(Long::longValue).toArray());
        tag.putUUID("Owner", owner);
        tag.putString("OriginDimension", originDimension);
        tag.putLong("OriginPos", originPos.asLong());
        tag.putLong("SpawnPos", spawnPos.asLong());
        tag.putLong("ExitPos", exitPos.asLong());
        tag.putInt("MinX", minX);
        tag.putInt("MinY", minY);
        tag.putInt("MinZ", minZ);
        tag.putInt("MaxX", maxX);
        tag.putInt("MaxY", maxY);
        tag.putInt("MaxZ", maxZ);
        tag.putLong("StartedAt", startedAt);
        tag.putBoolean("Generated", generated);
        tag.putBoolean("Completed", completed);
        tag.putInt("CollapseCursor", collapseCursor);
        long[] collapse = collapseBlocks.stream().mapToLong(Long::longValue).toArray();
        tag.putLongArray("CollapseBlocks", collapse);
        tag.putLongArray("CorruptionNests", spawnerPositions.stream().mapToLong(Long::longValue).toArray());
        return tag;
    }

    public static PocketRunEntry load(CompoundTag tag) {
        PocketRunEntry entry = new PocketRunEntry();
        entry.id = tag.getInt("Id");
        entry.slot = tag.contains("SectorSlot") ? tag.getInt("SectorSlot") : -1;
        entry.retiring = tag.getBoolean("Retiring");
        entry.cleanupCursor = tag.getInt("CleanupCursor");
        entry.abandonedAt = tag.contains("AbandonedAt") ? tag.getLong("AbandonedAt") : -1L;
        entry.nextShift = tag.getLong("NextShift");
        entry.nextErosionAt = tag.getLong("NextErosionAt");
        for(long p:tag.getLongArray("Footsteps"))entry.footsteps.add(p);
        entry.collapseStartAt = tag.getLong("CollapseStartAt");
        entry.nextPursuerAt = tag.getLong("NextPursuerAt");
        entry.nextDarknessAt=tag.getLong("NextDarknessAt");
        entry.doorKnockAt = tag.getLong("DoorKnockAt");
        entry.doorKnockDone = tag.getBoolean("DoorKnockDone");
        entry.doorKnocksRemaining = tag.getInt("DoorKnocksRemaining");
        entry.nextKnockPulseAt = tag.getLong("NextKnockPulseAt");
        entry.doorKnockX = tag.getDouble("DoorKnockX");
        entry.doorKnockY = tag.getDouble("DoorKnockY");
        entry.doorKnockZ = tag.getDouble("DoorKnockZ");
        entry.nextPursuerIdleSoundAt = tag.getLong("NextPursuerIdleSoundAt");
        entry.nextPursuerChaseSoundAt = tag.getLong("NextPursuerChaseSoundAt");
        for (long cell : tag.getLongArray("PhaseCandidates")) entry.phaseCandidates.add(cell);
        for (long cell : tag.getLongArray("PhaseOpen")) entry.phaseOpen.add(cell);
        entry.owner = tag.getUUID("Owner");
        entry.originDimension = tag.getString("OriginDimension");
        entry.originPos = BlockPos.of(tag.getLong("OriginPos"));
        entry.spawnPos = BlockPos.of(tag.getLong("SpawnPos"));
        entry.exitPos = BlockPos.of(tag.getLong("ExitPos"));
        entry.minX = tag.getInt("MinX");
        entry.minY = tag.getInt("MinY");
        entry.minZ = tag.getInt("MinZ");
        entry.maxX = tag.getInt("MaxX");
        entry.maxY = tag.getInt("MaxY");
        entry.maxZ = tag.getInt("MaxZ");
        entry.startedAt = tag.getLong("StartedAt");
        entry.generated = tag.getBoolean("Generated");
        entry.completed = tag.getBoolean("Completed");
        entry.collapseCursor = tag.getInt("CollapseCursor");
        for (long value : tag.getLongArray("CollapseBlocks")) {
            entry.collapseBlocks.add(value);
        }
        for (long packed : tag.getLongArray("CorruptionNests")) {
            entry.spawnerPositions.add(packed);
        }
        return entry;
    }
}
