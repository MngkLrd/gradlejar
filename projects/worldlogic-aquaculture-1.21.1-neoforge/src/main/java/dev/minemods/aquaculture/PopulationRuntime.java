package dev.minemods.aquaculture;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import java.util.*;

/** Bounded round-robin of loaded chunks; never creates tickets or depends on players. */
public final class PopulationRuntime {
 private static final Map<ServerLevel,LinkedHashSet<Long>> ACTIVE=new WeakHashMap<>();
 private PopulationRuntime(){}
 public static synchronized void load(ChunkEvent.Load e){
  if(e.getLevel() instanceof ServerLevel l&&e.getChunk() instanceof LevelChunk&&l.dimension().equals(Level.OVERWORLD))
   ACTIVE.computeIfAbsent(l,k->new LinkedHashSet<>()).add(e.getChunk().getPos().toLong());
 }
 public static synchronized void unload(ChunkEvent.Unload e){
  if(e.getLevel() instanceof ServerLevel l){var q=ACTIVE.get(l);if(q!=null)q.remove(e.getChunk().getPos().toLong());}
 }
 private static synchronized List<Long> batch(ServerLevel l){
  var q=ACTIVE.get(l);if(q==null||q.isEmpty())return List.of();
  List<Long> batch=new ArrayList<>();int n=Math.min(12,q.size());
  for(int i=0;i<n;i++){long k=q.getFirst();q.remove(k);q.add(k);batch.add(k);}return batch;
 }
 public static void tick(ServerTickEvent.Post e){
  if(e.getServer().getTickCount()%20!=0)return;
  for(ServerLevel l:e.getServer().getAllLevels())for(long k:batch(l)){
   ChunkPos c=new ChunkPos(k);
   if(l.isPositionEntityTicking(new BlockPos(c.getMiddleBlockX(),l.getSeaLevel(),c.getMiddleBlockZ())))seed(l,c);
  }
 }
 static void seed(ServerLevel l,ChunkPos c){
  var random=l.random;
  BlockPos fallback=null;
  // Three bounded surface probes in the already entity-ticking chunk; prefer banks before open water.
  for(int attempt=0;attempt<3;attempt++){
   int x=c.getMinBlockX()+random.nextInt(16),z=c.getMinBlockZ()+random.nextInt(16);
   if(!l.hasChunkAt(new BlockPos(x,l.getSeaLevel(),z)))continue;
   int surface=l.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,x,z)-1;
   BlockPos pos=null;
   for(int i=0;i<12;i++){
    BlockPos p=new BlockPos(x,surface-i,z);if(Aquaculture.water(l,p)){pos=p;break;}
   }
   if(pos==null)continue;
   if(fallback==null)fallback=pos;
   for(int d=0;d<2;d++){
    BlockPos p=pos.below(d);
    if(CoastalHabitat.nearBank(l,p)&&seedPair(l,p,true))return;
   }
  }
  if(fallback==null)return;
  MarineRuntime.trySeed(l,fallback.below(4+l.random.nextInt(5)));
  for(int d=0;d<6;d++){
   BlockPos p=fallback.below(d);if(!Aquaculture.water(l,p))break;
   if(seedPair(l,p,false))return;
  }
 }
 static boolean seedPair(ServerLevel l,BlockPos p,boolean coastal){
  var h=Aquaculture.habitat(l,p);if(h==null||!PopulationData.valid(h.kind()))return false;
  var data=PopulationData.get(l);var area=PopulationData.area(p,h.kind());
  if(data.total(area)>=SpawnSelection.LOCAL_CAP-1)return false;
  Map<String,Integer> excluded=new HashMap<>();
  for(Species s:Species.ALL){
   boolean allowed=coastal?CoastalHabitat.eligible(s,h)&&CoastalHabitat.canFound(data,area,s):!data.seeded(area,s)&&data.room(area,s)>=2;
   if(!allowed)excluded.put(s.id(),SpawnSelection.speciesCap(s));
  }
  Species s=SpawnSelection.choose(h,excluded,l.random);if(s==null)return false;
  boolean alreadySeeded=data.seeded(area,s);
  Fish first=Aquaculture.spawn(l,p,s,h);if(first==null)return false;
  founder(first,s,true,l.getGameTime());
  for(int dx=-2;dx<=2;dx++)for(int dz=-2;dz<=2;dz++){
   if(dx==0&&dz==0)continue;
   BlockPos p2=p.offset(dx,0,dz);
   if(!l.isPositionEntityTicking(p2)||(coastal&&!CoastalHabitat.nearBank(l,p2)))continue;
   var h2=Aquaculture.habitat(l,p2);
   if(h2==null||!PopulationData.area(p2,h2.kind()).equals(area)||(coastal&&!CoastalHabitat.eligible(s,h2)))continue;
   Fish second=Aquaculture.spawn(l,p2,s,h2);
   if(second!=null){founder(second,s,false,l.getGameTime());data.markSeeded(area,s);if(coastal)data.markShoreSeeded(area,s);return true;}
  }
  first.discard();if(!alreadySeeded)data.rollbackFailedFounders(area,s);return false;
 }
 private static void founder(Fish f,Species s,boolean female,long now){
  var t=Aquaculture.traits(f,s);
  t.putBoolean("wl_fish_female",female);
  t.putInt("wl_fish_age_years",Math.max(s.maturityYears(),t.getInt("wl_fish_age_years")));
  FishFeeding.feedNatural(f,s,now);
 }
}
