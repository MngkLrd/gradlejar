package dev.minemods.thermal.server;
import dev.minemods.thermal.ThermalHistory;
import dev.minemods.thermal.ThermalHistory.Point;
import dev.minemods.thermal.WeatherReading;
import dev.minemods.thermal.ThermalModel;
import dev.minemods.thermal.network.HistorySync;
import java.util.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;
final class HistoryStore {
    static final String KEY="worldlogic_thermal_history";
    private static final Set<ServerPlayer> SENT=Collections.newSetFromMap(new WeakHashMap<>());
    private HistoryStore(){}
    static List<Point> read(CompoundTag root){
        ListTag list=root.getList(KEY,Tag.TAG_COMPOUND);var result=new ArrayList<Point>();
        for(int i=Math.max(0,list.size()-ThermalHistory.LIMIT);i<list.size();i++){
            CompoundTag n=list.getCompound(i);var p=new Point(n.getLong("tick"),n.getLong("time"),n.getString("dim"),n.getString("biome"),
                n.getInt("x"),n.getInt("y"),n.getInt("z"),n.getFloat("air"),n.getFloat("felt"),n.getFloat("body"));
            if(ThermalHistory.valid(p))result.add(p);
        }
        return List.copyOf(result);
    }
    static void write(CompoundTag root,List<Point> points){
        ListTag list=new ListTag();
        for(Point p:points.subList(Math.max(0,points.size()-ThermalHistory.LIMIT),points.size())){
            CompoundTag n=new CompoundTag();n.putLong("tick",p.tick());n.putLong("time",p.time());n.putString("dim",p.dimension());n.putString("biome",p.biome());
            n.putInt("x",p.x());n.putInt("y",p.y());n.putInt("z",p.z());n.putFloat("air",p.air());n.putFloat("felt",p.felt());n.putFloat("body",p.body());list.add(n);
        }
        root.put(KEY,list);
    }
    static void tick(ServerPlayer p,WeatherReading w,ThermalModel.State state,double air){
        var level=p.serverLevel().getServer().overworld();
        long tick=level.getGameTime(),time=level.getDayTime();
        var root=p.getPersistentData();ListTag raw=root.getList(KEY,Tag.TAG_COMPOUND);
        boolean due=raw.isEmpty()||tick<raw.getCompound(raw.size()-1).getLong("tick")||tick-raw.getCompound(raw.size()-1).getLong("tick")>=ThermalHistory.INTERVAL;
        if(!due&&SENT.contains(p))return;
        var points=read(root);
        if(due){
            var pos=p.blockPosition();points=ThermalHistory.append(points,new Point(tick,time,w.dimension(),w.biome(),pos.getX(),pos.getY(),pos.getZ(),(float)air,(float)state.feelsLikeC(),(float)state.bodyC()));
            write(root,points);
        }
        HistorySync.send(p,points);SENT.add(p);
    }
    static void cloneHistory(net.minecraft.world.entity.player.Player old,net.minecraft.world.entity.player.Player next){
        if(old.getPersistentData().contains(KEY))next.getPersistentData().put(KEY,old.getPersistentData().getList(KEY,Tag.TAG_COMPOUND).copy());
    }
    static void logout(ServerPlayer p){SENT.remove(p);}
}
