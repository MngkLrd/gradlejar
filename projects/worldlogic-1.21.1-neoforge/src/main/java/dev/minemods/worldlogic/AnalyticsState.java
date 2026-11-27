package dev.minemods.worldlogic;
import java.util.*;
import net.minecraft.nbt.*;
import net.minecraft.core.HolderLookup;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
final class AnalyticsState extends SavedData{
 static final int LIMIT=4096,HISTORY=48;
 record Point(long tick,double air,double soil){}
 static final class Cell{
  double combustion,dust,soil;long tick=-1;int sources,logs,mined,disturbed,placed;
  double canopy=-1,baselineCanopy=-1,recoveredCanopy;long canopyTick=-1;
  final ArrayDeque<Point> history=new ArrayDeque<>();
  double air(){return Math.min(1,combustion+dust);}
  void record(long now){if(history.isEmpty()||now-history.getLast().tick()>=1200){history.add(new Point(now,air(),soil));while(history.size()>HISTORY)history.removeFirst();}}
 }
 final Map<Long,Cell> cells=new HashMap<>();
 static AnalyticsState get(ServerLevel l){return l.getDataStorage().computeIfAbsent(new SavedData.Factory<>(AnalyticsState::new,AnalyticsState::load,null),"worldlogic_analytics");}
 Cell cell(long key){var c=cells.get(key);if(c==null&&cells.size()<LIMIT){c=new Cell();cells.put(key,c);setDirty();}return c;}
 static double unit(double v){return Double.isFinite(v)?Math.max(0,Math.min(1,v)):0;}
 static AnalyticsState load(CompoundTag root,HolderLookup.Provider lookup){
  var s=new AnalyticsState();
  for(Tag tag:root.getList("cells",Tag.TAG_COMPOUND)){
   var n=(CompoundTag)tag;var c=s.cell(n.getLong("key"));if(c==null)break;
   c.combustion=unit(n.getDouble("combustion"));c.dust=unit(n.getDouble("dust"));c.soil=unit(n.getDouble("soil"));c.tick=n.getLong("tick");
   c.sources=Math.max(0,n.getInt("sources"));c.logs=Math.max(0,n.getInt("logs"));c.mined=Math.max(0,n.getInt("mined"));c.disturbed=Math.max(0,n.getInt("disturbed"));c.placed=Math.max(0,n.getInt("placed"));
   if(n.contains("canopy")){c.canopy=unit(n.getDouble("canopy"));c.baselineCanopy=unit(n.getDouble("baselineCanopy"));c.recoveredCanopy=Math.max(0,n.getDouble("recoveredCanopy"));c.canopyTick=n.getLong("canopyTick");}
   for(Tag t:n.getList("history",Tag.TAG_COMPOUND)){var p=(CompoundTag)t;c.history.add(new Point(p.getLong("t"),unit(p.getDouble("a")),unit(p.getDouble("s"))));if(c.history.size()>=HISTORY)break;}
  }return s;
 }
 @Override public CompoundTag save(CompoundTag root,HolderLookup.Provider lookup){
  var list=new ListTag();
  cells.forEach((key,c)->{var n=new CompoundTag();n.putLong("key",key);n.putDouble("combustion",c.combustion);n.putDouble("dust",c.dust);n.putDouble("soil",c.soil);n.putLong("tick",c.tick);n.putInt("sources",c.sources);n.putInt("logs",c.logs);n.putInt("mined",c.mined);n.putInt("disturbed",c.disturbed);n.putInt("placed",c.placed);if(c.canopy>=0){n.putDouble("canopy",c.canopy);n.putDouble("baselineCanopy",c.baselineCanopy);n.putDouble("recoveredCanopy",c.recoveredCanopy);n.putLong("canopyTick",c.canopyTick);}var h=new ListTag();for(var p:c.history){var t=new CompoundTag();t.putLong("t",p.tick());t.putDouble("a",p.air());t.putDouble("s",p.soil());h.add(t);}n.put("history",h);list.add(n);});
  root.put("cells",list);return root;
 }
}
