package dev.minemods.treechopper;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/** Bounded, multi-source ownership by leaf path distance, not matching leaf species alone. */
final class CrownOwnership {
    private static final int LIMIT=6, MAX_VISITED=8192;
    private CrownOwnership(){}
    static Set<BlockPos> owned(Level l,Set<BlockPos> logs){
        Map<BlockPos,Integer> own=new HashMap<>();
        Deque<BlockPos> queue=new ArrayDeque<>();
        for(BlockPos log:logs){own.put(log,0);queue.add(log);}
        if(!spread(l,own,queue))return Set.of();
        Map<BlockPos,Integer> foreign=new HashMap<>();
        Deque<BlockPos> other=new ArrayDeque<>();
        // Candidate canopy leaves adjacent to any other actual timber receive competing support.
        for(var e:own.entrySet()){
            BlockPos p=e.getKey();
            if(e.getValue()==0)continue;
            for(Direction d:Direction.values()){
                BlockPos n=p.relative(d);
                if(!l.hasChunkAt(n))continue;
                var state=l.getBlockState(n);
                if(!logs.contains(n)&&(state.is(BlockTags.LOGS)||state.getBlock() instanceof CutBlock)){
                    if(foreign.putIfAbsent(n,0)==null)other.add(n);
                }
                // Outside the scanned crown: distance metadata can identify a nearer external source.
                if(!own.containsKey(n)&&TreeChopper.naturalLeaf(state)&&state.hasProperty(BlockStateProperties.DISTANCE)){
                    int distance=state.getValue(BlockStateProperties.DISTANCE);
                    if(distance<LIMIT&&distance+1<=e.getValue()&&foreign.getOrDefault(n,99)>distance){foreign.put(n,distance);other.add(n);}
                }
            }
        }
        if(!spread(l,foreign,other))return Set.of();
        Set<BlockPos> result=new LinkedHashSet<>();
        for(var e:own.entrySet()){
            if(e.getValue()==0)continue;
            BlockPos p=e.getKey();var state=l.getBlockState(p);
            if(!TreeChopper.naturalLeaf(state))continue;
            if(state.hasProperty(BlockStateProperties.DISTANCE)&&state.getValue(BlockStateProperties.DISTANCE)<e.getValue())continue;
            if(foreign.getOrDefault(p,99)>e.getValue())result.add(p);
        }
        return result;
    }
    private static boolean spread(Level l,Map<BlockPos,Integer> distances,Deque<BlockPos> queue){
        while(!queue.isEmpty()){
            BlockPos p=queue.removeFirst();int next=distances.get(p)+1;
            if(next>LIMIT)continue;
            for(Direction d:Direction.values()){
                BlockPos n=p.relative(d);
                if(!l.hasChunkAt(n)||!TreeChopper.naturalLeaf(l.getBlockState(n))||distances.getOrDefault(n,99)<=next)continue;
                if(distances.size()>=MAX_VISITED)return false;
                distances.put(n,next);queue.add(n);
            }
        }
        return true;
    }
}
