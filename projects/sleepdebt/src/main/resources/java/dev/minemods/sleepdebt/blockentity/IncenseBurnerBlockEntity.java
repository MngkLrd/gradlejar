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
  remainingTicks+=ticks;setChanged();return true;
 }
 public static void serverTick(Level level,BlockPos pos,BlockState state,IncenseBurnerBlockEntity block){
  if(block.remainingTicks<=0||!(level instanceof ServerLevel server))return;
  block.remainingTicks--;
  if(server.getGameTime()%20==0){
   server.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,
     pos.getX()+.5,pos.getY()+.72,pos.getZ()+.5,2,.12,.06,.12,.005);
  }
  if(server.getGameTime()%40==0){
   for(ServerPlayer player:server.getEntitiesOfClass(ServerPlayer.class,
     new AABB(pos).inflate(7))){
    SleepEngine.soothe(player,6,4);
   }
   block.setChanged();
  }
  if(block.remainingTicks==0)block.setChanged();
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

