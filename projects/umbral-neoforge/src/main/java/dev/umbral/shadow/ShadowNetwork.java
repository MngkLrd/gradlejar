package dev.umbral.shadow;

import com.mojang.logging.LogUtils;
import dev.umbral.Umbral;
import dev.umbral.block.ProjectorBlock;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import java.util.*;

public final class ShadowNetwork {
    private ShadowNetwork(){}
    public static void tick(ServerTickEvent.Post event){
        for(ServerLevel level:event.getServer().getAllLevels()){
            if(level.getGameTime()%4!=0)continue;
            ShadowData data=ShadowData.get(level);
            if(data.sources.isEmpty())continue;
            // Bounded work, even with many emitters. Update only loaded space; no chunk tickets.
            var sources=new ArrayList<>(data.sources.values());
            int count=Math.min(2,sources.size());
            for(int i=0;i<count;i++){
                ShadowData.Source s=sources.get(Math.floorMod(data.cursor++,sources.size()));
                try{update(level,data,s);}catch(RuntimeException ex){
                    if(level.getGameTime()%100==0)LogUtils.getLogger().error("Umbral projector at "+s.pos+" failed",ex);
                }
            }
        }
    }
    private static void update(ServerLevel l,ShadowData data,ShadowData.Source source){
        if(!l.hasChunkAt(source.pos)){
            data.reconcile(l,source,Map.of());return;
        }
        var state=l.getBlockState(source.pos);
        if(!state.is(Umbral.PROJECTOR.get())){
            source.retired=true;
        }
        if(source.retired){
            data.reconcile(l,source,Map.of());
            if(source.cells.isEmpty()){data.sources.remove(source.pos);data.setDirty();}
            return;
        }
        boolean active=state.getValue(ProjectorBlock.ENABLED)&&!l.hasNeighborSignal(source.pos);
        if(state.getValue(ProjectorBlock.LIT)!=active){
            state=state.setValue(ProjectorBlock.LIT,active);l.setBlock(source.pos,state,3);
        }
        data.reconcile(l,source,active?Projection.compute(l,source.pos,state):Map.of());
    }
}
