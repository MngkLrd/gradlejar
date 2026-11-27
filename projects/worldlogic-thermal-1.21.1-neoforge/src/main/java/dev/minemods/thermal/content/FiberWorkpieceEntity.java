package dev.minemods.thermal.content;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
public final class FiberWorkpieceEntity extends BlockEntity {
    private static final String MASK="fiber_mask";
    private long mask;
    public FiberWorkpieceEntity(BlockPos pos,BlockState state){super(ThermalContent.WORKPIECE_ENTITY.get(),pos,state);}
    public long mask(){return mask;}
    public void update(long next){
        if(mask==next)return;mask=next;setChanged();
        if(level!=null&&!level.isClientSide)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);
    }
    public static long fromItem(ItemStack stack){return stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getLong(MASK);}
    public ItemStack result(){
        if(FiberMask.finished(mask))return new ItemStack(ThermalContent.FIBER.get(),4);
        ItemStack raw=new ItemStack(ThermalContent.RAW_FIBER.get());
        if(mask!=0){CompoundTag n=new CompoundTag();n.putLong(MASK,mask);raw.set(DataComponents.CUSTOM_DATA,CustomData.of(n));}
        return raw;
    }
    @Override protected void saveAdditional(CompoundTag n,HolderLookup.Provider registries){super.saveAdditional(n,registries);n.putLong(MASK,mask);}
    @Override protected void loadAdditional(CompoundTag n,HolderLookup.Provider registries){super.loadAdditional(n,registries);mask=n.getLong(MASK);}
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries){return saveWithoutMetadata(registries);}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
}
