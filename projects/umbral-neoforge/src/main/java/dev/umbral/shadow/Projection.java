package dev.umbral.shadow;

import dev.umbral.Umbral;
import dev.umbral.block.ProjectorBlock;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import java.util.*;

/** Samples physical silhouette surfaces at half-block horizontal and quarter-block vertical resolution. */
public final class Projection {
    private static final int MAX_CELLS=640;
    private Projection(){}
    public static Map<BlockPos,Integer> compute(ServerLevel l,BlockPos origin,BlockState projector){
        Direction f=projector.getValue(ProjectorBlock.FACING),r=f.getClockWise();
        int range=ProjectorBlock.DISTANCES[projector.getValue(ProjectorBlock.RANGE)];
        ArrayList<ProjectionMath.Box> boxes=new ArrayList<>();
        // Only nearby opaque full blocks serve as casters; generated shadows never cast recursively.
        for(int d=1;d<=5;d++)for(int u=-3;u<=3;u++)for(int y=-2;y<=3;y++){
            if(Math.abs(u)>d)continue;
            BlockPos p=origin.relative(f,d).relative(r,u).above(y);
            if(!l.hasChunkAt(p))continue;
            BlockState s=l.getBlockState(p);
            if(!s.is(Umbral.SHADOW.get())&&!s.is(Umbral.PROJECTOR.get())&&s.isSolidRender(l,p)){
                boxes.add(new ProjectionMath.Box(d-.5,d+.5,u-.5,u+.5,y-1,y));
            }
        }
        Map<BlockPos,Integer> result=new LinkedHashMap<>();
        if(boxes.isEmpty())return result;
        double sy=origin.getY()+1.0;
        // Lateral sample offsets .25/.75 ensure the grid aligns exactly with world half-block quadrants.
        for(int depth=2;depth<range*2;depth++){
            double d=depth*.5+.25;
            for(int lateral=-12;lateral<12;lateral++){
                double u=lateral*.5+.25;
                if(Math.abs(u)>Math.min(6.0,d*.80))continue;
                double x=origin.getX()+.5+f.getStepX()*d+r.getStepX()*u;
                double z=origin.getZ()+.5+f.getStepZ()*d+r.getStepZ()*u;
                int bx=(int)Math.floor(x),bz=(int)Math.floor(z);
                int quadrant=(x-bx>=.5?1:0)+(z-bz>=.5?2:0);
                List<ProjectionMath.Span> spans=ProjectionMath.silhouette(boxes,d,u);
                for(var span:spans){
                    if(span.top()<-5||span.top()>5)continue;
                    int q=(int)Math.round((sy+span.top())*4);
                    int by=ProjectionMath.worldBlockY(q);
                    BlockPos p=new BlockPos(bx,by,bz);
                    if(by<l.getMinBuildHeight()||by>=l.getMaxBuildHeight()||!l.hasChunkAt(p)||!l.getWorldBorder().isWithinBounds(p))continue;
                    BlockState state=l.getBlockState(p);
                    if(!state.isAir()&&!state.is(Umbral.SHADOW.get()))continue;
                    if(!result.containsKey(p)&&result.size()>=MAX_CELLS)continue;
                    result.merge(p,ProjectionMath.mergeTile(0,quadrant,ProjectionMath.localHeight(q)),
                        (a,b)->ProjectionMath.mergeTile(a,quadrant,ProjectionMath.localHeight(q)));
                }
            }
        }
        return result;
    }
}
