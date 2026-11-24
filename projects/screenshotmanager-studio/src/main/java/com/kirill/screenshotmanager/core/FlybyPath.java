package com.kirill.screenshotmanager.core;
import java.util.*;
import java.util.function.Predicate;

/** Pure Java bounded camera navigation. The caller supplies a loaded-world clearance test. */
public final class FlybyPath {
    public record Vec(double x,double y,double z){
        public double distance(Vec b){return Math.sqrt((x-b.x)*(x-b.x)+(y-b.y)*(y-b.y)+(z-b.z)*(z-b.z));}
        public Vec lerp(Vec b,double t){return new Vec(x+(b.x-x)*t,y+(b.y-y)*t,z+(b.z-z)*t);}
    }
    private record Cell(int x,int y,int z){
        static Cell of(Vec v){return new Cell((int)Math.floor(v.x),(int)Math.floor(v.y),(int)Math.floor(v.z));}
        Vec center(){return new Vec(x+.5,y+.5,z+.5);}
    }
    private record Node(Cell c,double g,double f){}
    private static final int[][] DIR={{1,0,0},{-1,0,0},{0,0,1},{0,0,-1},{0,1,0},{0,-1,0}};
    private final List<Vec> points;private final double[] cumulative;
    private FlybyPath(List<Vec> points){
        this.points=List.copyOf(points);cumulative=new double[points.size()];
        for(int i=1;i<points.size();i++)cumulative[i]=cumulative[i-1]+points.get(i-1).distance(points.get(i));
    }
    public double length(){return cumulative[cumulative.length-1];}
    public Vec sample(double t){
        if(t<=0)return points.getFirst();if(t>=1)return points.getLast();
        double d=length()*t;int i=Arrays.binarySearch(cumulative,d);
        if(i>=0)return points.get(i);
        i=-i-1;int prev=Math.max(0,i-1);
        return points.get(prev).lerp(points.get(i),(d-cumulative[prev])/Math.max(1e-9,cumulative[i]-cumulative[prev]));
    }
    public static boolean line(Vec a,Vec b,Predicate<Vec> free){
        int steps=Math.max(1,(int)Math.ceil(a.distance(b)/.15));
        for(int i=0;i<=steps;i++)if(!free.test(a.lerp(b,i/(double)steps)))return false;
        return true;
    }
    private static Cell anchor(Vec v,Predicate<Vec> free){
        Cell origin=Cell.of(v),best=null;double distance=Double.POSITIVE_INFINITY;
        for(int x=-1;x<=1;x++)for(int y=-1;y<=1;y++)for(int z=-1;z<=1;z++){
            Cell c=new Cell(origin.x+x,origin.y+y,origin.z+z);
            double d=v.distance(c.center());
            if(d<distance&&line(v,c.center(),free)){best=c;distance=d;}
        }return best;
    }
    public static FlybyPath plan(Vec start,Vec end,Predicate<Vec> free){
        if(start.distance(end)>55||!free.test(start)||!free.test(end))return null;
        if(line(start,end,free))return new FlybyPath(List.of(start,end));
        Cell a=anchor(start,free),b=anchor(end,free);if(a==null||b==null)return null;
        var queue=new PriorityQueue<Node>(Comparator.comparingDouble(Node::f));
        Map<Cell,Double> costs=new HashMap<>();Map<Cell,Cell> parents=new HashMap<>();
        Map<Cell,Boolean> clearance=new HashMap<>();
        costs.put(a,0.0);queue.add(new Node(a,0,a.center().distance(b.center())));
        boolean reached=false;
        for(int visited=0;visited<4500&&!queue.isEmpty();visited++){
            Node n=queue.poll();if(n.g>costs.getOrDefault(n.c,Double.POSITIVE_INFINITY))continue;
            if(n.c.equals(b)){reached=true;break;}
            for(int[] d:DIR){
                Cell c=new Cell(n.c.x+d[0],n.c.y+d[1],n.c.z+d[2]);
                if(c.center().distance(end)>55||Math.abs(c.y-a.y)>24&&Math.abs(c.y-b.y)>24)continue;
                double cost=n.g+(d[1]!=0?1.18:1.0);
                if(cost>=costs.getOrDefault(c,Double.POSITIVE_INFINITY))continue;
                if(!clearance.computeIfAbsent(c,k->free.test(k.center()))||!line(n.c.center(),c.center(),free))continue;
                costs.put(c,cost);parents.put(c,n.c);queue.add(new Node(c,cost,cost+c.center().distance(b.center())*1.1));
            }
        }
        if(!reached)return null;
        List<Vec> raw=new ArrayList<>();raw.add(end);
        for(Cell c=b;c!=null;c=parents.get(c))raw.add(c.center());
        raw.add(start);Collections.reverse(raw);
        List<Vec> shortPath=new ArrayList<>();shortPath.add(start);
        for(int i=0;i<raw.size()-1;){
            int next=raw.size()-1;while(next>i+1&&!line(raw.get(i),raw.get(next),free))next--;
            if(!line(raw.get(i),raw.get(next),free))return null;
            Vec v=raw.get(next);if(v.distance(shortPath.getLast())>1e-6)shortPath.add(v);i=next;
        }
        if(shortPath.size()<2)return new FlybyPath(List.of(start,end));
        // Round each corner with a quadratic arc, preserving clearance for the complete arc.
        List<Vec> smooth=new ArrayList<>();smooth.add(start);
        for(int i=1;i<shortPath.size()-1;i++){
            Vec prev=shortPath.get(i-1),corner=shortPath.get(i),next=shortPath.get(i+1);
            boolean rounded=false;
            for(double r:new double[]{1.4,.7,.3,.12}){
                Vec entry=corner.lerp(prev,Math.min(.4,r/Math.max(.001,corner.distance(prev))));
                Vec exit=corner.lerp(next,Math.min(.4,r/Math.max(.001,corner.distance(next))));
                List<Vec> arc=new ArrayList<>();arc.add(entry);boolean ok=line(smooth.getLast(),entry,free);
                for(int k=1;k<=12&&ok;k++){
                    double t=k/12.0;Vec point=entry.lerp(corner,t).lerp(corner.lerp(exit,t),t);
                    ok=line(arc.getLast(),point,free);arc.add(point);
                }
                if(ok){smooth.addAll(arc);rounded=true;break;}
            }
            if(!rounded)smooth.add(corner);
        }
        smooth.add(end);
        for(int i=1;i<smooth.size();i++)if(!line(smooth.get(i-1),smooth.get(i),free))return null;
        return new FlybyPath(smooth);
    }
}
