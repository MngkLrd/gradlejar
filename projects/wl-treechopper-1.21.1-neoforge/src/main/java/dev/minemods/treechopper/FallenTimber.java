package dev.minemods.treechopper;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
final class FallenTimber {
    private FallenTimber(){}
    static boolean clear(ServerLevel l,BlockPos p,Set<BlockPos> removed){
        return p.getY()>=l.getMinBuildHeight()&&p.getY()<l.getMaxBuildHeight()&&l.hasChunkAt(p)
            &&(removed.contains(p)||l.getBlockState(p).isAir());
    }
    // The animation's persisted coordinator is the sole owner of the log yield.
    static boolean land(ServerLevel l,BlockPos origin,CompoundTag data){
        ListTag plan=data.getList("landing",Tag.TAG_COMPOUND);
        if(plan.isEmpty()||!data.hasUUID("group"))return true;
        String species=data.getString("species"); UUID group=data.getUUID("group");
        List<BlockPos> positions=new ArrayList<>();
        List<Direction.Axis> axes=new ArrayList<>();
        for(int i=0;i<plan.size();i++){
            CompoundTag entry=plan.getCompound(i);BlockPos p=BlockPos.of(entry.getLong("pos"));
            if(!l.hasChunkAt(p))return false;
            positions.add(p);
            Direction.Axis axis=Direction.Axis.byName(entry.getString("axis"));axes.add(axis==null?Direction.Axis.Y:axis);
        }
        ItemStack log=new ItemStack(TreeChopper.original(species).getBlock());
        // If a player placed an obstruction during the animation, never overwrite it.
        if(positions.stream().anyMatch(p->!clear(l,p,Set.of()))){
            drop(l,origin,log,positions.size());return true;
        }
        int sections=Math.min(3,positions.size());
        List<BlockPos> made=new ArrayList<>();BlockPos prior=null;
        for(int section=0;section<sections;section++){
            int from=section*positions.size()/sections,to=(section+1)*positions.size()/sections;
            int center=from+(to-from-1)/2;
            BlockPos marker=positions.get(center);
            long[] cells=positions.subList(from,to).stream().mapToLong(BlockPos::asLong).toArray();
            for(int i=from;i<to;i++){
                BlockPos pos=positions.get(i);
                if(!l.setBlock(pos,TreeChopper.fallenState(species,axes.get(i),i==center),Block.UPDATE_ALL)){
                    rollback(l,made);drop(l,origin,log,positions.size());return true;
                }
                made.add(pos);
                if(!(l.getBlockEntity(pos) instanceof FallenProgress fp)){
                    rollback(l,made);drop(l,origin,log,positions.size());return true;
                }
                fp.assign(group,marker,prior,i==center?cells:new long[0]);
            }
            prior=marker;
        }
        l.playSound(null,origin,SoundEvents.WOOD_BREAK,SoundSource.BLOCKS,1.3f,.6f);
        return true;
    }
    private static void rollback(ServerLevel l,List<BlockPos> made){for(BlockPos p:made)l.setBlock(p,Blocks.AIR.defaultBlockState(),Block.UPDATE_ALL);}
    private static void drop(ServerLevel l,BlockPos p,ItemStack log,int count){
        while(count>0&&!log.isEmpty()){
            int n=Math.min(count,log.getMaxStackSize());Block.popResource(l,p,log.copyWithCount(n));count-=n;
        }
    }
    private static boolean same(ServerLevel l,BlockPos p,UUID id){
        return l.getBlockEntity(p) instanceof FallenProgress fp&&id.equals(fp.group);
    }
    static boolean process(ServerLevel l,ServerPlayer player,BlockPos pos){
        BlockState state=l.getBlockState(pos);
        if(!(state.getBlock() instanceof FallenLogBlock logBlock))return false;
        FallenProgress fp=l.getBlockEntity(pos) instanceof FallenProgress f?f:null;
        // A missing anchor after external removal cannot permanently strand the remaining logs.
        if(fp==null||fp.group==null)return single(l,player,pos,state,logBlock);
        BlockPos marker=BlockPos.of(fp.marker);
        if(!l.hasChunkAt(marker))return false;
        if(!same(l,marker,fp.group))return single(l,player,pos,state,logBlock);
        if(!marker.equals(pos)){
            player.displayClientMessage(Component.translatable("message.wl_treechopper.find_notch"),true);return false;
        }
        if(fp.hasPrevious){
            BlockPos prior=BlockPos.of(fp.previous);
            if(!l.hasChunkAt(prior)||same(l,prior,fp.group)){
                player.displayClientMessage(Component.translatable("message.wl_treechopper.notch_order"),true);return false;
            }
        }
        if(fp.members.length==0)return single(l,player,pos,state,logBlock);
        List<BlockPos> harvest=new ArrayList<>();
        for(long cell:fp.members){
            BlockPos p=BlockPos.of(cell);
            if(!l.hasChunkAt(p))return false;
            if(!same(l,p,fp.group))continue;
            if(!TreeChopper.allowed(l,player,p,l.getBlockState(p)))return false;
            harvest.add(p);
        }
        int count=0;
        // Remove the marker last, so failures cannot unlock the next section early.
        harvest.sort(Comparator.comparing(p->p.equals(pos)));
        for(BlockPos p:harvest){
            if(l.setBlock(p,Blocks.AIR.defaultBlockState(),Block.UPDATE_ALL))count++;
        }
        if(count>0){drop(l,pos,new ItemStack(TreeChopper.original(logBlock.species).getBlock()),count);l.playSound(null,pos,SoundEvents.WOOD_BREAK,SoundSource.BLOCKS,1f,1f);}
        return count>0;
    }
    private static boolean single(ServerLevel l,ServerPlayer player,BlockPos pos,BlockState state,FallenLogBlock b){
        if(!TreeChopper.allowed(l,player,pos,state)||!l.setBlock(pos,Blocks.AIR.defaultBlockState(),Block.UPDATE_ALL))return false;
        drop(l,pos,new ItemStack(TreeChopper.original(b.species).getBlock()),1);return true;
    }
}
