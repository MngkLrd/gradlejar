package dev.minemods.aquaculture;
import dev.minemods.worldlogic.api.WorldLogicApi.WaterKind;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.*;

/** One record per real fish or clutch. Unloading does not release capacity. */
public final class PopulationData extends SavedData {
 public record Area(int x,int z,WaterKind kind){}
 private record Member(Area area,String species,int amount,boolean clutch){}
 private record Origin(Area area,String species){}
 private final Map<UUID,Member> members=new HashMap<>();
 private final Map<Area,Map<String,Integer>> totals=new HashMap<>();
 private final Set<Origin> seeded=new HashSet<>();
 private static final SavedData.Factory<PopulationData> FACTORY=new SavedData.Factory<>(PopulationData::new,PopulationData::load,null);
 public static PopulationData get(ServerLevel l){return l.getDataStorage().computeIfAbsent(FACTORY,"worldlogic_aqua_population");}
 public static Area area(BlockPos p,WaterKind k){return new Area(Math.floorDiv(p.getX(),64),Math.floorDiv(p.getZ(),64),k);}
 public static boolean valid(WaterKind k){return k!=WaterKind.NONE&&k!=WaterKind.UNKNOWN&&k!=WaterKind.GROUNDWATER;}
 public int total(Area a,String id){return totals.getOrDefault(a,Map.of()).getOrDefault(id,0);}
 public int total(Area a){return totals.getOrDefault(a,Map.of()).values().stream().mapToInt(Integer::intValue).sum();}
 public int room(Area a,Species s){return Math.max(0,Math.min(SpawnSelection.LOCAL_CAP-total(a),SpawnSelection.speciesCap(s)-total(a,s.id())));}
 public boolean seeded(Area a,Species s){return seeded.contains(new Origin(a,s.id()));}
 public void markSeeded(Area a,Species s){if(seeded.add(new Origin(a,s.id())))setDirty();}
 private void delta(Member m,int change){
  var map=totals.computeIfAbsent(m.area,k->new HashMap<>());
  int n=map.getOrDefault(m.species,0)+change*m.amount;
  if(n<=0)map.remove(m.species);else map.put(m.species,n);
  if(map.isEmpty())totals.remove(m.area);
 }
 private void put(UUID id,Member m){
  Member old=members.get(id);if(m.equals(old))return;
  if(old!=null)delta(old,-1);
  members.put(id,m);delta(m,1);setDirty();
 }
 public void observe(UUID id,Area a,Species s){
  if(!valid(a.kind))return;
  put(id,new Member(a,s.id(),1,false));markSeeded(a,s);
 }
 public boolean reserve(UUID id,Area a,Species s,int amount){
  if(!valid(a.kind)||amount<1||amount>room(a,s)||members.containsKey(id))return false;
  put(id,new Member(a,s.id(),amount,true));return true;
 }
 public boolean restoreClutch(UUID id,Area a,Species s,int eggs){
  if(eggs<=0)return false;
  Member m=members.get(id);
  if(m!=null)return m.clutch&&m.area.equals(a)&&m.species.equals(s.id())&&m.amount==eggs;
  return reserve(id,a,s,eggs);
 }
 public boolean takeEgg(UUID id){
  Member m=members.get(id);
  if(m==null||!m.clutch||m.amount<1)return false;
  if(m.amount==1)remove(id);else put(id,new Member(m.area,m.species,m.amount-1,true));
  return true;
 }
 public void returnEgg(UUID id,Area a,Species s){
  Member m=members.get(id);
  if(m==null)put(id,new Member(a,s.id(),1,true));
  else if(m.clutch&&m.area.equals(a)&&m.species.equals(s.id()))put(id,new Member(a,s.id(),m.amount+1,true));
 }
 public void remove(UUID id){Member m=members.remove(id);if(m!=null){delta(m,-1);setDirty();}}
 @Override public CompoundTag save(CompoundTag root,HolderLookup.Provider lookup){
  ListTag origins=new ListTag();for(Origin o:seeded){CompoundTag n=write(o.area,o.species);origins.add(n);}
  ListTag entries=new ListTag();members.forEach((id,m)->{CompoundTag n=write(m.area,m.species);n.putUUID("uuid",id);n.putInt("amount",m.amount);n.putBoolean("clutch",m.clutch);entries.add(n);});
  root.put("seeded",origins);root.put("members",entries);return root;
 }
 private static CompoundTag write(Area a,String species){
  CompoundTag n=new CompoundTag();n.putInt("x",a.x);n.putInt("z",a.z);n.putString("kind",a.kind.name());n.putString("species",species);return n;
 }
 private static Area readArea(CompoundTag n){return new Area(n.getInt("x"),n.getInt("z"),WaterKind.valueOf(n.getString("kind")));}
 static PopulationData load(CompoundTag root,HolderLookup.Provider lookup){
  PopulationData d=new PopulationData();ListTag origins=root.getList("seeded",Tag.TAG_COMPOUND);
  for(int i=0;i<origins.size();i++){CompoundTag n=origins.getCompound(i);try{
   Area a=readArea(n);Species s=Species.BY_ID.get(n.getString("species"));if(s!=null&&valid(a.kind))d.seeded.add(new Origin(a,s.id()));
  }catch(IllegalArgumentException ignored){}}
  ListTag entries=root.getList("members",Tag.TAG_COMPOUND);
  for(int i=0;i<entries.size();i++){CompoundTag n=entries.getCompound(i);try{
   Area a=readArea(n);Species s=Species.BY_ID.get(n.getString("species"));boolean clutch=n.getBoolean("clutch");
   int count=Math.min(s==null?0:s.hatchMax(),n.getInt("amount"));
   if(s==null||!valid(a.kind)||!n.hasUUID("uuid")||count<=0)continue;
   UUID id=n.getUUID("uuid");if(d.members.containsKey(id))continue;
   Member m=new Member(a,s.id(),clutch?count:1,clutch);d.members.put(id,m);d.delta(m,1);
  }catch(IllegalArgumentException ignored){}}
  return d;
 }
}
