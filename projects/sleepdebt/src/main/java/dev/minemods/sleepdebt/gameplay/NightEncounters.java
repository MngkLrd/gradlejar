package dev.minemods.sleepdebt.gameplay;
import dev.minemods.sleepdebt.SleepSettings;
import dev.minemods.sleepdebt.entity.SleeplessEntity;
import dev.minemods.sleepdebt.registry.ModBlocks;
import dev.minemods.sleepdebt.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.Heightmap;
/** Night encounters separated from fatigue simulation; never generate chunks. */
final class NightEncounters {
 static void trySpawn(ServerPlayer player,int debt){
  if(!SleepSettings.ENABLE_SLEEPLESS.get()||debt<SleepSettings.SLEEPLESS_THRESHOLD.get())return;
  var level=player.serverLevel();
  if(level.isDay()||level.random.nextInt(5)!=0)return;
  if(!level.getEntitiesOfClass(SleeplessEntity.class,player.getBoundingBox().inflate(40)).isEmpty())return;
  for(BlockPos pos:BlockPos.betweenClosed(player.blockPosition().offset(-7,-3,-7),
      player.blockPosition().offset(7,4,7))){
   if(level.getBlockState(pos).is(ModBlocks.DREAMCATCHER.get()))return;
  }
  double angle=level.random.nextDouble()*Math.PI*2;
  int radius=19+level.random.nextInt(11);
  int x=player.getBlockX()+(int)(Math.cos(angle)*radius),z=player.getBlockZ()+(int)(Math.sin(angle)*radius);
  BlockPos probe=new BlockPos(x,player.getBlockY(),z);
  if(!level.hasChunkAt(probe))return;
  int y=level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,x,z);
  BlockPos spawn=new BlockPos(x,y,z);
  if(!level.getBlockState(spawn).isAir()||!level.getBlockState(spawn.above()).isAir())return;
  if(level.getMaxLocalRawBrightness(spawn)>7)return;
  SleeplessEntity mob=ModEntities.SLEEPLESS.get().create(level);
  if(mob==null)return;
  mob.moveTo(x+.5,y,z+.5,level.random.nextFloat()*360,0);
  level.addFreshEntity(mob);
 }
 private NightEncounters(){}
}