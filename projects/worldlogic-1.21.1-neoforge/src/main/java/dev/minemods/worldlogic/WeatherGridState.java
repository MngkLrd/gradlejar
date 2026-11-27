package dev.minemods.worldlogic;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/** Persistent opt-in atmospheric grid, one bounded domain per world. */
public final class WeatherGridState extends SavedData {
 AtmosphericGrid grid;
 static WeatherGridState get(ServerLevel l){
  return l.getDataStorage().computeIfAbsent(new SavedData.Factory<>(WeatherGridState::new,WeatherGridState::load,null),"worldlogic_weather_grid");
 }
 static WeatherGridState load(CompoundTag n,HolderLookup.Provider lookup){
  WeatherGridState s=new WeatherGridState();
  if(n.getInt("version")!=1)return s;
  double[] m=decode(n.getLongArray("mass")),h=decode(n.getLongArray("heat")),q=decode(n.getLongArray("vapor"));
  double wx=n.getDouble("windX"),wz=n.getDouble("windZ");
  if(AtmosphericGrid.valid(m,h,q)&&Double.isFinite(wx)&&Double.isFinite(wz))s.grid=new AtmosphericGrid(n.getInt("x"),n.getInt("z"),wx,wz,n.getLong("tick"),m,h,q);
  return s;
 }
 private static long[] encode(double[] a){long[] b=new long[a.length];for(int i=0;i<a.length;i++)b[i]=Double.doubleToLongBits(a[i]);return b;}
 private static double[] decode(long[] a){double[] b=new double[a.length];for(int i=0;i<a.length;i++)b[i]=Double.longBitsToDouble(a[i]);return b;}
 @Override public CompoundTag save(CompoundTag n,HolderLookup.Provider lookup){
  if(grid==null)return n;
  n.putInt("version",1);n.putInt("x",grid.originX);n.putInt("z",grid.originZ);n.putLong("tick",grid.lastTick);
  n.putDouble("windX",grid.windX);n.putDouble("windZ",grid.windZ);
  n.putLongArray("mass",encode(grid.mass));n.putLongArray("heat",encode(grid.heat));n.putLongArray("vapor",encode(grid.water));
  return n;
 }
 AtmosphericGrid.Cell update(ServerLevel l,BlockPos anchor,double baseC,double moisture,double wx,double wz){
  long now=l.getGameTime();
  if(grid==null||!grid.contains(anchor.getX(),anchor.getZ()))grid=AtmosphericGrid.initialize(l.getSeed(),anchor.getX(),anchor.getZ(),now,baseC,moisture,wx,wz);
  else grid.advance(l.getSeed(),now);
  setDirty();return grid.sample(anchor.getX(),anchor.getZ(),anchor.getY()-l.getSeaLevel());
 }
 public record Conditions(double pressureHpa,double airC,double vaporGkg,double relativeHumidityPercent,long modeledTick){}
 public static Optional<Conditions> sample(ServerLevel l,BlockPos pos){
  var g=get(l).grid;
  if(g==null||!g.contains(pos.getX(),pos.getZ()))return Optional.empty();
  var c=g.sample(pos.getX(),pos.getZ(),pos.getY()-l.getSeaLevel());
  return Optional.of(new Conditions(c.pressureHpa(),c.airC(),c.mixingRatioGkg(),c.relativeHumidity(),g.lastTick));
 }
}
