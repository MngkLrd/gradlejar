package dev.minemods.treechopper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;

/** Exact work is persisted on the server. An immutable quantized depth key is synchronized to the client. */
final class CutProgress extends BlockEntity {
    static final ModelProperty<Integer> DEPTHS=new ModelProperty<>();
    private int[] work=new int[4];
    private int guide=-1;
    private double moisturePercent=Double.NaN;
    private volatile int geometryKey;
    private volatile ModelData modelData=ModelData.builder().with(DEPTHS,0).build();
    CutProgress(BlockPos pos,BlockState state){super(TreeChopper.PROGRESS.get(),pos,state);}
    static int index(Direction face){return switch(face){case NORTH->0;case SOUTH->1;case WEST->2;case EAST->3;default->0;};}
    static int geometryKey(BlockGetter l,BlockPos p){return l.getBlockEntity(p) instanceof CutProgress cut?cut.geometryKey:0;}
    static int[] read(Level level,BlockPos pos,BlockState state){return level.getBlockEntity(pos) instanceof CutProgress p?p.work.clone():new int[4];}
    static int guide(Level l,BlockPos pos){return l.getBlockEntity(pos) instanceof CutProgress p?p.guide:-1;}
    static double moisture(Level l,BlockPos pos){return l.getBlockEntity(pos) instanceof CutProgress p?p.moisturePercent:Double.NaN;}
    void rememberMoisture(double value){if(Double.isFinite(value)&&value>=0&&value<=250){moisturePercent=value;setChanged();}}
    void write(int[] value){write(value,-1);}
    void write(int[] value,int face){
        work=value.clone();guide=FallBalance.validGuide(work,face)?face:-1;
        int previous=geometryKey;
        setGeometry(CutGeometry.key(work));setChanged();
        if(previous!=geometryKey&&level!=null&&!level.isClientSide){
            BlockState s=getBlockState();level.sendBlockUpdated(worldPosition,s,s,Block.UPDATE_CLIENTS);
        }
    }
    private void setGeometry(int key){
        key=CutGeometry.sanitize(key);
        if(key==geometryKey)return;
        geometryKey=key;modelData=ModelData.builder().with(DEPTHS,key).build();
    }
    @Override public ModelData getModelData(){return modelData;}
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider lookup){
        CompoundTag tag=new CompoundTag();tag.putInt("depthKey",geometryKey);return tag;
    }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
    @Override public void handleUpdateTag(CompoundTag tag,HolderLookup.Provider lookup){
        int previous=geometryKey;
        setGeometry(tag.getInt("depthKey"));
        if(previous!=geometryKey&&level!=null&&level.isClientSide){
            requestModelDataUpdate();BlockState s=getBlockState();level.sendBlockUpdated(worldPosition,s,s,Block.UPDATE_CLIENTS);
        }
    }
    @Override public void onDataPacket(Connection connection,ClientboundBlockEntityDataPacket packet,HolderLookup.Provider lookup){
        handleUpdateTag(packet.getTag(),lookup);
    }
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider lookup){
        super.saveAdditional(tag,lookup);tag.putIntArray("work",work);if(guide>=0)tag.putInt("guide",guide);if(Double.isFinite(moisturePercent))tag.putDouble("moisturePercent",moisturePercent);
    }
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider lookup){
        super.loadAdditional(tag,lookup);int[] v=tag.getIntArray("work");
        work=v.length==4?v:new int[4];
        for(int i=0;i<4;i++)work[i]=Math.max(0,Math.min(ChopEffort.COMPLETE,work[i]));
        setGeometry(CutGeometry.key(work));
        double wet=tag.contains("moisturePercent")?tag.getDouble("moisturePercent"):Double.NaN;
        moisturePercent=Double.isFinite(wet)&&wet>=0&&wet<=250?wet:Double.NaN;
        int saved=tag.contains("guide")?tag.getInt("guide"):-1;guide=FallBalance.validGuide(work,saved)?saved:-1;
    }
}
