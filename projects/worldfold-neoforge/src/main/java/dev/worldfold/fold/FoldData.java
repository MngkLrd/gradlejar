package dev.worldfold.fold;

import com.mojang.logging.LogUtils;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.*;

public final class FoldData extends SavedData {
    public final Map<UUID,FoldLink> links=new LinkedHashMap<>();
    public final Set<UUID> liveGenerations=new HashSet<>();
    public final Map<Integer,Anchor> pocketReturns=new HashMap<>();
    public int nextPocket=0;
    public static FoldData get(MinecraftServer server){
        return server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(FoldData::new,FoldData::read,null),"worldfold_network_v2");
    }
    public FoldLink find(Anchor anchor){return find(anchor.dimension(),anchor.floor());}
    public FoldLink find(net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dim,BlockPos p){
        for(FoldLink f:links.values())for(Anchor a:f.anchors())if(a.dimension().equals(dim)&&a.floor().equals(p))return f;return null;
    }
    public boolean free(Anchor anchor){
        for(FoldLink f:links.values())for(Anchor a:f.anchors())if(anchor.conflicts(a))return false;return true;
    }
    public void add(FoldLink f){links.put(f.id,f);liveGenerations.add(f.generation);setDirty();}
    public void remove(FoldLink f){links.remove(f.id);liveGenerations.remove(f.generation);setDirty();}
    public void generation(FoldLink f,UUID next){liveGenerations.remove(f.generation);f.generation=next;liveGenerations.add(next);setDirty();}
    public boolean limit(UUID owner){return links.size()>=128||links.values().stream().filter(f->f.owner.equals(owner)).count()>=24;}
    public int allocate(Anchor entry){
        if(nextPocket>=65536)throw new IllegalStateException("Pocket registry full");
        int n=nextPocket++;pocketReturns.put(n,entry);setDirty();return n;
    }
    @Override public CompoundTag save(CompoundTag t,HolderLookup.Provider registries){
        ListTag list=new ListTag();for(FoldLink f:links.values())list.add(f.save(registries));t.put("links",list);
        t.putInt("nextPocket",nextPocket);ListTag pockets=new ListTag();
        for(var e:pocketReturns.entrySet()){CompoundTag p=new CompoundTag();p.putInt("index",e.getKey());p.put("return",e.getValue().save());pockets.add(p);}
        t.put("pockets",pockets);return t;
    }
    private static FoldData read(CompoundTag t,HolderLookup.Provider registries){
        FoldData data=new FoldData();data.nextPocket=Math.max(0,t.getInt("nextPocket"));
        ListTag list=t.getList("links",Tag.TAG_COMPOUND);
        for(int i=0;i<list.size();i++)try{
            FoldLink f=FoldLink.read(list.getCompound(i),registries);data.links.put(f.id,f);data.liveGenerations.add(f.generation);
        }catch(RuntimeException e){LogUtils.getLogger().error("Invalid World Fold link in save",e);}
        ListTag pockets=t.getList("pockets",Tag.TAG_COMPOUND);
        for(int i=0;i<pockets.size();i++)try{
            CompoundTag p=pockets.getCompound(i);data.pocketReturns.put(p.getInt("index"),Anchor.read(p.getCompound("return")));
        }catch(RuntimeException e){LogUtils.getLogger().error("Invalid World Fold pocket in save",e);}
        return data;
    }
}
