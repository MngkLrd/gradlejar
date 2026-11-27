package dev.minemods.traumacraft.climb;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** One solid block only. Vertical lift precedes horizontal motion; swept boxes reject walls/ceilings. */
public final class ClimbGeometry {
    private ClimbGeometry(){}
    public record Route(BlockPos ledge,Direction facing,Vec3 start,Vec3 end,AABB base){
        public Vec3 position(double progress){
            double rise=ClimbRules.ease((progress-.10)/.52);
            double forward=ClimbRules.ease((progress-.62)/.38);
            return new Vec3(start.x+(end.x-start.x)*forward,start.y+(end.y-start.y)*rise,start.z+(end.z-start.z)*forward);
        }
        public AABB box(Vec3 point){return base.move(point.subtract(start));}
        public boolean clear(Player p,Vec3 previous,Vec3 next){
            return p.level().noCollision(p,box(previous).minmax(box(next)).deflate(1.0e-5));
        }
        public boolean supported(Player p){
            var level=p.level();
            return level.hasChunkAt(ledge)&&level.getWorldBorder().isWithinBounds(ledge)
                &&Block.isShapeFullBlock(level.getBlockState(ledge).getCollisionShape(level,ledge));
        }
    }
    public static Route find(Player p){
        if(!p.onGround()||p.isPassenger()||p.isSwimming()||p.isInWater()||p.isFallFlying()||p.isCrouching())return null;
        Direction facing=Direction.fromYRot(p.getYRot());
        Vec3 start=p.position();
        BlockPos at=BlockPos.containing(start.x+facing.getStepX()*.72,start.y+.04,start.z+facing.getStepZ()*.72);
        double height=at.getY()+1-start.y;
        if(height<.88||height>1.12)return null;
        double distance=switch(facing){
            case EAST -> at.getX()-start.x;
            case WEST -> start.x-(at.getX()+1);
            case SOUTH -> at.getZ()-start.z;
            case NORTH -> start.z-(at.getZ()+1);
            default -> -1;
        };
        if(distance<.29||distance>.72)return null;
        Vec3 end=new Vec3(at.getX()+.5,at.getY()+1.015,at.getZ()+.5);
        var route=new Route(at,facing,start,end,p.getBoundingBox());
        if(!route.supported(p))return null;
        Vec3 prev=start;
        for(int i=1;i<=20;i++){
            Vec3 next=route.position(i/20d);
            if(!route.clear(p,prev,next))return null;
            prev=next;
        }
        return route;
    }
}
