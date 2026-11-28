package dev.minemods.excavation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;

/** Saved only for touched rock; the client receives geometry, never authority over drops. */
final class ChipProgress extends BlockEntity {
    static final ModelProperty<Integer> DEPTHS=new ModelProperty<>();
    private FractureMath.State fracture=new FractureMath.State(0,0,0);
    private boolean valid;
    private volatile int geometry;
    private volatile ModelData model=ModelData.builder().with(DEPTHS,0).build();
    private double incident=Double.NaN;
    ChipProgress(BlockPos pos,BlockState state){super(Excavation.PROGRESS.get(),pos,state);}
    static int key(BlockGetter level,BlockPos pos){return level.getBlockEntity(pos) instanceof ChipProgress p?p.geometry:0;}
    FractureMath.State read(){return valid?fracture:null;}
    double energy(){return incident;}
    void write(FractureMath.Plan plan,double energy){
        if(plan.complete())throw new IllegalArgumentException("Cannot save completed fracture");
        fracture=new FractureMath.State(plan.work(),plan.extracted(),plan.key());valid=true;incident=energy;
        updateGeometry(plan.key());setChanged();
        if(level!=null&&!level.isClientSide){var state=getBlockState();level.sendBlockUpdated(worldPosition,state,state,Block.UPDATE_CLIENTS);}
    }
    private void updateGeometry(int key){
        geometry=FractureMath.sanitize(key);model=ModelData.builder().with(DEPTHS,geometry).build();
    }
    @Override public ModelData getModelData(){return model;}
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider lookup){CompoundTag t=new CompoundTag();t.putInt("geometry",geometry);return t;}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
    @Override public void handleUpdateTag(CompoundTag tag,HolderLookup.Provider lookup){
        updateGeometry(tag.getInt("geometry"));
        if(level!=null&&level.isClientSide){requestModelDataUpdate();var s=getBlockState();level.sendBlockUpdated(worldPosition,s,s,Block.UPDATE_CLIENTS);}
    }
    @Override public void onDataPacket(Connection connection,ClientboundBlockEntityDataPacket packet,HolderLookup.Provider lookup){handleUpdateTag(packet.getTag(),lookup);}
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider lookup){
        super.saveAdditional(tag,lookup);tag.putBoolean("valid",valid);tag.putInt("work",fracture.work());
        tag.putInt("extracted",fracture.extracted());tag.putInt("geometry",fracture.key());
        if(Double.isFinite(incident))tag.putDouble("incidentJm2",incident);
    }
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider lookup){
        super.loadAdditional(tag,lookup);
        try{
            fracture=new FractureMath.State(tag.getInt("work"),tag.getInt("extracted"),tag.getInt("geometry"));
            valid=tag.getBoolean("valid")&&fracture.work()>0&&fracture.key()!=0;
            if(getBlockState().getBlock() instanceof ChipBlock b&&b.spec.ore()&&fracture.extracted()==0)valid=false;
            if(getBlockState().getBlock() instanceof ChipBlock b&&!b.spec.ore()&&fracture.extracted()!=0)valid=false;
        }catch(IllegalArgumentException ex){valid=false;fracture=new FractureMath.State(0,0,0);}
        incident=tag.contains("incidentJm2")?tag.getDouble("incidentJm2"):Double.NaN;updateGeometry(fracture.key());
    }
}
