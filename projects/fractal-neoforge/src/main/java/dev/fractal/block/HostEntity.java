package dev.fractal.block;
import dev.fractal.Fractal;
import dev.fractal.cell.Preview;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import java.util.Arrays;

public final class HostEntity extends BlockEntity {
 private String encoded="";
 private BlockState[] tiles=new BlockState[0];
 public HostEntity(BlockPos pos,BlockState state){super(Fractal.HOST_ENTITY.get(),pos,state);}
 public BlockState[] tiles(){return tiles;}
 public void setPreview(String next){
  if(encoded.equals(next))return;
  decode(next);setChanged();
  if(level!=null&&!level.isClientSide)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);
 }
 private void decode(String value){
  encoded=value.length()>20000?"":value;
  if(encoded.isEmpty()){tiles=new BlockState[0];return;}
  String[] parts=encoded.split(";",-1);
  if(parts.length!=Preview.VOLUME){tiles=new BlockState[0];return;}
  tiles=new BlockState[parts.length];
  for(int i=0;i<parts.length;i++){
   ResourceLocation id=ResourceLocation.tryParse(parts[i]);
   tiles[i]=id==null?Blocks.AIR.defaultBlockState():BuiltInRegistries.BLOCK.get(id).defaultBlockState();
  }
 }
 @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider registries){super.saveAdditional(tag,registries);tag.putString("preview",encoded);}
 @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider registries){super.loadAdditional(tag,registries);decode(tag.getString("preview"));}
 @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries){CompoundTag t=new CompoundTag();saveAdditional(t,registries);return t;}
 @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
}
