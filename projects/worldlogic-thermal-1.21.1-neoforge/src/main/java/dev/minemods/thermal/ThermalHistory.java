package dev.minemods.thermal;
import java.util.*;
public final class ThermalHistory {
    public static final int LIMIT=840, INTERVAL=200;
    public static final long RETENTION=7*24000L;
    public record Point(long tick,long time,String dimension,String biome,int x,int y,int z,float air,float felt,float body){
        public long day(){return Math.floorDiv(time,24000L);}
        public int dayTick(){return (int)Math.floorMod(time,24000L);}
    }
    private ThermalHistory(){}
    public static boolean valid(Point p){return Float.isFinite(p.air())&&Float.isFinite(p.felt())&&Float.isFinite(p.body())&&p.dimension().length()<=256&&p.biome().length()<=256;}
    public static boolean due(Point previous,long now){return previous==null||now<previous.tick()||now-previous.tick()>=INTERVAL;}
    public static List<Point> append(List<Point> old,Point point){
        if(!valid(point))return List.copyOf(old);
        var out=new ArrayList<Point>();
        for(Point p:old)if(valid(p)&&p.tick()<=point.tick()&&point.tick()-p.tick()<RETENTION)out.add(p);
        if(!out.isEmpty()&&out.getLast().tick()==point.tick())out.removeLast();
        out.add(point);
        if(out.size()>LIMIT)out.subList(0,out.size()-LIMIT).clear();
        return List.copyOf(out);
    }
    public static boolean connects(Point a,Point b){
        long dt=b.tick()-a.tick(), clock=b.time()-a.time();
        return a.day()==b.day()&&a.dimension().equals(b.dimension())&&dt>0&&dt<=INTERVAL*2&&clock>0&&clock<=INTERVAL*2
          &&Math.hypot((double)a.x()-b.x(),(double)a.z()-b.z())<=256;
    }
    public static List<Long> days(List<Point> points){return points.stream().map(Point::day).distinct().sorted(Comparator.reverseOrder()).limit(7).toList();}
}
