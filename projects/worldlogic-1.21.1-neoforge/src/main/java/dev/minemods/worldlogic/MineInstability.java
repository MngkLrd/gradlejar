package dev.minemods.worldlogic;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.event.tick.*;
import net.neoforged.neoforge.event.level.BlockEvent;
public final class MineInstability {
 private record Risk(BlockPos pos,BlockPos trigger,Block block,long due){}
 private static final Map<ServerLevel,ArrayDeque<Risk>> QUEUE=new WeakHashMap<>();
 static boolean rock(net.minecraft.world.level.block.state.BlockState s){return s.is(Blocks.STONE)||s.is(Blocks.DEEPSLATE)||s.is(Blocks.GRAVEL)||s.is(Blocks.ANDESITE)||s.is(Blocks.DIORITE)||s.is(Blocks.GRANITE);}
 private static boolean supported(ServerLevel l,BlockPos roof){
  for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++){BlockPos q=roof.offset(x,-1,z);if(!l.hasChunkAt(q))return true;var s=l.getBlockState(q);
   if(!s.is(BlockTags.LOGS)&&!s.is(BlockTags.FENCES))continue;
   for(int y=1;y<=4;y++){BlockPos base=q.below(y);var b=l.getBlockState(base);if(b.is(BlockTags.LOGS)||b.is(BlockTags.FENCES))continue;if(b.isFaceSturdy(l,base,Direction.UP))return true;break;}
  }return false;
 }
 public static void placed(BlockEvent.EntityPlaceEvent e){if(e.getLevel() instanceof ServerLevel l&&rock(e.getPlacedBlock()))WorldState.markPlaced(l,e.getPos());}
 public static void mined(BlockEvent.BreakEvent e){
  if(e.isCanceled()||!Rules.on("mine_instability")||e.getPlayer().isCreative()||!(e.getLevel() instanceof ServerLevel l)||!rock(e.getState())||e.getPos().getY()>48)return;
  if(WorldState.get(l).treeLimit||WorldState.placed(l,e.getPos()))return;
  var q=QUEUE.computeIfAbsent(l,k->new ArrayDeque<>());if(q.size()>=128)return;
  BlockPos center=e.getPos().above();int n=0;
  for(BlockPos p:BlockPos.betweenClosed(center.offset(-1,0,-1),center.offset(1,2,1))){
   if(n>=2||!l.hasChunkAt(p)||WorldState.placed(l,p)||l.canSeeSky(p)||!rock(l.getBlockState(p))||supported(l,p))continue;
   if(!p.below().equals(e.getPos())&&!l.isEmptyBlock(p.below()))continue;
   int sides=0;for(Direction d:Direction.Plane.HORIZONTAL)if(l.isEmptyBlock(p.relative(d).below()))sides++;
   if(sides<2||l.random.nextFloat()>=Rules.chance("impact",.18))continue;
   BlockPos target=p.immutable();if(q.stream().anyMatch(r->r.pos.equals(target)))continue;
   q.add(new Risk(target,e.getPos().immutable(),l.getBlockState(target).getBlock(),l.getGameTime()+40));n++;
   l.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK,l.getBlockState(target)),target.getX()+.5,target.getY()-.05,target.getZ()+.5,18,.3,.08,.3,.025);
   l.playSound(null,target,SoundEvents.STONE_BREAK,SoundSource.BLOCKS,.7f,.55f);
  }
  if(n>0)e.getPlayer().displayClientMessage(Component.literal("The roof is cracking! Place a supported timber post or retreat."),true);
 }
 public static void tick(LevelTickEvent.Post e){
  if(!(e.getLevel() instanceof ServerLevel l))return;var q=QUEUE.get(l);if(q==null)return;int n=6;
  while(n-->0&&!q.isEmpty()&&q.peek().due<=l.getGameTime()){
   Risk r=q.remove();if(!Rules.on("mine_instability")||!l.hasChunkAt(r.pos)||!l.hasChunkAt(r.trigger)||!l.isEmptyBlock(r.trigger)||!l.getBlockState(r.pos).is(r.block)||WorldState.placed(l,r.pos)||supported(l,r.pos)||!l.isEmptyBlock(r.pos.below()))continue;
   var s=l.getBlockState(r.pos);if(!s.is(Blocks.GRAVEL)){var rubble=s.is(Blocks.DEEPSLATE)?Blocks.COBBLED_DEEPSLATE:Blocks.COBBLESTONE;l.setBlock(r.pos,rubble.defaultBlockState(),3);}
   FallingBlockEntity.fall(l,r.pos,l.getBlockState(r.pos));
  }
 }
}
