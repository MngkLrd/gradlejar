package dev.worldfold.fold;

import dev.worldfold.WorldFold;
import dev.worldfold.block.SeamAnchorBlock;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public final class PocketSpace {
    public static final ResourceKey<Level> KEY=ResourceKey.create(Registries.DIMENSION,ResourceLocation.fromNamespaceAndPath("worldfold","foldspace"));
    private static final int SPACING=96,Y=80;
    public static final int[] SIZES={17,25,33};
    public static final int[] COSTS={1,2,4};
    private PocketSpace(){}
    private static int x(int index){return (index%1024)*SPACING+48;}
    private static int z(int index){return (index/1024)*SPACING+48;}
    public static Anchor build(MinecraftServer server,int index,int tier){
        ServerLevel l=server.getLevel(KEY);if(l==null)throw new IllegalStateException("Foldspace dimension unavailable");
        int half=SIZES[tier]/2,top=7+2*tier,cx=x(index),cz=z(index);
        BlockState floor=Blocks.POLISHED_DEEPSLATE.defaultBlockState(),wall=Blocks.DEEPSLATE_BRICKS.defaultBlockState(),
            trim=Blocks.POLISHED_BLACKSTONE.defaultBlockState(),light=Blocks.SEA_LANTERN.defaultBlockState(),accent=Blocks.AMETHYST_BLOCK.defaultBlockState();
        BlockPos.MutableBlockPos p=new BlockPos.MutableBlockPos();
        for(int dx=-half;dx<=half;dx++)for(int dz=-half;dz<=half;dz++){
            boolean border=Math.abs(dx)==half||Math.abs(dz)==half;
            boolean rib=(Math.abs(dx)%4==0&&Math.abs(dz)%4==0);
            l.setBlock(p.set(cx+dx,Y,cz+dz),rib?light:border?trim:floor,3);
            l.setBlock(p.set(cx+dx,Y+top,cz+dz),rib?light:border?trim:wall,3);
            if(border)for(int dy=1;dy<top;dy++)
                l.setBlock(p.set(cx+dx,Y+dy,cz+dz),(dy==1||dy==top-1)?trim:((dx+dz)%4==0?accent:wall),3);
        }
        BlockPos node=new BlockPos(cx,Y,cz-half+4);
        l.setBlock(node,WorldFold.ANCHOR.get().defaultBlockState().setValue(SeamAnchorBlock.FACING,Direction.SOUTH),3);
        Anchor a=Anchor.at(l,node,3,3,1);a.loadChunks(l);
        if(!a.clear(l))throw new IllegalStateException("Pocket entry obstructed");
        return a;
    }
    public static int recall(ServerPlayer p){
        if(!p.serverLevel().dimension().equals(KEY))return 0;
        MinecraftServer server=p.getServer();FoldData data=FoldData.get(server);
        int ix=Math.floorDiv(p.blockPosition().getX(),SPACING),iz=Math.floorDiv(p.blockPosition().getZ(),SPACING);
        Anchor a=ix>=0&&ix<1024&&iz>=0?data.pocketReturns.get(iz*1024+ix):null;
        ServerLevel target=a==null?server.overworld():a.level(server);
        if(target==null)target=server.overworld();
        BlockPos base=a==null||!target.dimension().equals(a.dimension())?target.getSharedSpawnPos():a.floor().relative(a.normal(),2).above();
        // Find a dry, collision-free two-block landing close to the return anchor.
        BlockPos safe=null;
        for(int r=0;r<=6&&safe==null;r++)for(int dy=-2;dy<=4&&safe==null;dy++)
            for(int dx=-r;dx<=r&&safe==null;dx++)for(int dz=-r;dz<=r&&safe==null;dz++){
                BlockPos pos=base.offset(dx,dy,dz);
                if(!target.getWorldBorder().isWithinBounds(pos)||pos.getY()<=target.getMinBuildHeight()||pos.getY()+2>=target.getMaxBuildHeight())continue;
                if(target.getBlockState(pos.below()).isFaceSturdy(target,pos.below(),Direction.UP)
                    &&target.getFluidState(pos).isEmpty()&&target.getFluidState(pos.above()).isEmpty()
                    &&!target.getBlockCollisions(null,new AABB(pos.getX()+.15,pos.getY(),pos.getZ()+.15,pos.getX()+.85,pos.getY()+1.9,pos.getZ()+.85)).iterator().hasNext())safe=pos;
            }
        if(safe==null){target=server.overworld();safe=target.getSharedSpawnPos().above();}
        p.teleportTo(target,safe.getX()+.5,safe.getY(),safe.getZ()+.5,p.getYRot(),p.getXRot());
        p.resetFallDistance();return 1;
    }
}
