package dev.minemods.traumacraft.content;
import dev.minemods.traumacraft.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
public final class HerbWorkpieceEntity extends BlockEntity {
    private static final String MASK="herb_mask";
    private int mask;
    public HerbWorkpieceEntity(BlockPos pos,BlockState state){super(HerbContent.ENTITY.get(),pos,state);}
    public int mask(){return mask;}
    public void update(int next){
        next&=HerbMask.COMPLETE;if(mask==next)return;mask=next;setChanged();
        if(level!=null&&!level.isClientSide)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);
    }
    public static int fromItem(ItemStack stack){return stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getInt(MASK)&HerbMask.COMPLETE;}
    public ItemStack result(){
        if(HerbMask.finished(mask))return new ItemStack(ModItems.CRUSHED_HERBS.get(),2);
        ItemStack raw=new ItemStack(ModItems.RAW_HERBS.get());
        if(mask!=0){CompoundTag n=new CompoundTag();n.putInt(MASK,mask);raw.set(DataComponents.CUSTOM_DATA,CustomData.of(n));}
        return raw;
    }
    @Override protected void saveAdditional(CompoundTag n,HolderLookup.Provider registries){super.saveAdditional(n,registries);n.putInt(MASK,mask);}
    @Override protected void loadAdditional(CompoundTag n,HolderLookup.Provider registries){super.loadAdditional(n,registries);mask=n.getInt(MASK)&HerbMask.COMPLETE;}
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries){return saveWithoutMetadata(registries);}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
}
