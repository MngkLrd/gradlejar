package dev.minemods.treechopper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.minecraft.world.level.block.Block;
import net.minecraft.util.RandomSource;

/** Ground litter is generated once. Runtime ecology only releases rare actual falling branches. */
final class ForestSticks {
    private ForestSticks(){}
    record Site(BlockPos ground,BlockPos canopy,BlockState twig){}
    static void tick(PlayerTickEvent.Post event){
        if(!(event.getEntity() instanceof ServerPlayer player)||!(player.level() instanceof ServerLevel l)||player.isSpectator()||player.isCreative()||player instanceof net.neoforged.neoforge.common.util.FakePlayer)return;
        // One small sampling budget per minute per player, and only 1/3 of those cycles.
        if((l.getGameTime()+player.getId())%1200!=0||l.random.nextInt(3)!=0)return;
        // Nearby players share one ecology sampling source.
        for(var other:l.players())if(other.getId()<player.getId()&&!other.isSpectator()&&!other.isCreative()&&other.distanceToSqr(player)<32*32)return;
        for(int i=0;i<4;i++){
            int x=player.blockPosition().getX()+l.random.nextInt(25)-12,z=player.blockPosition().getZ()+l.random.nextInt(25)-12;
            if(offer(l,x,z,true))break;
        }
    }
    static Site site(LevelReader l,int x,int z,RandomSource random){
        int top=l.getHeight(Heightmap.Types.MOTION_BLOCKING,x,z)-1;
        if(top<=l.getMinBuildHeight())return null;
        var leaf=l.getBlockState(new BlockPos(x,top,z));
        if(!TreeChopper.naturalLeaf(leaf))return null;
        var twig=TreeChopper.twigState(leaf,Direction.Plane.HORIZONTAL.getRandomDirection(random));
        if(twig==null)return null;
        int lowestLeaf=top;
        for(int y=top;y>=Math.max(l.getMinBuildHeight()+1,top-40);y--){
            BlockPos p=new BlockPos(x,y,z);var state=l.getBlockState(p);
            if(TreeChopper.naturalLeaf(state)){lowestLeaf=y;continue;}
            if(state.getBlock() instanceof TwigBlock||state.getBlock() instanceof PebbleBlock||state.is(net.minecraft.tags.BlockTags.LOGS)||state.is(net.minecraft.tags.BlockTags.LEAVES)||!state.getFluidState().isEmpty())return null;
            if(state.getCollisionShape(l,p).isEmpty())continue;
            if(!state.isFaceSturdy(l,p,Direction.UP)||lowestLeaf-y<2||!l.getBlockState(p.above()).isAir())return null;
            return new Site(p.above(),new BlockPos(x,lowestLeaf-1,z),twig);
        }
        return null;
    }
    static boolean offer(ServerLevel l,int x,int z,boolean fromCanopy){
        if(!l.hasChunkAt(new BlockPos(x,l.getMinBuildHeight(),z)))return false;
        var site=site(l,x,z,l.random);
        if(site==null)return false;
        BlockPos floor=site.ground();
        for(BlockPos near:BlockPos.betweenClosed(floor.offset(-1,-1,-1),floor.offset(1,1,1))){
            if(l.hasChunkAt(near)&&l.getBlockState(near).getBlock() instanceof TwigBlock)return false;
        }
        if(!l.getEntitiesOfClass(FallingBlockEntity.class,new net.minecraft.world.phys.AABB(floor).inflate(1,40,1),e->e.getBlockState().getBlock() instanceof TwigBlock).isEmpty())return false;
        // false is used only by the explicit QA fixture, not runtime ecology.
        if(!fromCanopy)return l.setBlock(floor,site.twig(),Block.UPDATE_ALL);
        if(!l.getBlockState(site.canopy()).isAir()||!l.setBlock(site.canopy(),site.twig(),Block.UPDATE_ALL))return false;
        FallingBlockEntity.fall(l,site.canopy(),site.twig());
        return true;
    }
}
