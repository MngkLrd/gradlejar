package dev.kirill.embershelter.camp;

import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public final class CampBuildTask {
    public final UUID owner;
    public final ResourceKey<Level> dimension;
    public final BlockPos center;
    public final List<CampBuildStep> steps;
    public final boolean paid;
    public long nextTick;
    public int index;

    public CampBuildTask(UUID owner, ResourceKey<Level> dimension, BlockPos center,
                         List<CampBuildStep> steps, boolean paid, long nextTick) {
        this.owner = owner;
        this.dimension = dimension;
        this.center = center;
        this.steps = steps;
        this.paid = paid;
        this.nextTick = nextTick;
    }
}
