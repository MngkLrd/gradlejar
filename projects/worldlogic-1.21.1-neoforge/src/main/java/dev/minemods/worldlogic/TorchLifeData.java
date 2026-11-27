package dev.minemods.worldlogic;
import java.util.*;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
public final class TorchLifeData extends SavedData {
 final LinkedHashMap<Long,Long> deadline=new LinkedHashMap<>();
 static TorchLifeData get(ServerLevel l){return l.getDataStorage().computeIfAbsent(new SavedData.Factory<>(TorchLifeData::new,TorchLifeData::load,null),"worldlogic_torches");}
 static TorchLifeData load(CompoundTag n,HolderLookup.Provider lookup){
  TorchLifeData d=new TorchLifeData();long[] p=n.getLongArray("positions"),t=n.getLongArray("deadlines");for(int i=0;i<Math.min(32768,Math.min(p.length,t.length));i++)d.deadline.put(p[i],t[i]);return d;
 }
 @Override public CompoundTag save(CompoundTag n,HolderLookup.Provider lookup){
  long[] p=new long[deadline.size()],t=new long[p.length];int i=0;for(var e:deadline.entrySet()){p[i]=e.getKey();t[i++]=e.getValue();}n.putLongArray("positions",p);n.putLongArray("deadlines",t);return n;
 }
}
