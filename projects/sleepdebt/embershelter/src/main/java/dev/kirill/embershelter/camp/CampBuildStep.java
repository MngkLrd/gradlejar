package dev.kirill.embershelter.camp;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public record CampBuildStep(BlockPos pos, BlockState state) {}
