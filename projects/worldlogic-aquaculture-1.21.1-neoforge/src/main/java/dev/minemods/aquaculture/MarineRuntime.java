package dev.minemods.aquaculture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.AABB;
import java.util.*;

/** Permanent founding record per 128x128 sea cell. No player proximity requirement or chunk tickets. */
public final class MarineRuntime extends SavedData{
 private final Set<Long> seeded=new HashSet<>();
 private static final SavedData.Factory<MarineRuntime> FACTORY=new SavedData.Factory<>(MarineRuntime::new,MarineRuntime::load,null);
 private static MarineRuntime load(CompoundTag n,HolderLookup.Provider lookup){var d=new MarineRuntime();for(long v:n.getLongArray("seeded"))d.seeded.add(v);return d;}
 @Override public CompoundTag save(CompoundTag n,HolderLookup.Provider lookup){n.putLongArray("seeded",seeded.stream().mapToLong(Long::longValue).toArray());return n;}
 static long cell(BlockPos p){return ((long)Math.floorDiv(p.getX(),128)<<32)|(Math.floorDiv(p.getZ(),128)&0xffffffffL);}
 static MarineSpecies choose(Aquaculture.Habitat h,net.minecraft.util.RandomSource r){
  int total=0;for(var s:MarineSpecies.values())if(s.suitable(h))total+=s.weight;
  if(total==0)return null;
  int n=r.nextInt(total);for(var s:MarineSpecies.values())if(s.suitable(h)){n-=s.weight;if(n<0)return s;}return null;
 }
 static void trySeed(ServerLevel l,BlockPos p){
  if(l.random.nextInt(35)!=0)return;
  var data=l.getDataStorage().computeIfAbsent(FACTORY,"worldlogic_aqua_marine");
  long key=cell(p);if(data.seeded.contains(key))return;
  var h=Aquaculture.habitat(l,p);MarineSpecies s=choose(h,l.random);if(s==null)return;
  if(!l.getEntitiesOfClass(MarineAnimal.class,new AABB(p).inflate(72)).isEmpty())return;
  var entity=Aquaculture.MARINE.get(s).get().create(l);if(entity==null)return;
  entity.moveTo(p.getX()+.5,p.getY()+.1,p.getZ()+.5,l.random.nextFloat()*360,0);
  // A wide animal may not spawn intersecting coastline or unloaded chunks.
  AABB bb=entity.getBoundingBox();
  for(int x=(int)Math.floor(bb.minX);x<=Math.floor(bb.maxX);x++)for(int z=(int)Math.floor(bb.minZ);z<=Math.floor(bb.maxZ);z++)
   for(int y=(int)Math.floor(bb.minY);y<=Math.floor(bb.maxY);y++)if(!Aquaculture.water(l,new BlockPos(x,y,z)))return;
  if(!l.noCollision(entity)||!l.addFreshEntity(entity))return;
  data.seeded.add(key);data.setDirty();
 }
}
