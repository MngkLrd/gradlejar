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
    private HerbSpecies species=HerbSpecies.LEGACY;
    private int drySeconds;
    public HerbWorkpieceEntity(BlockPos pos,BlockState state){super(HerbContent.ENTITY.get(),pos,state);}
    public int mask(){return mask;}
    public HerbSpecies species(){return species;}
    public int drySeconds(){return drySeconds;}
    public void initialize(ItemStack input,HerbSpecies s){
        species=s;boolean bundle=input.is(ModItems.RAW_HERBS.get());
        mask=bundle?fromItem(input):0;drySeconds=bundle&&s.botanical()?HerbSpecies.dry(input):0;changed();
    }
    private void changed(){setChanged();if(level!=null&&!level.isClientSide)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);}
    public boolean dryStep(boolean exposed){
        if(!exposed||!species.botanical()||mask!=0||drySeconds>=HerbSpecies.DRY_SECONDS)return false;
        drySeconds++;changed();return true;
    }
    public void dryTick(){
        if(level!=null&&!level.isClientSide&&level.getGameTime()%20==0)
            dryStep(level.canSeeSky(worldPosition.above())&&!level.isRainingAt(worldPosition.above()));
    }
    public void update(int next){
        next&=HerbMask.COMPLETE;if(mask==next)return;mask=next;changed();
    }
    public static int fromItem(ItemStack stack){return stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getInt(MASK)&HerbMask.COMPLETE;}
    public ItemStack result(){
        if(HerbMask.finished(mask))return species==HerbSpecies.LEGACY?new ItemStack(ModItems.CRUSHED_HERBS.get(),2):HerbSpecies.powder(species,2,drySeconds);
        ItemStack raw=new ItemStack(ModItems.RAW_HERBS.get());
        if(mask!=0){CompoundTag n=new CompoundTag();n.putInt(MASK,mask);raw.set(DataComponents.CUSTOM_DATA,CustomData.of(n));}
        HerbSpecies.write(raw,species,drySeconds);return raw;
    }
    @Override protected void saveAdditional(CompoundTag n,HolderLookup.Provider registries){super.saveAdditional(n,registries);n.putInt(MASK,mask);n.putString(HerbSpecies.SPECIES,species.id());n.putInt(HerbSpecies.DRY,drySeconds);}
    @Override protected void loadAdditional(CompoundTag n,HolderLookup.Provider registries){super.loadAdditional(n,registries);mask=n.getInt(MASK)&HerbMask.COMPLETE;species=HerbSpecies.decode(n.getString(HerbSpecies.SPECIES));drySeconds=species.botanical()?HerbSpecies.clampDry(n.getInt(HerbSpecies.DRY)):0;}
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries){return saveWithoutMetadata(registries);}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
}
