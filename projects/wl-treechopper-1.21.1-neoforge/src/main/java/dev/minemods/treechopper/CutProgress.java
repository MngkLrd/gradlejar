package dev.minemods.treechopper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Per-face work is saved on the cut block; geometry remains a compact 4-stage blockstate. */
final class CutProgress extends BlockEntity {
    private static final Direction[] FACES={Direction.NORTH,Direction.SOUTH,Direction.WEST,Direction.EAST};
    private int[] work;
    CutProgress(BlockPos pos,BlockState state){super(TreeChopper.PROGRESS.get(),pos,state);}
    static int index(Direction face){return switch(face){case NORTH->0;case SOUTH->1;case WEST->2;case EAST->3;default->0;};}
    static int[] read(Level level,BlockPos pos,BlockState state){
        if(level.getBlockEntity(pos) instanceof CutProgress p&&p.work!=null)return p.work.clone();
        int[] legacy=new int[4];
        if(state.getBlock() instanceof CutBlock)for(int i=0;i<4;i++)legacy[i]=250*CutBlock.depth(state,FACES[i]);
        return legacy;
    }
    void write(int[] value){work=value.clone();setChanged();}
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider lookup){
        super.saveAdditional(tag,lookup);if(work!=null)tag.putIntArray("work",work);
    }
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider lookup){
        super.loadAdditional(tag,lookup);int[] v=tag.getIntArray("work");
        work=v.length==4?v:null;
        if(work!=null)for(int i=0;i<4;i++)work[i]=Math.max(0,Math.min(ChopEffort.COMPLETE,work[i]));
    }
}
