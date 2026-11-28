package dev.minemods.treechopper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.minecraft.world.level.block.Block;

final class ForestSticks {
    private ForestSticks(){}
    static void tick(PlayerTickEvent.Post event){
        if(!(event.getEntity() instanceof ServerPlayer player)||!(player.level() instanceof ServerLevel l)||player.isSpectator()||player.isCreative())return;
        if((l.getGameTime()+player.getId())%160!=0)return;
        for(int i=0;i<8;i++){
            int x=player.blockPosition().getX()+l.random.nextInt(19)-9,z=player.blockPosition().getZ()+l.random.nextInt(19)-9;
            if(offer(l,x,z,l.random.nextInt(4)==0))break;
        }
    }
    static boolean offer(ServerLevel l,int x,int z,boolean fromCanopy){
        BlockPos column=new BlockPos(x,l.getMinBuildHeight(),z);
        if(!l.hasChunkAt(column))return false;
        int top=l.getHeight(Heightmap.Types.MOTION_BLOCKING,x,z)-1;
        var leaf=l.getBlockState(new BlockPos(x,top,z));
        if(!TreeChopper.naturalLeaf(leaf))return false;
        var twig=TreeChopper.twigState(leaf,Direction.Plane.HORIZONTAL.getRandomDirection(l.random));
        if(twig==null)return false;
        int lowestLeaf=top;
        for(int y=top;y>=Math.max(l.getMinBuildHeight()+1,top-40);y--){
            BlockPos p=new BlockPos(x,y,z);var state=l.getBlockState(p);
            if(TreeChopper.naturalLeaf(state)){lowestLeaf=y;continue;}
            if(state.getBlock() instanceof TwigBlock||state.is(net.minecraft.tags.BlockTags.LOGS)||state.is(net.minecraft.tags.BlockTags.LEAVES)||!state.getFluidState().isEmpty())return false;
            if(state.getCollisionShape(l,p).isEmpty())continue;
            if(!state.isFaceSturdy(l,p,Direction.UP)||lowestLeaf-y<2)return false;
            BlockPos floor=p.above();
            if(!l.getBlockState(floor).isAir())return false;
            for(BlockPos near:BlockPos.betweenClosed(floor.offset(-2,-2,-2),floor.offset(2,2,2))){
                if(l.hasChunkAt(near)&&l.getBlockState(near).getBlock() instanceof TwigBlock)return false;
            }
            if(!l.getEntitiesOfClass(FallingBlockEntity.class,new net.minecraft.world.phys.AABB(floor).inflate(2,40,2),e->e.getBlockState().getBlock() instanceof TwigBlock).isEmpty())return false;
            if(!fromCanopy)return l.setBlock(floor,twig,Block.UPDATE_ALL);
            BlockPos spawn=new BlockPos(x,lowestLeaf-1,z);
            if(!l.getBlockState(spawn).isAir())return false;
            if(!l.setBlock(spawn,twig,Block.UPDATE_ALL))return false;
            FallingBlockEntity.fall(l,spawn,twig);
            return true;
        }
        return false;
    }
}
