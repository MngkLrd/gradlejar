package dev.minemods.worldlogic;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
public final class PocketData extends SavedData {
 final List<AirAndFire.Pocket> pockets=new ArrayList<>();
 static PocketData get(ServerLevel l){return l.getDataStorage().computeIfAbsent(new SavedData.Factory<>(PocketData::new,PocketData::load,null),"worldlogic_air");}
 static PocketData load(CompoundTag root,HolderLookup.Provider registries){PocketData d=new PocketData();for(Tag t:root.getList("pockets",Tag.TAG_COMPOUND)){CompoundTag a=(CompoundTag)t;Set<BlockPos> cells=new HashSet<>();for(long n:a.getLongArray("cells"))cells.add(BlockPos.of(n));if(cells.isEmpty()||cells.size()>128||d.pockets.size()>=256)continue;var p=new AirAndFire.Pocket(cells,a.getLong("last"));p.oxygen=Math.max(0,Math.min(cells.size()*240,a.getInt("oxygen")));d.pockets.add(p);}return d;}
 @Override public CompoundTag save(CompoundTag root,HolderLookup.Provider registries){ListTag out=new ListTag();for(var p:pockets){CompoundTag a=new CompoundTag();a.putLongArray("cells",p.cells.stream().mapToLong(BlockPos::asLong).toArray());a.putInt("oxygen",p.oxygen);a.putLong("last",p.last);out.add(a);}root.put("pockets",out);return root;}
}
