package dev.minemods.worldlogic;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.*;
public final class Weather {
 private static final Map<Player,Integer> MUD=new WeakHashMap<>();
 public static void tick(PlayerTickEvent.Post e){Player p=e.getEntity();if(!(p.level() instanceof ServerLevel l)||p.isSpectator()||p.isCreative())return;
  long now=l.getGameTime();BlockPos pos=p.getOnPos();var s=l.getBlockState(pos);Vec3 v=p.getDeltaMovement();
  if(now%8!=0)return;
  boolean moving=p.onGround()&&v.horizontalDistance()>=.035;
  if(moving&&Rules.on("mud_tracks")){
   int steps=MUD.getOrDefault(p,0);if(s.is(Blocks.MUD)||s.is(Blocks.DIRT)&&l.isRainingAt(pos.above()))steps=6;
   if(steps>0){var shape=s.getCollisionShape(l,pos);if(!shape.isEmpty()){
    double yaw=Math.toRadians(p.getYRot());double side=now%16==0?.10:-.10;
    l.sendParticles(Content.FOOTPRINT.get(),p.getX()+Math.cos(yaw)*side,pos.getY()+shape.max(Direction.Axis.Y)+.012,p.getZ()+Math.sin(yaw)*side,0,yaw,0,0,1);
   }MUD.put(p,steps-1);}
  }
  BlockPos snow=p.blockPosition();var b=l.getBlockState(snow);
  if(moving&&Rules.on("snow_compression")&&b.is(Blocks.SNOW)){
   var data=WorldState.get(l);long key=snow.asLong();int old=data.snow.getOrDefault(key,0);if(old<8&&(data.snow.size()<32768||data.snow.containsKey(key))){data.snow.put(key,old+1);data.setDirty();}
   if(old>=4&&v.horizontalDistance()<.25){p.setDeltaMovement(v.x*1.025,v.y,v.z*1.025);}
  }
  if(Rules.on("snow_weight")&&l.isRaining()&&now%40==0){
   BlockPos q=p.blockPosition().offset(l.random.nextInt(7)-3,1+l.random.nextInt(5),l.random.nextInt(7)-3);
   if(l.hasChunkAt(q)&&l.getBlockState(q).is(Blocks.SNOW)&&l.getBlockState(q.below()).getBlock() instanceof LeavesBlock&&l.getBlockState(q).getValue(SnowLayerBlock.LAYERS)>=4){
    for(Direction d:Direction.Plane.HORIZONTAL){BlockPos t=q.relative(d);if(l.hasChunkAt(t)&&l.isEmptyBlock(t)&&l.isEmptyBlock(t.below())){var original=l.getBlockState(q);int layers=original.getValue(SnowLayerBlock.LAYERS);BlockPos landing=t;for(int fall=0;fall<12&&l.hasChunkAt(landing.below())&&l.isEmptyBlock(landing.below());fall++)landing=landing.below();if(!Blocks.SNOW.defaultBlockState().canSurvive(l,landing))continue;if(!l.setBlock(landing,Blocks.SNOW.defaultBlockState(),3))continue;l.setBlock(q,original.setValue(SnowLayerBlock.LAYERS,layers-1),3);l.sendParticles(ParticleTypes.SNOWFLAKE,t.getX()+.5,t.getY(),t.getZ()+.5,12,.2,.4,.2,.02);break;}}
   }
  }
  if(Rules.on("rain_weight")&&l.isRainingAt(p.blockPosition())&&l.getBlockState(p.blockPosition().above(2)).getBlock() instanceof LeavesBlock)l.sendParticles(ParticleTypes.DRIPPING_WATER,p.getX(),p.getY()+2,p.getZ(),2,.35,.1,.35,0);
 }
}
