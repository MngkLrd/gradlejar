package com.kirill.simplesorter.sorting;

import java.util.*;

/** Deterministic geometry of actual chest footprints, not an imposed grid. */
public final class SpatialWarehouseLayout {
    private SpatialWarehouseLayout() {}
    public record Column(int x2,int z2) {}
    public static Column column(AdaptiveWarehousePlanner.Storage s){return new Column(s.x2(),s.z2());}
    public static List<Integer> columnOrder(List<AdaptiveWarehousePlanner.Storage> boxes,boolean swapAxes){
        List<Integer> result=new ArrayList<>();
        for(int i=0;i<boxes.size();i++)result.add(i);
        result.sort(Comparator.comparingInt((Integer i)->swapAxes?boxes.get(i).z2():boxes.get(i).x2())
                .thenComparingInt(i->swapAxes?boxes.get(i).x2():boxes.get(i).z2())
                .thenComparing(Comparator.comparingInt((Integer i)->boxes.get(i).y()).reversed())
                .thenComparingInt(i->i));
        return result;
    }
    /** Walk nearby columns in order, preserving vertical adjacency inside each. */
    public static List<Integer> neighborOrder(List<AdaptiveWarehousePlanner.Storage> boxes){
        Map<Column,List<Integer>> grouped=new LinkedHashMap<>();
        for(int i:columnOrder(boxes,false))grouped.computeIfAbsent(column(boxes.get(i)),k->new ArrayList<>()).add(i);
        List<Column> remaining=new ArrayList<>(grouped.keySet());List<Integer> result=new ArrayList<>();
        Column current=null;
        while(!remaining.isEmpty()){
            int best=0;
            if(current!=null)for(int i=1;i<remaining.size();i++)
                if(distance(current,remaining.get(i))<distance(current,remaining.get(best)))best=i;
            current=remaining.remove(best);result.addAll(grouped.get(current));
        }
        return result;
    }
    private static long distance(Column a,Column b){return Math.abs((long)a.x2-b.x2)+Math.abs((long)a.z2-b.z2);}
    /** Bounded spread cost; footprint coordinates are doubled to support double chests. */
    public static long dispersion(Set<Column> columns,int columnWeight,int distanceWeight){
        if(columns.size()<2)return 0;
        int minX=Integer.MAX_VALUE,maxX=Integer.MIN_VALUE,minZ=Integer.MAX_VALUE,maxZ=Integer.MIN_VALUE;
        for(Column c:columns){minX=Math.min(minX,c.x2);maxX=Math.max(maxX,c.x2);minZ=Math.min(minZ,c.z2);maxZ=Math.max(maxZ,c.z2);}
        long span=((long)maxX-minX+(long)maxZ-minZ+1)/2;
        return (long)columnWeight*(columns.size()-1)+(long)distanceWeight*Math.min(128,span);
    }
}
