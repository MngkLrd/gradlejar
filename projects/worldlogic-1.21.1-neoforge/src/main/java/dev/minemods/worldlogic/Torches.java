package dev.minemods.worldlogic;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.InteractionResult;
import net.neoforged.neoforge.event.tick.*;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
public final class Torches {
 private static final Map<ServerLevel,Map<BlockPos,Long>> WET=new WeakHashMap<>();
 public static void tick(PlayerTickEvent.Post e){
  if(!Rules.on("wet_torches")||!(e.getEntity().level() instanceof ServerLevel l)||l.getGameTime()%20!=0||!LocalWeatherRuntime.rainingAt(l,e.getEntity().blockPosition()))return;
  long now=l.getGameTime();var wet=WET.computeIfAbsent(l,k->new HashMap<>());wet.entrySet().removeIf(x->now-x.getValue()>1600);if(wet.size()>2048)wet.clear();
  BlockPos center=e.getEntity().blockPosition();for(int i=0;i<192;i++){BlockPos q=center.offset(l.random.nextInt(17)-8,l.random.nextInt(9)-4,l.random.nextInt(17)-8);
   if(!l.hasChunkAt(q)||!LocalWeatherRuntime.rainingAt(l,q))continue;BlockState s=l.getBlockState(q);
   if(!s.is(Blocks.TORCH)&&!s.is(Blocks.WALL_TORCH))continue;long first=wet.computeIfAbsent(q.immutable(),k->now);
   l.sendParticles(ParticleTypes.SMOKE,q.getX()+.5,q.getY()+.65,q.getZ()+.5,2,.05,.07,.05,.01);
   if(now-first<120)continue;
   BlockState next=(s.is(Blocks.TORCH)?Content.TORCH.get():Content.WALL_TORCH.get()).defaultBlockState();
   if(s.hasProperty(BlockStateProperties.HORIZONTAL_FACING))next=next.setValue(BlockStateProperties.HORIZONTAL_FACING,s.getValue(BlockStateProperties.HORIZONTAL_FACING));
   if(l.setBlock(q,next,3)){TorchLifetime.forget(l,q);wet.remove(q);}
  }
 }
 public static void relight(PlayerInteractEvent.RightClickBlock e){
  if(!(e.getLevel() instanceof ServerLevel l)||e.isCanceled()||!e.getItemStack().is(Items.FLINT_AND_STEEL))return;
  var s=l.getBlockState(e.getPos());boolean wall=s.is(Content.WALL_TORCH.get());if(!wall&&!s.is(Content.TORCH.get()))return;
  var next=(wall?Blocks.WALL_TORCH:Blocks.TORCH).defaultBlockState();
  if(wall)next=next.setValue(BlockStateProperties.HORIZONTAL_FACING,s.getValue(BlockStateProperties.HORIZONTAL_FACING));
  if(l.setBlock(e.getPos(),next,3)){TorchLifetime.register(l,e.getPos());if(!e.getEntity().isCreative())e.getItemStack().hurtAndBreak(1,e.getEntity(),e.getHand()==net.minecraft.world.InteractionHand.MAIN_HAND?EquipmentSlot.MAINHAND:EquipmentSlot.OFFHAND);}
  e.setCancellationResult(InteractionResult.SUCCESS);e.setCanceled(true);
 }
}
