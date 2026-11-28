package dev.minemods.treechopper;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
final class FallenProgress extends BlockEntity {
    UUID group;
    long marker;
    boolean hasPrevious;
    long previous;
    long[] members=new long[0];
    FallenProgress(BlockPos pos,BlockState state){super(TreeChopper.FALLEN_PROGRESS.get(),pos,state);}
    void assign(UUID id,BlockPos anchor,BlockPos prior,long[] cells){
        group=id;marker=anchor.asLong();hasPrevious=prior!=null;previous=prior==null?0:prior.asLong();members=cells.clone();setChanged();
    }
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider lookup){
        super.saveAdditional(tag,lookup);
        if(group!=null){tag.putUUID("group",group);tag.putLong("marker",marker);tag.putBoolean("hasPrevious",hasPrevious);tag.putLong("previous",previous);tag.putLongArray("members",members);}
    }
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider lookup){
        super.loadAdditional(tag,lookup);
        group=tag.hasUUID("group")?tag.getUUID("group"):null;marker=tag.getLong("marker");hasPrevious=tag.getBoolean("hasPrevious");previous=tag.getLong("previous");
        long[] saved=tag.getLongArray("members");members=saved.length<=256?saved:new long[0];
    }
}
