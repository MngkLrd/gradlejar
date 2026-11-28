package dev.minemods.thermal.server;
import dev.minemods.thermal.RoomPhysics;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.LinkedHashMap;
/** Dimension-local persisted state. Geometry is rebuilt and signature-checked before reuse. */
final class RoomClimateData extends SavedData {
    static final int LIMIT=512;
    record Entry(long signature,long tick,RoomPhysics.State state){}
    private final LinkedHashMap<Long,Entry> entries=new LinkedHashMap<>(64,.75f,true);
    static RoomClimateData get(ServerLevel l){
        return l.getDataStorage().computeIfAbsent(new SavedData.Factory<>(RoomClimateData::new,RoomClimateData::load,null),"worldlogic_thermal_rooms");
    }
    Entry get(long key){return entries.get(key);}
    void put(long key,Entry entry){
        entries.put(key,entry);
        while(entries.size()>LIMIT)entries.remove(entries.keySet().iterator().next());
        setDirty();
    }
    void remove(long key){if(entries.remove(key)!=null)setDirty();}
    static RoomClimateData load(CompoundTag tag,HolderLookup.Provider provider){
        var data=new RoomClimateData();
        var rows=tag.getList("rooms",Tag.TAG_COMPOUND);
        for(int i=0;i<rows.size()&&i<LIMIT;i++){
            var row=rows.getCompound(i);
            double a=row.getDouble("air"),m=row.getDouble("mass"),v=row.getDouble("vapor");
            if(!Double.isFinite(a)||!Double.isFinite(m)||!Double.isFinite(v)||Math.abs(a)>115||Math.abs(m)>100||v<0||v>250)continue;
            data.entries.put(row.getLong("key"),new Entry(row.getLong("signature"),row.getLong("tick"),new RoomPhysics.State(a,m,v)));
        }
        return data;
    }
    @Override public CompoundTag save(CompoundTag tag,HolderLookup.Provider provider){
        ListTag rows=new ListTag();
        for(var pair:entries.entrySet()){
            var row=new CompoundTag();var e=pair.getValue();
            row.putLong("key",pair.getKey());row.putLong("signature",e.signature());row.putLong("tick",e.tick());
            row.putDouble("air",e.state().air());row.putDouble("mass",e.state().mass());row.putDouble("vapor",e.state().vapor());rows.add(row);
        }
        tag.putInt("schema",1);tag.put("rooms",rows);return tag;
    }
}
