package dev.minemods.aquaculture;

import dev.minemods.worldlogic.api.WorldLogicApi.WaterKind;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.*;

/** Persistent 64x64 water-kind cells. Forage and harvest pressure, not a census of fish. */
public final class AquaEcologyData extends SavedData {
 public static final int CELL_SIZE=64, MAX_CELLS=8192, FOOD_MAX=12;
 public static final long FORAGE_REGEN=1200L, PRESSURE_REGEN=12000L;
 public enum Food { PLANTS, PLANKTON, BENTHOS }
 private record Key(int x,int z,WaterKind kind){}
 private record Debt(int amount,long at){}
 private static final class Cell {
  final EnumMap<Food,Debt> forage=new EnumMap<>(Food.class);
  final Map<String,Debt> harvest=new HashMap<>();
 }
 private final Map<Key,Cell> cells=new HashMap<>();
 private static final SavedData.Factory<AquaEcologyData> FACTORY=new SavedData.Factory<>(AquaEcologyData::new,AquaEcologyData::load,null);
 public static AquaEcologyData get(ServerLevel level){
  return level.getDataStorage().computeIfAbsent(FACTORY,"worldlogic_aqua_ecology");
 }
 private static Key key(BlockPos p,WaterKind kind){return new Key(Math.floorDiv(p.getX(),CELL_SIZE),Math.floorDiv(p.getZ(),CELL_SIZE),kind);}
 private static boolean aquatic(WaterKind kind){return kind!=WaterKind.NONE&&kind!=WaterKind.UNKNOWN&&kind!=WaterKind.GROUNDWATER;}
 private static int remaining(Debt d,long now,long interval){
  return d==null?0:(int)Math.max(0,d.amount-Math.max(0L,now-d.at)/interval);
 }
 private Cell create(Key k,long now){
  if(!aquatic(k.kind))return null;
  Cell existing=cells.get(k);if(existing!=null)return existing;
  if(cells.size()>=MAX_CELLS){
   cells.entrySet().removeIf(e->e.getValue().forage.values().stream().allMatch(d->remaining(d,now,FORAGE_REGEN)==0)
    &&e.getValue().harvest.values().stream().allMatch(d->remaining(d,now,PRESSURE_REGEN)==0));
  }
  if(cells.size()>=MAX_CELLS)return null;
  Cell c=new Cell();cells.put(k,c);setDirty();return c;
 }
 public int foodAvailable(BlockPos p,WaterKind kind,Food food,long now){
  if(!aquatic(kind))return 0;
  Cell c=cells.get(key(p,kind));
  return FOOD_MAX-remaining(c==null?null:c.forage.get(food),now,FORAGE_REGEN);
 }
 public boolean takeFood(BlockPos p,WaterKind kind,Food food,long now){
  if(foodAvailable(p,kind,food,now)<=0)return false;
  Cell c=create(key(p,kind),now);if(c==null)return false;
  c.forage.put(food,new Debt(remaining(c.forage.get(food),now,FORAGE_REGEN)+1,now));setDirty();return true;
 }
 public int pressure(BlockPos p,WaterKind kind,String id,long now){
  Cell c=cells.get(key(p,kind));return c==null?0:remaining(c.harvest.get(id),now,PRESSURE_REGEN);
 }
 public Map<String,Integer> pressures(BlockPos p,WaterKind kind,long now){
  Cell c=cells.get(key(p,kind));if(c==null)return Map.of();
  Map<String,Integer> result=new HashMap<>();
  c.harvest.forEach((id,d)->{int n=remaining(d,now,PRESSURE_REGEN);if(n>0)result.put(id,n);});return result;
 }
 public void harvest(BlockPos p,WaterKind kind,Species species,long now){
  Cell c=create(key(p,kind),now);if(c==null)return;
  String id=species.id();int n=remaining(c.harvest.get(id),now,PRESSURE_REGEN);
  c.harvest.put(id,new Debt(Math.min(SpawnSelection.speciesCap(species),n+1),now));setDirty();
 }
 @Override public CompoundTag save(CompoundTag root,HolderLookup.Provider registries){
  ListTag list=new ListTag();
  for(var entry:cells.entrySet()){
   Key k=entry.getKey();Cell c=entry.getValue();CompoundTag row=new CompoundTag();
   row.putInt("x",k.x);row.putInt("z",k.z);row.putString("kind",k.kind.name());
   ListTag food=new ListTag();c.forage.forEach((id,d)->food.add(write(id.name(),d)));row.put("food",food);
   ListTag harvest=new ListTag();c.harvest.forEach((id,d)->harvest.add(write(id,d)));row.put("harvest",harvest);
   list.add(row);
  }
  root.put("cells",list);return root;
 }
 private static CompoundTag write(String id,Debt d){
  CompoundTag n=new CompoundTag();n.putString("id",id);n.putInt("amount",d.amount);n.putLong("at",d.at);return n;
 }
 static AquaEcologyData load(CompoundTag root,HolderLookup.Provider registries){
  AquaEcologyData data=new AquaEcologyData();ListTag rows=root.getList("cells",Tag.TAG_COMPOUND);
  for(int i=0;i<rows.size()&&data.cells.size()<MAX_CELLS;i++){
   CompoundTag row=rows.getCompound(i);WaterKind kind;
   try{kind=WaterKind.valueOf(row.getString("kind"));}catch(IllegalArgumentException ex){continue;}
   if(!aquatic(kind))continue;
   Cell c=new Cell();
   ListTag food=row.getList("food",Tag.TAG_COMPOUND);
   for(int a=0;a<Math.min(food.size(),Food.values().length);a++){
    CompoundTag n=food.getCompound(a);
    try{c.forage.put(Food.valueOf(n.getString("id")),read(n,FOOD_MAX));}catch(IllegalArgumentException ignored){}
   }
   ListTag harvest=row.getList("harvest",Tag.TAG_COMPOUND);
   for(int a=0;a<Math.min(harvest.size(),Species.ALL.size());a++){
    CompoundTag n=harvest.getCompound(a);Species s=Species.BY_ID.get(n.getString("id"));
    if(s!=null)c.harvest.put(s.id(),read(n,SpawnSelection.speciesCap(s)));
   }
   data.cells.put(new Key(row.getInt("x"),row.getInt("z"),kind),c);
  }
  return data;
 }
 private static Debt read(CompoundTag n,int cap){return new Debt(Math.max(0,Math.min(cap,n.getInt("amount"))),Math.max(0L,n.getLong("at")));}
}
