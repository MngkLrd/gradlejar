package com.kirill.pocketrealms.block;

import com.kirill.pocketrealms.world.PocketTeleporter;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** An optional next-stage gate. Existing Return Portal remains unchanged. */
public final class DescentGateBlock extends Block {
    public DescentGateBlock(Properties properties){super(properties);}
    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit){
        if(level.isClientSide())return InteractionResult.SUCCESS;
        if(player instanceof ServerPlayer sp)return PocketTeleporter.advanceStage(sp,pos)
                ? InteractionResult.SUCCESS:InteractionResult.FAIL;
        return InteractionResult.FAIL;
    }
}
