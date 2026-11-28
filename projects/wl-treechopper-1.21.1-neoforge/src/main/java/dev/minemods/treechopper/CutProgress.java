package dev.minemods.treechopper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Persisted per-face work is authoritative; compact visual profile is rebuilt after old-save load. */
final class CutProgress extends BlockEntity {
    private static final Direction[] FACES={Direction.NORTH,Direction.SOUTH,Direction.WEST,Direction.EAST};
    private int[] work;
    private int guide=-1;
    private double moisturePercent=Double.NaN;
    CutProgress(BlockPos pos,BlockState state){super(TreeChopper.PROGRESS.get(),pos,state);}
    static int index(Direction face){return switch(face){case NORTH->0;case SOUTH->1;case WEST->2;case EAST->3;default->0;};}
    static int[] read(Level level,BlockPos pos,BlockState state){
        if(level.getBlockEntity(pos) instanceof CutProgress p&&p.work!=null)return p.work.clone();
        int[] legacy=new int[4];
        if(state.getBlock() instanceof CutBlock)for(int i=0;i<4;i++)legacy[i]=125*CutBlock.depth(state,FACES[i]);
        return legacy;
    }
    static int guide(Level l,BlockPos pos){return l.getBlockEntity(pos) instanceof CutProgress p?p.guide:-1;}
    static double moisture(Level l,BlockPos pos){return l.getBlockEntity(pos) instanceof CutProgress p?p.moisturePercent:Double.NaN;}
    void rememberMoisture(double value){if(Double.isFinite(value)&&value>=0&&value<=250){moisturePercent=value;setChanged();}}
    void write(int[] value){write(value,-1);}
    void write(int[] value,int face){work=value.clone();guide=FallBalance.validGuide(work,face)?face:-1;setChanged();}
    @Override public void onLoad(){
        super.onLoad();
        if(work!=null&&level instanceof net.minecraft.server.level.ServerLevel server&&getBlockState().getBlock() instanceof CutBlock)
            server.scheduleTick(worldPosition,getBlockState().getBlock(),1);
    }
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider lookup){
        super.saveAdditional(tag,lookup);if(work!=null)tag.putIntArray("work",work);if(guide>=0)tag.putInt("guide",guide);if(Double.isFinite(moisturePercent))tag.putDouble("moisturePercent",moisturePercent);
    }
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider lookup){
        super.loadAdditional(tag,lookup);int[] v=tag.getIntArray("work");
        work=v.length==4?v:null;
        if(work!=null)for(int i=0;i<4;i++)work[i]=Math.max(0,Math.min(ChopEffort.COMPLETE,work[i]));
        double wet=tag.contains("moisturePercent")?tag.getDouble("moisturePercent"):Double.NaN;
        moisturePercent=Double.isFinite(wet)&&wet>=0&&wet<=250?wet:Double.NaN;
        int saved=tag.contains("guide")?tag.getInt("guide"):-1;guide=work!=null&&FallBalance.validGuide(work,saved)?saved:-1;
    }
}
