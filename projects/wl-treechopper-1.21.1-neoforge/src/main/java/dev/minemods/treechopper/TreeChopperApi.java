package dev.minemods.treechopper;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

/** Read-only server-thread tree cutting contract. No forced chunk loading. */
public final class TreeChopperApi {
    public static final int API_VERSION=1, API_REVISION=0;
    private TreeChopperApi(){}
    public record CutSnapshot(int north,int south,int west,int east,int totalPermille,boolean severed){}
    public static boolean requiresAxe(BlockState state){return TreeChopper.requiresAxe(state);}
    public static Optional<CutSnapshot> cut(ServerLevel l,BlockPos pos){
        if(!l.getServer().isSameThread()||!l.hasChunkAt(pos)||l.isOutsideBuildHeight(pos))return Optional.empty();
        var state=l.getBlockState(pos);
        if(!(state.getBlock() instanceof CutBlock))return Optional.empty();
        int[] w=CutProgress.read(l,pos,state);
        return Optional.of(new CutSnapshot(w[0],w[1],w[2],w[3],ChopEffort.progress(w),ChopEffort.severed(w)));
    }
}
