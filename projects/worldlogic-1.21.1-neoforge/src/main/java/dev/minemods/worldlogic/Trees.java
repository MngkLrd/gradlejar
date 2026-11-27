package dev.minemods.worldlogic;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.neoforged.neoforge.event.tick.*;
import net.neoforged.neoforge.event.level.BlockEvent;
/** Conservative opt-in tree decay. Never process unloaded chunks; player placements persisted in SavedData. */
public final class Trees {
 record Job(BlockPos pos,BlockPos root,net.minecraft.world.level.block.Block block,long due){}
 private static final Map<ServerLevel,ArrayDeque<Job>> QUEUE=new WeakHashMap<>();
 public static void placed(BlockEvent.EntityPlaceEvent e){if(e.getLevel() instanceof ServerLevel l&&e.getPlacedBlock().is(BlockTags.LOGS)){WorldState.markPlaced(l,e.getPos());}}
 public static void breakBase(BlockEvent.BreakEvent e){
  if(e.isCanceled()||!(e.getLevel() instanceof ServerLevel l))return;BlockPos origin=e.getPos();
  var data=WorldState.get(l);boolean artificial=WorldState.placed(l,origin);if(e.getState().is(BlockTags.LOGS))WorldState.clear(l,origin);
  if(!Rules.on("tree_weight")||artificial||data.treeLimit||e.getPlayer().isCreative()||!e.getState().is(BlockTags.LOGS)||!l.getBlockState(origin.below()).is(BlockTags.DIRT))return;
  Set<BlockPos> logs=new HashSet<>();ArrayDeque<BlockPos> todo=new ArrayDeque<>();todo.add(origin.above());boolean leaves=false;
  while(!todo.isEmpty()){BlockPos q=todo.remove();if(q.equals(origin)||logs.contains(q))continue;if(!l.hasChunkAt(q)||q.getY()<origin.getY()||q.getY()>origin.getY()+12||Math.abs(q.getX()-origin.getX())>4||Math.abs(q.getZ()-origin.getZ())>4)return;
   if(!l.getBlockState(q).is(e.getState().getBlock()))continue;
   if(WorldState.placed(l,q)||logs.size()>=16||l.getBlockState(q.below()).is(BlockTags.DIRT))return;logs.add(q.immutable());
   for(Direction d:Direction.values()){BlockPos t=q.relative(d);if(!l.hasChunkAt(t))return;var st=l.getBlockState(t);
    if(st.getBlock() instanceof LeavesBlock&&!st.getValue(LeavesBlock.PERSISTENT))leaves=true;
    if(!t.equals(origin)&&st.is(e.getState().getBlock())&&!logs.contains(t))todo.add(t);}
  }
  if(!leaves||logs.isEmpty())return;var queue=QUEUE.computeIfAbsent(l,k->new ArrayDeque<>());if(queue.size()+logs.size()>128)return;
  List<BlockPos> ordered=new ArrayList<>(logs);ordered.sort(Comparator.comparingInt((BlockPos pos)->pos.getY()).reversed());long due=l.getGameTime()+10;
  for(BlockPos q:ordered){queue.add(new Job(q,origin.immutable(),e.getState().getBlock(),due));due+=5;}
 }
 public static void tick(LevelTickEvent.Post e){if(!(e.getLevel() instanceof ServerLevel l))return;var queue=QUEUE.get(l);if(queue==null)return;int n=6;
  while(n-->0&&!queue.isEmpty()&&queue.peek().due<=l.getGameTime()){Job job=queue.remove();BlockPos q=job.pos;if(Rules.on("tree_weight")&&l.hasChunkAt(q)&&l.hasChunkAt(job.root)&&l.isEmptyBlock(job.root)&&l.getBlockState(q).is(job.block)&&!WorldState.placed(l,q)){if(Rules.on("dangerous_trees")&&l.getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_MOBGRIEFING)){var falling=net.minecraft.world.entity.item.FallingBlockEntity.fall(l,q,l.getBlockState(q));falling.getPersistentData().putBoolean("worldlogic_falling_log",true);}else l.destroyBlock(q,true);}}
 }
}
