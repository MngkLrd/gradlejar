package dev.minemods.worldlogic;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ExplosionEvent;
public final class StructuralHazards {
 private record Job(BlockPos pos,BlockPos support,Block expected,long due,boolean blast){}
 private record Heat(Block block,long first,long last){}
 private static final Map<ServerLevel,ArrayDeque<Job>> JOBS=new WeakHashMap<>();
 private static final Map<ServerLevel,Map<BlockPos,Heat>> HEAT=new WeakHashMap<>();
 private static final Map<ServerLevel,Map<BlockPos,Heat>> BURN=new WeakHashMap<>();
 private static final Map<ServerLevel,Integer> CURSOR=new WeakHashMap<>();
 static boolean loaded(ServerLevel l,BlockPos p){return l.hasChunkAt(p)&&l.isInWorldBounds(p);}
 static boolean free(ServerLevel l,BlockPos p){return loaded(l,p)&&l.isEmptyBlock(p);}
 static boolean grief(ServerLevel l){return l.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING);}
 static boolean vulnerable(BlockState s){return Material.block(s,"structural_weak");}
 static int minBurn(double strength){return (int)Math.max(80,Math.round(160/strength));}
 static boolean supported(ServerLevel l,BlockPos p){
  if(!free(l,p.below()))return true;
  for(Direction d:Direction.Plane.HORIZONTAL){BlockPos q=p.relative(d);if(!loaded(l,q)||!loaded(l,q.below()))return true;
   var s=l.getBlockState(q);
   if(s.is(BlockTags.LOGS)||s.is(BlockTags.FENCES)||s.is(BlockTags.PLANKS))if(l.getBlockState(q.below()).isFaceSturdy(l,q.below(),Direction.UP))return true;
  }
  return false;
 }
 static void warn(ServerLevel l,BlockPos p,BlockState s){
  l.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK,s),p.getX()+.5,p.getY()-.03,p.getZ()+.5,9,.22,.05,.22,.025);
  l.playSound(null,p,SoundEvents.STONE_BREAK,SoundSource.BLOCKS,.45f,.65f);
 }
 static void enqueue(ServerLevel l,BlockPos p,BlockPos support,boolean blast){
  if(!loaded(l,p)||!loaded(l,support))return;var s=l.getBlockState(p);
  if(!vulnerable(s)||s.hasBlockEntity()||WorldState.get(l).treeLimit||WorldState.placed(l,p))return;
  var q=JOBS.computeIfAbsent(l,k->new ArrayDeque<>());if(q.size()>=128||q.stream().anyMatch(j->j.pos.equals(p)))return;
  q.add(new Job(p.immutable(),support.immutable(),s.getBlock(),l.getGameTime()+30,blast));warn(l,p,s);
 }
 public static void place(BlockEvent.EntityPlaceEvent e){
  if(!e.isCanceled()&&e.getLevel() instanceof ServerLevel l&&vulnerable(e.getPlacedBlock()))WorldState.markPlaced(l,e.getPos());
 }
 public static void broken(BlockEvent.BreakEvent e){
  if(e.isCanceled()||!Rules.on("structural_stress")||e.getPlayer().isCreative()||!(e.getLevel() instanceof ServerLevel l))return;
  enqueue(l,e.getPos().above(),e.getPos(),false);
 }
 public static void blast(ExplosionEvent.Detonate e){
  if(!(e.getLevel() instanceof ServerLevel l))return;
  if(Rules.on("blast_chain_reaction")&&grief(l)){
   var affected=new HashSet<>(e.getAffectedBlocks());int n=0;
   for(BlockPos p:e.getAffectedBlocks()){if(n++>=48)break;BlockPos above=p.above();if(!affected.contains(above))enqueue(l,above,p,true);}
  }
  if(Rules.on("animal_stampede")||Rules.on("herd_panic")){int n=0;for(var x:e.getAffectedEntities()){if(n++>=40)break;if(x instanceof net.minecraft.world.entity.animal.Animal a)Ecology.panic(a,l.getGameTime()+140);}}
 }
 static FallingBlockEntity fall(ServerLevel l,BlockPos p,BlockState s,boolean debris){
  if(s.hasBlockEntity())return null;
  FallingBlockEntity f=FallingBlockEntity.fall(l,p,s);
  if(debris)f.getPersistentData().putBoolean("worldlogic_debris",true);return f;
 }
 private static boolean hot(BlockState s){return s.is(Blocks.FIRE)||s.is(Blocks.SOUL_FIRE)||s.is(Blocks.LAVA);}
 private static boolean adjacentFire(ServerLevel l,BlockPos p){for(Direction d:Direction.values()){BlockPos q=p.relative(d);if(loaded(l,q)&&hot(l.getBlockState(q)))return true;}return false;}
 static BlockState quenched(BlockState s){
  if(s.is(Blocks.STONE_BRICKS))return Blocks.CRACKED_STONE_BRICKS.defaultBlockState();
  if(s.is(Blocks.DEEPSLATE_BRICKS))return Blocks.CRACKED_DEEPSLATE_BRICKS.defaultBlockState();
  if(s.is(Blocks.STONE))return Blocks.COBBLESTONE.defaultBlockState();
  return s;
 }
 private static void sample(ServerLevel l,BlockPos p,long now){
  if(!loaded(l,p))return;var s=l.getBlockState(p);if(s.hasBlockEntity())return;
  if(Rules.on("thermal_expansion")&&Material.block(s,"thermal_mass")){
   var map=HEAT.computeIfAbsent(l,k->new HashMap<>());Heat old=map.get(p);
   if(old!=null&&old.block!=s.getBlock()){map.remove(p);old=null;}
   boolean wet=false;for(Direction d:Direction.values()){BlockPos q=p.relative(d);if(loaded(l,q)&&l.getFluidState(q).is(FluidTags.WATER)){wet=true;break;}}
   if(old!=null&&wet&&old.last-old.first>=60&&now-old.last<=240){map.remove(p);BlockState next=quenched(s);
    if(next!=s&&grief(l)){l.setBlock(p,next,3);l.sendParticles(ParticleTypes.CLOUD,p.getX()+.5,p.getY()+.8,p.getZ()+.5,8,.25,.2,.25,.02);}
   }else if(adjacentFire(l,p)&&(map.size()<512||old!=null)){map.put(p.immutable(),new Heat(s.getBlock(),old==null||now-old.last>30?now:old.first,now));}
  }
  if(!Rules.on("burning_structures")||!grief(l)||!l.getGameRules().getBoolean(GameRules.RULE_DOFIRETICK)||!Material.block(s,"burning_structure"))return;
  var map=BURN.computeIfAbsent(l,k->new HashMap<>());Heat old=map.get(p);
  if(!adjacentFire(l,p)){map.remove(p);return;}
  if(old==null||old.block!=s.getBlock()||now-old.last>100){if(map.size()<512||old!=null)map.put(p.immutable(),new Heat(s.getBlock(),now,now));return;}
  if(now-old.first>=minBurn(Rules.strength("fire"))&&!supported(l,p)){
   map.remove(p);warn(l,p,s);fall(l,p,s,true);
  }else map.put(p.immutable(),new Heat(s.getBlock(),old.first,now));
 }
 /** Production-server fixture; called only with worldlogic.ci=true in a disposable test world. */
 static boolean verifyWorld(ServerLevel l){
  BlockPos p=l.getSharedSpawnPos().atY(l.getMaxBuildHeight()-12);
  List<BlockPos> cells=new ArrayList<>();cells.add(p);cells.add(p.below());
  for(Direction d:Direction.Plane.HORIZONTAL){cells.add(p.relative(d));cells.add(p.relative(d).below());}
  for(BlockPos q:cells)if(!loaded(l,q)||l.getBlockState(q).hasBlockEntity())return false;
  Map<BlockPos,BlockState> backup=new LinkedHashMap<>();for(BlockPos q:cells)backup.put(q,l.getBlockState(q));
  FallingBlockEntity debris=null;
  try{
   for(BlockPos q:cells)l.setBlock(q,Blocks.AIR.defaultBlockState(),3);
   l.setBlock(p,Blocks.CLAY.defaultBlockState(),3);
   if(supported(l,p))return false;
   BlockPos post=p.east();l.setBlock(post.below(),Blocks.STONE.defaultBlockState(),3);l.setBlock(post,Blocks.OAK_LOG.defaultBlockState(),3);
   if(!supported(l,p))return false;
   l.setBlock(post,Blocks.AIR.defaultBlockState(),3);
   if(supported(l,p))return false;
   debris=fall(l,p,l.getBlockState(p),true);
   return debris!=null&&l.isEmptyBlock(p)&&debris.getBlockState().is(Blocks.CLAY)&&debris.getPersistentData().getBoolean("worldlogic_debris");
  }finally{
   if(debris!=null)debris.discard();
   backup.forEach((q,s)->l.setBlock(q,s,3));
  }
 }
 public static void tick(TickEvent.LevelTickEvent.Post e){
  if(!(e.level instanceof ServerLevel l))return;long now=l.getGameTime();var q=JOBS.get(l);
  if(q!=null){int budget=6;while(budget-->0&&!q.isEmpty()&&q.peek().due<=now){Job j=q.remove();
   if(!(j.blast?Rules.on("blast_chain_reaction"):Rules.on("structural_stress"))||!grief(l)||!loaded(l,j.pos)||!free(l,j.support)||supported(l,j.pos)||WorldState.placed(l,j.pos))continue;
   var s=l.getBlockState(j.pos);if(s.is(j.expected)&&vulnerable(s))fall(l,j.pos,s,true);
  }}
  if(now%10!=0||(!Rules.on("burning_structures")&&!Rules.on("thermal_expansion")))return;
  // Revisit active heat locations so exposure is measured, not a random sampling count.
  for(var map:List.of(HEAT.computeIfAbsent(l,k->new HashMap<>()),BURN.computeIfAbsent(l,k->new HashMap<>()))){int n=0;for(var x:new ArrayList<>(map.entrySet())){if(now-x.getValue().last>300){map.remove(x.getKey());continue;}if(n++<64&&loaded(l,x.getKey()))sample(l,x.getKey(),now);}}
  // Deterministic round-robin scan of a small region around players, no full-world scan.
  int start=CURSOR.getOrDefault(l,0);int players=0;for(var p:l.players()){if(players++>=8)break;BlockPos center=p.blockPosition();for(int i=0;i<96;i++){int k=(start+i)%847;sample(l,center.offset(k%11-5,(k/121)%7-3,(k/11)%11-5),now);}}
  CURSOR.put(l,(start+96)%847);
 }
}
