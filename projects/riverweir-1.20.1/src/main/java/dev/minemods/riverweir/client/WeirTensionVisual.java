package dev.minemods.riverweir.client;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.minemods.riverweir.RiverWeirMod;
import dev.minemods.riverweir.block.WeirNetBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Iterator;

/** Small client-only tension accents, only around the active fish; no cloth solver or mesh scans. */
@Mod.EventBusSubscriber(modid=RiverWeirMod.ID,bus=Mod.EventBusSubscriber.Bus.FORGE,value=Dist.CLIENT)
public final class WeirTensionVisual {
    private static final int LIFE=16,MAX_PULSES=64;
    private static final Map<BlockPos,Pulse> ACTIVE=new LinkedHashMap<>();
    private static final MultiBufferSource.BufferSource BUFFER=MultiBufferSource.immediate(new BufferBuilder(512));
    private static Level currentLevel;
    private record Pulse(long start,int side){}
    private WeirTensionVisual(){}
    private static void reset(Level level){
        if(level!=currentLevel){ACTIVE.clear();currentLevel=level;}
    }
    public static void pulse(Level level,BlockPos pos,int side){
        if(!level.isClientSide)return;
        reset(level);
        if(ACTIVE.size()>=MAX_PULSES&&!ACTIVE.containsKey(pos)){
            Iterator<BlockPos> keys=ACTIVE.keySet().iterator();
            if(keys.hasNext()){keys.next();keys.remove();}
        }
        ACTIVE.put(pos.immutable(),new Pulse(level.getGameTime(),side));
    }
    @SubscribeEvent public static void render(RenderLevelStageEvent event){
        if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_ENTITIES)return;
        Level level=Minecraft.getInstance().level;
        reset(level);
        if(level==null||ACTIVE.isEmpty())return;
        Vec3 camera=event.getCamera().getPosition();
        PoseStack pose=event.getPoseStack();
        VertexConsumer lines=null;
        Iterator<Map.Entry<BlockPos,Pulse>> iterator=ACTIVE.entrySet().iterator();
        while(iterator.hasNext()){
            Map.Entry<BlockPos,Pulse> entry=iterator.next();
            BlockPos p=entry.getKey();Pulse pulse=entry.getValue();
            double age=level.getGameTime()-pulse.start()+event.getPartialTick();
            if(age<0||age>=LIFE||!level.hasChunkAt(p)){iterator.remove();continue;}
            BlockState state=level.getBlockState(p);
            if(!state.is(RiverWeirMod.NET.get())||state.getValue(WeirNetBlock.STAGE)==2){iterator.remove();continue;}
            if(camera.distanceToSqr(p.getX()+.5,p.getY()+.5,p.getZ()+.5)>32*32)continue;
            double envelope=Math.sin(Math.PI*age/LIFE);
            double bend=-pulse.side()*.115*envelope;
            if(Math.abs(bend)<.015)continue;
            if(lines==null)lines=BUFFER.getBuffer(RenderType.lines());
            boolean x=state.getValue(WeirNetBlock.AXIS)==Direction.Axis.X;
            int tier=state.getValue(WeirNetBlock.TIER);
            // Muted material-dependent shades. These are four displaced tension cords,
            // not a replacement model and not a second fish.
            int r=tier==0?129:tier==1?143:111;
            int g=tier==0?109:tier==1?106:118;
            int b=tier==0?76:tier==1?73:119;
            pose.pushPose();
            pose.translate(p.getX()-camera.x,p.getY()-camera.y,p.getZ()-camera.z);
            // Start on the surface facing the viewer's original fish side, then bow
            // toward the other side. All end points remain anchored on the net.
            double plane=.5-pulse.side()*.04;
            for(int i=-1;i<=1;i++){
                double u=.5+i*.10;
                Vec3 a=point(x,u-.18,.34,plane);
                Vec3 mid=point(x,u,.50,plane+bend);
                Vec3 end=point(x,u+.18,.66,plane);
                line(pose,lines,a,mid,r,g,b);line(pose,lines,mid,end,r,g,b);
                Vec3 a2=point(x,u+.18,.34,plane);
                Vec3 end2=point(x,u-.18,.66,plane);
                line(pose,lines,a2,mid,r,g,b);line(pose,lines,mid,end2,r,g,b);
            }
            pose.popPose();
        }
        if(lines!=null)BUFFER.endBatch(RenderType.lines());
    }
    private static Vec3 point(boolean x,double along,double y,double normal){
        return x?new Vec3(along,y,normal):new Vec3(normal,y,along);
    }
    private static void line(PoseStack pose,VertexConsumer out,Vec3 a,Vec3 b,int r,int g,int blue){
        Vec3 n=b.subtract(a).normalize();
        PoseStack.Pose mat=pose.last();
        out.vertex(mat.pose(),(float)a.x,(float)a.y,(float)a.z).color(r,g,blue,255)
            .normal(mat.normal(),(float)n.x,(float)n.y,(float)n.z).endVertex();
        out.vertex(mat.pose(),(float)b.x,(float)b.y,(float)b.z).color(r,g,blue,255)
            .normal(mat.normal(),(float)n.x,(float)n.y,(float)n.z).endVertex();
    }
}
