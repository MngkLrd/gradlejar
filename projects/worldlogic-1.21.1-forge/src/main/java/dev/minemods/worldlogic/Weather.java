package dev.minemods.worldlogic;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
public final class Weather {
 private static final Map<ServerLevel,Map<BlockPos,Long>> SNOW=new WeakHashMap<>();
 public static void tick(TickEvent.PlayerTickEvent.Post e){Player p=e.player;if(!(p.level() instanceof ServerLevel l)||p.isSpectator()||p.isCreative())return;
  long now=l.getGameTime();BlockPos pos=p.getOnPos();var s=l.getBlockState(pos);Vec3 v=p.getDeltaMovement();
  if(Rules.on("slippery_rain")&&p.onGround()&&l.isRainingAt(pos.above())&&Material.block(s,"slippery")&&v.horizontalDistance()>.04&&v.horizontalDistance()<.32){p.setDeltaMovement(v.x*1.045,v.y,v.z*1.045);}
  if(now%8!=0||!p.onGround()||v.horizontalDistance()<.035)return;
  if(Rules.on("mud_tracks")&&(s.is(Blocks.MUD)||s.is(Blocks.DIRT)&&l.isRainingAt(pos.above())))l.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK,s),p.getX(),p.getY()+.02,p.getZ(),3,.12,.01,.12,.01);
  BlockPos snow=p.blockPosition();var b=l.getBlockState(snow);
  if(Rules.on("snow_compression")&&b.is(Blocks.SNOW)&&b.getValue(SnowLayerBlock.LAYERS)>1){
   var map=SNOW.computeIfAbsent(l,k->new HashMap<>());if(map.size()>2048)map.clear();long last=map.getOrDefault(snow,0L);
   if(last>0&&now-last<100&&l.random.nextFloat()<.08){l.setBlock(snow,b.setValue(SnowLayerBlock.LAYERS,b.getValue(SnowLayerBlock.LAYERS)-1),3);map.remove(snow);}else map.put(snow.immutable(),now);
  }
  if(Rules.on("snow_weight")&&l.isRaining()&&now%40==0){
   BlockPos q=p.blockPosition().offset(l.random.nextInt(7)-3,1+l.random.nextInt(5),l.random.nextInt(7)-3);
   if(l.hasChunkAt(q)&&l.getBlockState(q).is(Blocks.SNOW)&&l.getBlockState(q.below()).getBlock() instanceof LeavesBlock&&l.getBlockState(q).getValue(SnowLayerBlock.LAYERS)>=4){
    for(Direction d:Direction.Plane.HORIZONTAL){BlockPos t=q.relative(d);if(l.hasChunkAt(t)&&l.isEmptyBlock(t)&&l.isEmptyBlock(t.below())){l.setBlock(q,l.getBlockState(q).setValue(SnowLayerBlock.LAYERS,3),3);l.sendParticles(ParticleTypes.SNOWFLAKE,t.getX()+.5,t.getY(),t.getZ()+.5,12,.2,.4,.2,.02);break;}}
   }
  }
  if(Rules.on("rain_weight")&&l.isRainingAt(p.blockPosition())&&l.getBlockState(p.blockPosition().above(2)).getBlock() instanceof LeavesBlock)l.sendParticles(ParticleTypes.DRIPPING_WATER,p.getX(),p.getY()+2,p.getZ(),2,.35,.1,.35,0);
 }
}
