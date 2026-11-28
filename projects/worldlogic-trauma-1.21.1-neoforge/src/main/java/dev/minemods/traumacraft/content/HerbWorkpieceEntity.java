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
    private boolean washed, emptySurface;
    public boolean washed(){return washed;}
    public boolean emptySurface(){return emptySurface;}
    public void prepareSurface(){washed=true;emptySurface=true;mask=0;drySeconds=0;species=HerbSpecies.LEGACY;changed();}
    public HerbWorkpieceEntity(BlockPos pos,BlockState state){super(HerbContent.ENTITY.get(),pos,state);}
    public int mask(){return mask;}
    public HerbSpecies species(){return species;}
    public int drySeconds(){return drySeconds;}
    public void initialize(ItemStack input,HerbSpecies s){
        species=s;emptySurface=false;boolean bundle=input.is(ModItems.RAW_HERBS.get());
        mask=bundle?fromItem(input):0;drySeconds=bundle&&s.botanical()?HerbSpecies.dry(input):0;changed();
    }
    private void changed(){setChanged();if(level!=null&&!level.isClientSide)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);}
    public boolean dryStep(boolean exposed){
        if(!washed||emptySurface||!exposed||!species.botanical()||mask!=0||drySeconds>=HerbSpecies.DRY_SECONDS)return false;
        drySeconds++;changed();return true;
    }
    public void dryTick(){
        if(level instanceof net.minecraft.server.level.ServerLevel server&&level.getGameTime()%20==0&&washed){
            if(!HerbWorkpieceBlock.dryingSurface(level,worldPosition.below())){level.removeBlock(worldPosition,false);return;}
            if(emptySurface){server.sendParticles(net.minecraft.core.particles.ParticleTypes.SPLASH,worldPosition.getX()+.5,worldPosition.getY()+.04,worldPosition.getZ()+.5,2,.26,0,.26,0);return;}
            dryStep(level.canSeeSky(worldPosition.above())&&!level.isRainingAt(worldPosition.above()));
        }
    }
    public void update(int next){
        next&=HerbMask.COMPLETE;if(mask==next)return;mask=next;changed();
    }
    public static int fromItem(ItemStack stack){return stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getInt(MASK)&HerbMask.COMPLETE;}
    public ItemStack result(){
        if(emptySurface)return ItemStack.EMPTY;
        if(HerbMask.finished(mask))return species==HerbSpecies.LEGACY?new ItemStack(ModItems.CRUSHED_HERBS.get(),2):HerbSpecies.powder(species,2,drySeconds);
        ItemStack raw=new ItemStack(ModItems.RAW_HERBS.get());
        if(mask!=0){CompoundTag n=new CompoundTag();n.putInt(MASK,mask);raw.set(DataComponents.CUSTOM_DATA,CustomData.of(n));}
        HerbSpecies.write(raw,species,drySeconds);return raw;
    }
    @Override protected void saveAdditional(CompoundTag n,HolderLookup.Provider registries){super.saveAdditional(n,registries);n.putInt(MASK,mask);n.putString(HerbSpecies.SPECIES,species.id());n.putInt(HerbSpecies.DRY,drySeconds);n.putBoolean("washed_surface",washed);n.putBoolean("empty_surface",emptySurface);}
    @Override protected void loadAdditional(CompoundTag n,HolderLookup.Provider registries){super.loadAdditional(n,registries);mask=n.getInt(MASK)&HerbMask.COMPLETE;species=HerbSpecies.decode(n.getString(HerbSpecies.SPECIES));drySeconds=species.botanical()?HerbSpecies.clampDry(n.getInt(HerbSpecies.DRY)):0;washed=n.getBoolean("washed_surface");emptySurface=washed&&n.getBoolean("empty_surface");}
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries){return saveWithoutMetadata(registries);}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
}
