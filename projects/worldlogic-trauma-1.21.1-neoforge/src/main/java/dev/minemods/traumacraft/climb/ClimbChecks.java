package dev.minemods.traumacraft.climb;

import dev.minemods.traumacraft.health.*;
import dev.minemods.traumacraft.integration.WorldLogicBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import java.util.LinkedHashMap;

public final class ClimbChecks {
    private ClimbChecks(){}
    private static void require(boolean value,String label){if(!value)throw new IllegalStateException("Climb QA: "+label);}
    public static void verify(ServerLevel l,ServerPlayer p){
        var b=BodyStates.get(p);
        var old=p.position();float yaw=p.getYRot(),pitch=p.getXRot();
        boolean grounded=p.onGround();
        var blocks=new LinkedHashMap<BlockPos,BlockState>();
        var ledge=new BlockPos(12,101,12);
        try{
            for(int x=10;x<=13;x++)for(int y=100;y<=104;y++)for(int z=11;z<=13;z++){
                var at=new BlockPos(x,y,z);blocks.put(at,l.getBlockState(at));
                l.setBlockAndUpdate(at,y==100?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState());
            }
            l.setBlockAndUpdate(ledge,Blocks.STONE.defaultBlockState());
            p.setYRot(-90);p.setXRot(0);
            Vec3 start=new Vec3(11.62,101,12.5);
            p.setPos(start.x,start.y,start.z);p.setOnGround(true);
            require(ClimbGeometry.find(p)!=null,"clear full ledge");
            require(!ClimbManager.start(p),"healthy legs rejected");
            b.setFractured(BodyPart.LEFT_LEG,true);
            require(ClimbManager.needs(b)&&ClimbManager.mask(b)==3,"two hands intact");
            int both=ClimbManager.duration(b,3);
            b.setFractured(BodyPart.RIGHT_ARM,true);
            require(ClimbManager.mask(b)==1&&ClimbManager.duration(b,1)>both,"left hand only slower");
            b.setFractured(BodyPart.LEFT_ARM,true);
            require(ClimbManager.mask(b)==0&&!ClimbManager.start(p),"both arms broken");
            b.setFractured(BodyPart.RIGHT_ARM,false);
            require(ClimbManager.mask(b)==2,"right hand only");
            b.setFractured(BodyPart.LEFT_ARM,false);
            l.setBlockAndUpdate(ledge.above(),Blocks.STONE.defaultBlockState());
            require(ClimbGeometry.find(p)==null&&!ClimbManager.start(p),"blocked headroom");
            l.setBlockAndUpdate(ledge.above(),Blocks.AIR.defaultBlockState());
            require(WorldLogicBridge.functionsEnabled(p),"injury rule fixture");
            require(ClimbManager.start(p),"start two-arm ascent");
            require(!ClimbManager.start(p),"duplicate start rejected");
            double previousY=p.getY();
            for(int i=0;i<both;i++){
                ClimbManager.tick(p);
                require(p.getY()+1e-7>=previousY,"monotonic ascent");
                require(l.noCollision(p,p.getBoundingBox().deflate(1e-5)),"no collision during ascent");
                previousY=p.getY();
            }
            require(!ClimbManager.active(p)&&p.position().distanceToSqr(new Vec3(12.5,102.015,12.5))<.001,"reached top");
            p.setPos(start.x,start.y,start.z);p.setOnGround(true);
            b.setFractured(BodyPart.RIGHT_ARM,true);
            require(ClimbManager.start(p),"one-arm ascent");
            ClimbManager.tick(p);
            l.setBlockAndUpdate(ledge,Blocks.AIR.defaultBlockState());ClimbManager.tick(p);
            require(!ClimbManager.active(p)&&p.getX()<12,"lost ledge stops ascent");
            l.setBlockAndUpdate(ledge,Blocks.STONE.defaultBlockState());
            p.setPos(start.x,start.y,start.z);p.setOnGround(true);
            require(ClimbManager.start(p),"restart after interruption");
            l.setBlockAndUpdate(ledge.above(),Blocks.STONE.defaultBlockState());
            for(int i=0;i<80&&ClimbManager.active(p);i++)ClimbManager.tick(p);
            require(!ClimbManager.active(p)&&p.getX()<12,"new obstruction stops ascent");
        }finally{
            ClimbManager.cancel(p);ClimbManager.forget(p);
            p.setPos(old.x,old.y,old.z);p.setYRot(yaw);p.setXRot(pitch);p.setOnGround(grounded);
            blocks.forEach(l::setBlockAndUpdate);
        }
    }
}
