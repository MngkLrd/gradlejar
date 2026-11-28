package dev.minemods.worldlogic;
import java.util.*;
import net.minecraft.nbt.*;
import net.minecraft.core.HolderLookup;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
final class HydrologyState extends SavedData{
 static final int LIMIT=16384;
 record Cell(HydrologyModel.Store store,double runoff,double evaporation,double recharge,double discharge,long tick){}
 record Eco(double salt,double cover,double roots,double fertilityDelta,double transpiration,double totalTranspiration,double saltExport,double recoveredCover,long vegetationTick,long irrigationTick){}
 static Eco emptyEco(){return new Eco(0,0,0,0,0,0,0,0,-1,-1);}
 final Map<Long,Cell> cells=new HashMap<>();
 final Map<Long,Eco> ecology=new HashMap<>();
 static HydrologyState get(ServerLevel l){return l.getDataStorage().computeIfAbsent(new SavedData.Factory<>(HydrologyState::new,HydrologyState::load,null),"worldlogic_hydrology");}
 static HydrologyState load(CompoundTag root,HolderLookup.Provider lookup){
  var d=new HydrologyState();
  for(Tag tag:root.getList("cells",Tag.TAG_COMPOUND)){
   var c=(CompoundTag)tag;double[] a={c.getDouble("soil"),c.getDouble("gw"),c.getDouble("snow"),c.getDouble("pond"),c.getDouble("runoff"),c.getDouble("evap"),c.getDouble("recharge"),c.getDouble("discharge")};
   if(d.cells.size()>=LIMIT)break;
   if(java.util.Arrays.stream(a).anyMatch(v->!Double.isFinite(v)||v<0))continue;
   d.cells.put(c.getLong("key"),new Cell(new HydrologyModel.Store(a[0],a[1],a[2],a[3]),a[4],a[5],a[6],a[7],c.getLong("tick")));
   if(c.contains("ecology")){
    var e=c.getCompound("ecology");double[] v={e.getDouble("salt"),e.getDouble("cover"),e.getDouble("roots"),e.getDouble("fertility"),e.getDouble("transpiration"),e.getDouble("total"),e.getDouble("export"),e.getDouble("recovered")};
    if(java.util.Arrays.stream(v).allMatch(Double::isFinite)&&v[0]>=0&&v[4]>=0&&v[5]>=0&&v[6]>=0&&v[7]>=0)
     d.ecology.put(c.getLong("key"),new Eco(v[0],HydrologyModel.unit(v[1]),HydrologyModel.unit(v[2]),Math.max(-.5,Math.min(.5,v[3])),v[4],v[5],v[6],v[7],e.getLong("vegetationTick"),e.getLong("irrigationTick")));
   }
  }
  return d;
 }
 @Override public CompoundTag save(CompoundTag root,HolderLookup.Provider lookup){
  ListTag list=new ListTag();
  cells.forEach((key,v)->{var c=new CompoundTag();c.putLong("key",key);c.putDouble("soil",v.store.soil());c.putDouble("gw",v.store.groundwater());c.putDouble("snow",v.store.snow());c.putDouble("pond",v.store.pond());c.putDouble("runoff",v.runoff);c.putDouble("evap",v.evaporation);c.putDouble("recharge",v.recharge);c.putDouble("discharge",v.discharge);c.putLong("tick",v.tick);
   var e=ecology.get(key);if(e!=null){var n=new CompoundTag();n.putDouble("salt",e.salt);n.putDouble("cover",e.cover);n.putDouble("roots",e.roots);n.putDouble("fertility",e.fertilityDelta);n.putDouble("transpiration",e.transpiration);n.putDouble("total",e.totalTranspiration);n.putDouble("export",e.saltExport);n.putDouble("recovered",e.recoveredCover);n.putLong("vegetationTick",e.vegetationTick);n.putLong("irrigationTick",e.irrigationTick);c.put("ecology",n);}
   list.add(c);});
  root.put("cells",list);return root;
 }
}
