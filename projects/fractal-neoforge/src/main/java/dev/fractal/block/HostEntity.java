package dev.fractal.block;
import dev.fractal.Fractal;
import dev.fractal.cell.Preview;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import java.util.*;

public final class HostEntity extends BlockEntity {
 private String encoded="";
 public record Tile(int x,int y,int z,BlockState state){}
 private List<Tile> visible=List.of();
 public HostEntity(BlockPos pos,BlockState state){super(Fractal.HOST_ENTITY.get(),pos,state);}
 public List<Tile> visible(){return visible;}
 public void setPreview(String next){
  if(encoded.equals(next))return;
  decode(next);setChanged();
  if(level!=null&&!level.isClientSide)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);
 }
 private void decode(String value){
  encoded=value.length()>Preview.MAX_ENCODED?"":value;
  visible=List.of();
  var decoded=Preview.unpack(encoded);if(decoded==null)return;
  BlockState[] palette=new BlockState[decoded.palette().length];
  for(int i=0;i<palette.length;i++){
   ResourceLocation id=ResourceLocation.tryParse(decoded.palette()[i]);
   palette[i]=id==null?Blocks.AIR.defaultBlockState():BuiltInRegistries.BLOCK.get(id).defaultBlockState();
  }
  short[] ids=decoded.voxels();ArrayList<Tile> mesh=new ArrayList<>();
  for(int y=0;y<Preview.HEIGHT;y++)for(int z=0;z<Preview.WIDTH;z++)for(int x=0;x<Preview.WIDTH;x++){
   BlockState s=palette[ids[Preview.index(x,y,z)]];
   if(s.isAir())continue;
   if(opaque(palette,ids,x-1,y,z)&&opaque(palette,ids,x+1,y,z)&&opaque(palette,ids,x,y-1,z)&&opaque(palette,ids,x,y+1,z)&&opaque(palette,ids,x,y,z-1)&&opaque(palette,ids,x,y,z+1))continue;
   mesh.add(new Tile(x,y,z,s));
  }
  visible=List.copyOf(mesh);
 }
 private static boolean opaque(BlockState[] p,short[] ids,int x,int y,int z){
  if(x<0||x>=Preview.WIDTH||z<0||z>=Preview.WIDTH||y<0||y>=Preview.HEIGHT)return false;
  return p[ids[Preview.index(x,y,z)]].canOcclude();
 }
 @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider registries){super.saveAdditional(tag,registries);tag.putString("preview",encoded);}
 @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider registries){super.loadAdditional(tag,registries);decode(tag.getString("preview"));}
 @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries){CompoundTag t=new CompoundTag();saveAdditional(t,registries);return t;}
 @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
}
