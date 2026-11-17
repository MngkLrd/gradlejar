package com.kirill.lurelantern.blockentity;
import com.kirill.lurelantern.block.LureLanternBlock;
import com.kirill.lurelantern.registry.ModBlockEntities;
import com.kirill.lurelantern.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
public final class LureLanternBlockEntity extends BlockEntity {
 public enum Mode {OFF,LURE,AUTO_CAPTURE;
  public Mode next(){return this==OFF?LURE:this==LURE?AUTO_CAPTURE:OFF;}
 }
 private CompoundTag mob;
 private Mode mode=Mode.OFF;
 private int waitTicks=5;
 public LureLanternBlockEntity(BlockPos pos,BlockState state){super(ModBlockEntities.LURE.get(),pos,state);}
 public boolean hasMob(){return mob!=null;}
 public CompoundTag previewMobTag(){return mob==null?null:mob.copy();}
 public String mobName(){return mob==null?"":mob.getStringOr("id","");}
 public Mode mode(){return mode;}
 public void setMode(Mode m){mode=m;sync();}
 public void loadStack(ItemStack stack){mob=CapturedMobData.mob(stack).orElse(null);sync();}
 public void saveStack(ItemStack stack){if(mob!=null)CapturedMobData.setMob(stack,mob);}
 public boolean release(ServerLevel server,BlockPos pos){
  if(mob==null)return false;
  ItemStack temp=new ItemStack(getBlockState().getBlock());CapturedMobData.setMob(temp,mob);
  if(!LureLanternLogic.release(server,pos,temp))return false;
  mob=null;sync();return true;
 }
 public static void serverTick(Level level,BlockPos pos,BlockState state,LureLanternBlockEntity be){
  if(!(level instanceof ServerLevel server)||!state.getValue(LureLanternBlock.ACTIVE)||--be.waitTicks>0)return;
  be.waitTicks=5;
  Vec3 c=Vec3.atCenterOf(pos);
  if(state.getBlock()==com.kirill.lurelantern.registry.ModBlocks.ADVANCED.get()){
    if(be.mode==Mode.OFF||be.hasMob())return;
    if(be.mode==Mode.AUTO_CAPTURE){
      ItemStack temp=new ItemStack(state.getBlock());
      if(LureLanternLogic.captureNearest(server,c,temp)){
        be.mob=CapturedMobData.mob(temp).orElse(null);
        be.setMode(Mode.OFF);server.setBlock(pos,state.setValue(LureLanternBlock.ACTIVE,false),3);return;
      }
    }
  }
  LureLanternLogic.lureMobs(server,c);
 }
 @Override protected void saveAdditional(ValueOutput out){super.saveAdditional(out);if(mob!=null)out.store("CapturedMob",CompoundTag.CODEC,mob);out.putInt("LureMode",mode.ordinal());}
 @Override protected void loadAdditional(ValueInput in){
  super.loadAdditional(in);mob=in.read("CapturedMob",CompoundTag.CODEC).orElse(null);
  mode=Mode.values()[Math.clamp(in.getIntOr("LureMode",0),0,2)];
 }
 @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries){return saveCustomOnly(registries);}
 @Nullable @Override public Packet<ClientGamePacketListener> getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
 private void sync(){setChanged();if(level!=null)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);}
}
