package dev.minemods.treechopper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Sparse renewable ground sticks, using ordinary pickup/despawn and loaded natural canopy. */
final class ForestSticks {
    private static final String TAG="wl_treechopper_forest_stick";
    private ForestSticks(){}
    static void tick(PlayerTickEvent.Post event){
        if(!(event.getEntity() instanceof ServerPlayer player)||!(player.level() instanceof ServerLevel l)||player.isSpectator()||player.isCreative())return;
        if((l.getGameTime()+player.getId())%160!=0)return;
        if(l.getEntitiesOfClass(ItemEntity.class,new AABB(player.blockPosition()).inflate(16,12,16),ForestSticks::isForestStick).size()>=8)return;
        for(int i=0;i<8;i++){
            int x=player.blockPosition().getX()+l.random.nextInt(19)-9,z=player.blockPosition().getZ()+l.random.nextInt(19)-9;
            if(offer(l,x,z,l.random.nextInt(4)==0))break;
        }
    }
    static boolean isForestStick(ItemEntity e){return e.getTags().contains(TAG)&&e.getItem().is(Items.STICK);}
    static boolean offer(ServerLevel l,int x,int z,boolean fromCanopy){
        BlockPos column=new BlockPos(x,l.getMinBuildHeight(),z);
        if(!l.hasChunkAt(column))return false;
        int top=l.getHeight(Heightmap.Types.MOTION_BLOCKING,x,z)-1;
        if(!TreeChopper.naturalLeaf(l.getBlockState(new BlockPos(x,top,z))))return false;
        int lowestLeaf=top;
        for(int y=top;y>=Math.max(l.getMinBuildHeight()+1,top-40);y--){
            BlockPos p=new BlockPos(x,y,z);var state=l.getBlockState(p);
            if(TreeChopper.naturalLeaf(state)){lowestLeaf=y;continue;}
            if(state.is(BlockTags.LOGS)||state.is(BlockTags.LEAVES)||!state.getFluidState().isEmpty())return false;
            if(state.getCollisionShape(l,p).isEmpty())continue;
            if(!state.isFaceSturdy(l,p,Direction.UP)||lowestLeaf-y<2)return false;
            BlockPos floor=p.above();
            if(!l.getEntitiesOfClass(ItemEntity.class,new AABB(floor).inflate(2,40,2),ForestSticks::isForestStick).isEmpty())return false;
            int spawnY=fromCanopy?lowestLeaf-1:floor.getY();
            BlockPos spawn=new BlockPos(x,spawnY,z);
            if(!l.getBlockState(spawn).getCollisionShape(l,spawn).isEmpty())spawn=floor;
            if(!l.getBlockState(spawn).getCollisionShape(l,spawn).isEmpty())return false;
            ItemEntity stick=new ItemEntity(l,x+.5,spawn.getY()+.1,z+.5,new ItemStack(Items.STICK));
            stick.setDeltaMovement(0,0,0);stick.setDefaultPickUpDelay();stick.addTag(TAG);
            return l.addFreshEntity(stick);
        }
        return false;
    }
}
