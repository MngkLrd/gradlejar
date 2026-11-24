package dev.umbral.shadow;

import dev.umbral.Umbral;
import dev.umbral.block.ShadowBlock;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.*;

/** Persisted ownership: a temporary block belongs to one projector, never overwrites ordinary world blocks. */
public final class ShadowData extends SavedData {
    public static final class Source {
        public final BlockPos pos;
        public boolean retired;
        public final Set<BlockPos> cells=new HashSet<>();
        Source(BlockPos p){pos=p.immutable();}
    }
    final Map<BlockPos,Source> sources=new LinkedHashMap<>();
    private final Map<BlockPos,BlockPos> owners=new HashMap<>();
    int cursor;
    public static ShadowData get(ServerLevel l){
        return l.getDataStorage().computeIfAbsent(new SavedData.Factory<>(ShadowData::new,ShadowData::read,null),"umbral_shadow_ownership");
    }
    public void register(BlockPos p){
        Source s=sources.computeIfAbsent(p.immutable(),Source::new);s.retired=false;setDirty();
    }
    public void retire(BlockPos p){Source s=sources.get(p);if(s!=null){s.retired=true;setDirty();}}
    public boolean owns(BlockPos p){return owners.containsKey(p);}
    void reconcile(ServerLevel l,Source s,Map<BlockPos,Integer> desired){
        // Clean old geometry first. Never force a chunk to load; pending cells stay indexed until loaded.
        Iterator<BlockPos> it=s.cells.iterator();
        while(it.hasNext()){
            BlockPos p=it.next();
            if(!l.hasChunkAt(p))continue;
            if(desired.containsKey(p)&&l.getBlockState(p).is(Umbral.SHADOW.get()))continue;
            if(s.pos.equals(owners.get(p))){
                if(l.getBlockState(p).is(Umbral.SHADOW.get()))l.removeBlock(p,false);
                owners.remove(p);
            }
            it.remove();setDirty();
        }
        for(var e:desired.entrySet()){
            BlockPos p=e.getKey();if(!l.hasChunkAt(p))continue;
            var state=l.getBlockState(p);
            BlockPos owner=owners.get(p);
            if(owner!=null&&!owner.equals(s.pos))continue;
            if(!state.isAir()&&!state.is(Umbral.SHADOW.get()))continue;
            if(state.is(Umbral.SHADOW.get())&&owner==null)continue;
            var next=ShadowBlock.fromCode(Umbral.SHADOW.get().defaultBlockState(),e.getValue());
            if(state==next)continue;
            boolean added=s.cells.add(p);owners.put(p,s.pos);
            if(!l.setBlock(p,next,3)&&added){s.cells.remove(p);owners.remove(p);}
            setDirty();
        }
    }
    @Override public CompoundTag save(CompoundTag t,HolderLookup.Provider registries){
        ListTag list=new ListTag();
        for(Source s:sources.values()){
            CompoundTag n=new CompoundTag();n.putLong("pos",s.pos.asLong());n.putBoolean("retired",s.retired);
            n.putLongArray("cells",s.cells.stream().mapToLong(BlockPos::asLong).toArray());list.add(n);
        }
        t.put("sources",list);return t;
    }
    private static ShadowData read(CompoundTag t,HolderLookup.Provider registries){
        ShadowData data=new ShadowData();ListTag list=t.getList("sources",Tag.TAG_COMPOUND);
        for(int i=0;i<list.size();i++){
            CompoundTag n=list.getCompound(i);Source s=new Source(BlockPos.of(n.getLong("pos")));
            s.retired=n.getBoolean("retired");data.sources.put(s.pos,s);
            for(long v:n.getLongArray("cells")){
                BlockPos p=BlockPos.of(v);
                if(data.owners.putIfAbsent(p,s.pos)==null)s.cells.add(p);
            }
        }
        return data;
    }
}
