package dev.minemods.sleepdebt.blockentity;

import dev.minemods.sleepdebt.gameplay.SleepEngine;
import dev.minemods.sleepdebt.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/** Server-driven area effect; does not keep chunks loaded or create entities. */
public final class IncenseBurnerBlockEntity extends BlockEntity {
 public static final int MAX_TICKS=12_000;
 private int remainingTicks;
 public IncenseBurnerBlockEntity(BlockPos pos,BlockState state){
  super(ModBlockEntities.INCENSE_BURNER.get(),pos,state);
 }
 public int remainingTicks(){return remainingTicks;}
 public boolean addFuel(int ticks){
  if(remainingTicks>MAX_TICKS-ticks)return false;
  remainingTicks+=ticks;setChanged();syncFill();return true;
 }
 public static void serverTick(Level level,BlockPos pos,BlockState state,IncenseBurnerBlockEntity block){
  if(!(level instanceof ServerLevel server))return;
  if(block.remainingTicks<=0){block.syncFill();return;}
  block.remainingTicks--;
  block.syncFill();
  if(server.getGameTime()%20==0){
   server.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,
     pos.getX()+.5,pos.getY()+.72,pos.getZ()+.5,2,.12,.06,.12,.005);
  }
  if(server.getGameTime()%40==0){
   for(ServerPlayer player:server.getEntitiesOfClass(ServerPlayer.class,
     new AABB(pos).inflate(7))){
    SleepEngine.incense(player);
   }
   block.setChanged();
  }
  if(block.remainingTicks==0)block.setChanged();
 }
 private void syncFill(){
  if(level==null||level.isClientSide)return;
  var state=level.getBlockState(worldPosition);
  if(state.getBlock() instanceof dev.minemods.sleepdebt.block.IncenseBurnerBlock){
   var property=dev.minemods.sleepdebt.block.IncenseBurnerBlock.FILL;
   int fill=dev.minemods.sleepdebt.block.IncenseBurnerBlock.visualFill(remainingTicks);
   if(state.getValue(property)!=fill)level.setBlock(worldPosition,state.setValue(property,fill),3);
  }
 }
 @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider provider){
  super.loadAdditional(tag,provider);
  remainingTicks=Math.max(0,Math.min(MAX_TICKS,tag.getInt("BurnTime")));
 }
 @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider provider){
  super.saveAdditional(tag,provider);
  tag.putInt("BurnTime",remainingTicks);
 }
}

