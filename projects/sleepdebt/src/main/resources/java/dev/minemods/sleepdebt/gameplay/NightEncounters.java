package dev.minemods.sleepdebt.gameplay;

import dev.minemods.sleepdebt.SleepSettings;
import dev.minemods.sleepdebt.entity.SleeplessEntity;
import dev.minemods.sleepdebt.registry.ModBlocks;
import dev.minemods.sleepdebt.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Controlled first encounter: several attempts on already loaded, genuinely
 * dark blocks rather than a single random unsuitable location. One hunter
 * per player vicinity, with a respawn grace period and protected camps.
 */
final class NightEncounters {
 static void trySpawn(ServerPlayer player,int debt){
  if(!SleepSettings.ENABLE_SLEEPLESS.get() ||
      debt<SleepSettings.SLEEPLESS_THRESHOLD.get() ||
      SleepEngine.hasSleeplessGrace(player))return;
  var level=player.serverLevel();
  if(level.isDay() || player.isSleeping())return;
  int stress=SleepEngine.getStress(player);
  // Higher debt and stress make an encounter more likely, but never guarantee
  // one near the player: avoid cheap unavoidable spawn-kills.
  int roll=debt>=930&&stress>=750?2:3;
  if(level.random.nextInt(roll)!=0)return;
  if(!level.getEntitiesOfClass(SleeplessEntity.class,
    player.getBoundingBox().inflate(46)).isEmpty())return;
  for(BlockPos pos:BlockPos.betweenClosed(player.blockPosition().offset(-7,-3,-7),
      player.blockPosition().offset(7,4,7))){
   if(level.getBlockState(pos).is(ModBlocks.DREAMCATCHER.get()))return;
  }
  for(int attempt=0;attempt<18;attempt++){
   double angle=level.random.nextDouble()*Math.PI*2;
   // More often behind or to the side of a moving player.
   if(attempt<7){
    var look=player.getLookAngle();
    angle=Math.atan2(look.z,look.x)+Math.PI+(level.random.nextDouble()-.5)*2.4;
   }
   int radius=19+level.random.nextInt(13);
   int x=player.getBlockX()+(int)(Math.cos(angle)*radius);
   int z=player.getBlockZ()+(int)(Math.sin(angle)*radius);
   BlockPos probe=new BlockPos(x,player.getBlockY(),z);
   if(!level.hasChunkAt(probe))continue;
   int y=level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,x,z);
   BlockPos spawn=new BlockPos(x,y,z);
   if(Math.abs(y-player.getY())>9 ||
      !level.getBlockState(spawn).isAir() ||
      !level.getBlockState(spawn.above()).isAir() ||
      !level.getFluidState(spawn).isEmpty() ||
      level.getMaxLocalRawBrightness(spawn)>9)continue;
   SleeplessEntity hunter=ModEntities.SLEEPLESS.get().create(level);
   if(hunter==null)return;
   hunter.moveTo(x+.5,y,z+.5,level.random.nextFloat()*360,0);
   level.addFreshEntity(hunter);
   return;
  }
 }
 private NightEncounters(){}
}
