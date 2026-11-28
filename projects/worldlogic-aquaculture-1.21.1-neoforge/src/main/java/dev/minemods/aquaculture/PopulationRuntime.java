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
  int x=c.getMinBlockX()+random.nextInt(16),z=c.getMinBlockZ()+random.nextInt(16);
  if(!l.hasChunkAt(new BlockPos(x,l.getSeaLevel(),z)))return;
  int surface=l.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,x,z)-1;
  BlockPos pos=null;
  for(int i=0;i<12;i++){
   BlockPos p=new BlockPos(x,surface-i,z);if(Aquaculture.water(l,p)){pos=p;break;}
  }
  if(pos==null)return;
  var data=PopulationData.get(l);
  for(int d=0;d<6;d++){
   BlockPos p=pos.below(d);var h=Aquaculture.habitat(l,p);if(h==null)break;
   var area=PopulationData.area(p,h.kind());if(!PopulationData.valid(h.kind())||data.total(area)>=SpawnSelection.LOCAL_CAP-1)continue;
   Map<String,Integer> excluded=new HashMap<>();
   for(Species s:Species.ALL)if(data.seeded(area,s)||data.room(area,s)<2)excluded.put(s.id(),SpawnSelection.speciesCap(s));
   Species s=SpawnSelection.choose(h,excluded,random);if(s==null)continue;
   Fish first=Aquaculture.spawn(l,p,s,h);if(first==null)continue;
   founder(first,s,true,l.getGameTime());
   boolean paired=false;
   for(int dx=-2;dx<=2&&!paired;dx++)for(int dz=-2;dz<=2&&!paired;dz++){
    if(dx==0&&dz==0)continue;
    BlockPos p2=p.offset(dx,0,dz);var h2=Aquaculture.habitat(l,p2);
    if(h2==null||!PopulationData.area(p2,h2.kind()).equals(area)||!l.isPositionEntityTicking(p2))continue;
    Fish second=Aquaculture.spawn(l,p2,s,h2);
    if(second!=null){founder(second,s,false,l.getGameTime());paired=true;}
   }
   if(!paired){first.discard();data.rollbackFailedFounders(area,s);continue;}
   data.markSeeded(area,s);return;
  }
 }
 private static void founder(Fish f,Species s,boolean female,long now){
  var t=Aquaculture.traits(f,s);
  t.putBoolean("wl_fish_female",female);
  t.putInt("wl_fish_age_years",Math.max(s.maturityYears(),t.getInt("wl_fish_age_years")));
  FishFeeding.feedNatural(f,s,now);
 }
}
