package dev.minemods.worldlogic;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.BlockEvent;
public final class TorchLifetime {
 static long lifetime(double strength){return Math.round(36000/Math.max(.25,strength));}
 static boolean lit(BlockState s){return s.is(Blocks.TORCH)||s.is(Blocks.WALL_TORCH);}
 static void register(ServerLevel l,BlockPos p){
  if(!Rules.on("torch_lifetime"))return;var data=TorchLifeData.get(l);long k=p.asLong();
  if(data.deadline.size()<32768||data.deadline.containsKey(k)){data.deadline.put(k,l.getGameTime()+lifetime(Rules.strength("fire")));data.setDirty();}
 }
 public static void placed(BlockEvent.EntityPlaceEvent e){if(!e.isCanceled()&&e.getLevel() instanceof ServerLevel l&&lit(e.getPlacedBlock()))register(l,e.getPos());}
 public static void broken(BlockEvent.BreakEvent e){if(!e.isCanceled()&&e.getLevel() instanceof ServerLevel l&&lit(e.getState()))forget(l,e.getPos());}
 static void forget(ServerLevel l,BlockPos p){var d=TorchLifeData.get(l);if(d.deadline.remove(p.asLong())!=null)d.setDirty();}
 static boolean extinguish(ServerLevel l,BlockPos p,BlockState s){
  if(!lit(s))return false;BlockState next=(s.is(Blocks.TORCH)?Content.TORCH.get():Content.WALL_TORCH.get()).defaultBlockState();
  if(s.hasProperty(BlockStateProperties.HORIZONTAL_FACING))next=next.setValue(BlockStateProperties.HORIZONTAL_FACING,s.getValue(BlockStateProperties.HORIZONTAL_FACING));
  if(!l.setBlock(p,next,3))return false;
  l.playSound(null,p,SoundEvents.FIRE_EXTINGUISH,SoundSource.BLOCKS,.4f,1.1f);forget(l,p);return true;
 }
 public static void tick(TickEvent.LevelTickEvent.Post e){
  if(!Rules.on("torch_lifetime")||!(e.level instanceof ServerLevel l)||l.getGameTime()%20!=0)return;var data=TorchLifeData.get(l);int budget=Math.min(128,data.deadline.size());long now=l.getGameTime();
  while(budget-->0){
   var iter=data.deadline.entrySet().iterator();if(!iter.hasNext())break;var row=iter.next();long k=row.getKey(),until=row.getValue();iter.remove();
   BlockPos p=BlockPos.of(k);if(!l.hasChunkAt(p)){data.deadline.put(k,until);continue;}var s=l.getBlockState(p);
   if(!lit(s)){data.setDirty();continue;}
   if(until<=now){if(extinguish(l,p,s)){data.setDirty();continue;}}
   else if(until-now<=1200&&now%100==0)l.sendParticles(ParticleTypes.SMOKE,p.getX()+.5,p.getY()+.65,p.getZ()+.5,1,.03,.04,.03,.01);
   data.deadline.put(k,until);
  }
 }
}
