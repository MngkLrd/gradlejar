package dev.minemods.worldlogic;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
/** Sparse world state; placement history never silently evicts entries. */
public final class WorldState extends SavedData {
 final Set<Long> placed=new HashSet<>();
 final Map<Long,Integer> snow=new HashMap<>();
 final Map<Long,Long> flood=new HashMap<>();
 boolean treeLimit;
 static WorldState get(ServerLevel l){return l.getDataStorage().computeIfAbsent(new SavedData.Factory<>(WorldState::new,WorldState::load,null),"worldlogic_state");}
 static WorldState load(CompoundTag root,HolderLookup.Provider lookup){
  WorldState d=new WorldState();for(long p:root.getLongArray("placed"))d.placed.add(p);d.treeLimit=root.getBoolean("treeLimit");
  for(Tag t:root.getList("snow",Tag.TAG_COMPOUND)){CompoundTag c=(CompoundTag)t;d.snow.put(c.getLong("p"),Math.max(0,Math.min(8,c.getInt("v"))));}
  for(Tag t:root.getList("flood",Tag.TAG_COMPOUND)){CompoundTag c=(CompoundTag)t;d.flood.put(c.getLong("p"),c.getLong("v"));}
  return d;
 }
 @Override public CompoundTag save(CompoundTag root,HolderLookup.Provider lookup){
  root.putLongArray("placed",placed.stream().mapToLong(Long::longValue).toArray());root.putBoolean("treeLimit",treeLimit);
  ListTag s=new ListTag(),w=new ListTag();
  snow.forEach((p,v)->{CompoundTag c=new CompoundTag();c.putLong("p",p);c.putInt("v",v);s.add(c);});
  flood.forEach((p,v)->{CompoundTag c=new CompoundTag();c.putLong("p",p);c.putLong("v",v);w.add(c);});
  root.put("snow",s);root.put("flood",w);return root;
 }
 static boolean placed(ServerLevel l,BlockPos p){return get(l).placed.contains(p.asLong());}
 static void markPlaced(ServerLevel l,BlockPos p){var d=get(l);if(d.placed.size()>=1000000)d.treeLimit=true;else d.placed.add(p.asLong());d.setDirty();}
 static void clear(ServerLevel l,BlockPos p){var d=get(l);long k=p.asLong();boolean changed=d.placed.remove(k);changed|=d.snow.remove(k)!=null;changed|=d.flood.remove(k)!=null;if(changed)d.setDirty();}
}
