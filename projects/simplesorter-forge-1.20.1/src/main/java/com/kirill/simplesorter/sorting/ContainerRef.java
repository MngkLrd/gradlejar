package com.kirill.simplesorter.sorting;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;

/** A logical inventory, with both physical identities for a double chest. */
public record ContainerRef(BlockPos pos, IItemHandler handler, Object identity,
                           ServerLevel level, BlockPos partnerPos, Object partnerIdentity) {
    public ContainerRef(BlockPos pos, IItemHandler handler, Object identity, ServerLevel level) {
        this(pos, handler, identity, level, null, null);
    }

    public int slots() { return handler.getSlots(); }

    public boolean isValid() {
        if (!level.hasChunkAt(pos) || level.getBlockEntity(pos) != identity) return false;
        BlockState state = level.getBlockState(pos);
        if (partnerPos == null) {
            // A single chest may have been connected to a new half since scan.
            return !(state.getBlock() instanceof ChestBlock)
                    || state.getValue(ChestBlock.TYPE) == ChestType.SINGLE;
        }
        if (!level.hasChunkAt(partnerPos)
                || level.getBlockEntity(partnerPos) != partnerIdentity
                || !(state.getBlock() instanceof ChestBlock)
                || state.getValue(ChestBlock.TYPE) == ChestType.SINGLE
                || !pos.relative(ChestBlock.getConnectedDirection(state)).equals(partnerPos))
            return false;
        BlockState partner = level.getBlockState(partnerPos);
        return partner.getBlock() == state.getBlock()
                && partner.getValue(ChestBlock.TYPE) != ChestType.SINGLE
                && partnerPos.relative(ChestBlock.getConnectedDirection(partner)).equals(pos);
    }
}
