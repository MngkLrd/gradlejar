package com.kirill.simplesorter.sorting;
import java.util.*;
/** Independent paths/cycles of a complete, collision-free slot permutation. */
public final class WarehouseTransferComponents {
    private WarehouseTransferComponents() {}
    public record Slot(int chest,int slot) {}
    public static List<List<Integer>> decompose(List<Slot> source,List<Slot> target){
        if(source.size()!=target.size())throw new IllegalArgumentException("layout size");
        int n=source.size();int[] parent=new int[n];
        Map<Slot,Integer> occupant=new HashMap<>();Set<Slot> destinations=new HashSet<>();
        for(int i=0;i<n;i++){
            parent[i]=i;
            if(occupant.put(source.get(i),i)!=null||!destinations.add(target.get(i)))
                throw new IllegalArgumentException("duplicate slot");
        }
        for(int i=0;i<n;i++){
            if(source.get(i).equals(target.get(i)))continue;
            Integer j=occupant.get(target.get(i));
            if(j!=null){
                if(source.get(j).equals(target.get(j)))throw new IllegalArgumentException("stationary destination");
                int a=root(parent,i),b=root(parent,j);parent[a]=b;
            }
        }
        Map<Integer,List<Integer>> groups=new LinkedHashMap<>();
        for(int i=0;i<n;i++)if(!source.get(i).equals(target.get(i)))
            groups.computeIfAbsent(root(parent,i),k->new ArrayList<>()).add(i);
        return groups.values().stream().map(List::copyOf).toList();
    }
    private static int root(int[] parent,int i){
        while(parent[i]!=i){parent[i]=parent[parent[i]];i=parent[i];}return i;
    }
}
