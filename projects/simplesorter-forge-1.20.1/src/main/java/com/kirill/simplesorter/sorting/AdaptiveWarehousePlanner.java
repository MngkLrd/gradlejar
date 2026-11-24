package com.kirill.simplesorter.sorting;

import java.util.*;

/**
 * One capacity-aware plan over ALL eligible inventories. This class never
 * accesses Minecraft or mutates an inventory. Every candidate starts from
 * a complete legal layout and preserves exactly one destination per stack.
 */
public final class AdaptiveWarehousePlanner {
    private AdaptiveWarehousePlanner() {}

    public record Stack(int chest,int slot,SemanticItemProfile profile) {}
    public record Storage(int capacity,int x2,int y,int z2) {
        public Storage { if(capacity<=0) throw new IllegalArgumentException("capacity"); }
    }
    @FunctionalInterface public interface Accepts { boolean test(int item,int chest,int slot); }
    public record Plan(int[] chestByStack,int[] slotByStack,int movedStacks,
                       long beforeCost,long afterCost,int activeInventories,int groups) {
        public Plan { chestByStack=chestByStack.clone();slotByStack=slotByStack.clone(); }
        @Override public int[] chestByStack(){return chestByStack.clone();}
        @Override public int[] slotByStack(){return slotByStack.clone();}
    }
    private record Address(int chest,int slot) {}
    private record Candidate(int[] chest,int[] slot,long cost,int moved) {}

    public static Plan plan(List<Stack> input,List<Storage> storage,Accepts accepts) {
        return plan(input,storage,accepts,false);
    }

    public static Plan plan(List<Stack> input,List<Storage> storage,Accepts accepts,boolean spatial) {
        Search search=new Search(input,storage,accepts,spatial);
        while(!search.advance()){}
        return search.result();
    }

    /** One candidate per advance; no game objects or inventories are mutated. */
    public static final class Search {
        private final List<Stack> items;
        private final List<Storage> boxes;
        private final Accepts accepts;
        private final boolean spatial;
        private final int[][] original;
        private final List<Integer> sorted=new ArrayList<>();
        private final List<List<Integer>> orders;
        private final long before;
        private Candidate best;
        private int cursor;
        public Search(List<Stack> input,List<Storage> storage,Accepts accepts,boolean spatial){
            this.items=List.copyOf(input);this.boxes=List.copyOf(storage);
            this.accepts=accepts;this.spatial=spatial;
            int n=items.size(),c=boxes.size();
            int[] home=new int[n],origin=new int[n];
            original=new int[c][];
            for(int i=0;i<c;i++){original[i]=new int[boxes.get(i).capacity];Arrays.fill(original[i],-1);}
            String[] keys=new String[n];
            for(int i=0;i<n;i++){
                Stack a=items.get(i);
                if(a.chest<0||a.chest>=c||a.slot<0||a.slot>=original[a.chest].length
                        ||original[a.chest][a.slot]>=0)throw new IllegalArgumentException("invalid original slot");
                original[a.chest][a.slot]=i;home[i]=a.chest;origin[i]=a.slot;
                sorted.add(i);keys[i]=semanticKey(a.profile);
            }
            before=cost(items,boxes,home,spatial);
            best=new Candidate(home.clone(),origin.clone(),before,0);
            sorted.sort(Comparator.comparing((Integer i)->keys[i])
                    .thenComparing(i->items.get(i).profile.itemId())
                    .thenComparingInt(i->items.get(i).chest).thenComparingInt(i->items.get(i).slot));
            List<Integer> natural=SpatialWarehouseLayout.columnOrder(boxes,false);
            List<List<Integer>> candidates=new ArrayList<>();
            candidates.add(natural);
            List<Integer> capacityFirst=new ArrayList<>(natural);
            capacityFirst.sort(Comparator.comparingInt((Integer i)->-boxes.get(i).capacity));
            candidates.add(capacityFirst);
            List<Integer> reverse=new ArrayList<>(natural);Collections.reverse(reverse);candidates.add(reverse);
            if(spatial){
                candidates.add(SpatialWarehouseLayout.columnOrder(boxes,true));
                candidates.add(SpatialWarehouseLayout.neighborOrder(boxes));
            }
            orders=List.copyOf(new LinkedHashSet<>(candidates));
        }
        public boolean advance(){
            if(items.isEmpty()||cursor>=orders.size()*2)return true;
            List<Integer> order=orders.get(cursor/2);
            int granularity=cursor%2==0?3:2;cursor++;
            List<Address> desired=pack(items,boxes,sorted,order,granularity);
            if(desired.size()==items.size()){
                Candidate candidate=repair(items,boxes,original,sorted,desired,accepts,spatial);
                if(candidate.cost<best.cost || candidate.cost==best.cost&&candidate.moved<best.moved)best=candidate;
            }
            return cursor>=orders.size()*2;
        }
        public Plan result(){
            Set<Integer> used=new HashSet<>();Set<String> groups=new HashSet<>();
            for(int i=0;i<items.size();i++){used.add(best.chest[i]);groups.add(items.get(i).profile.group());}
            return new Plan(best.chest,best.slot,best.moved,before,best.cost,used.size(),groups.size());
        }
    }

    private static int occupied(int[] row){int n=0;for(int i:row)if(i>=0)n++;return n;}
    private static String semanticKey(SemanticItemProfile p){
        String material=p.hasMaterial()?p.material():"~"+p.legacyFamily();
        if("unknown".equals(p.group()))material=p.itemId();
        // Equipment material must not force tools, armour and ammunition into
        // one fine group just because all three happen to contain iron.
        if("equipment".equals(p.group()))material=p.purpose()+"/"+material;
        return p.group()+"/"+material+"/"+p.form()+"/"+p.legacyFamily();
    }
    private static String cluster(SemanticItemProfile p) {
        if("unknown".equals(p.group()))return "unknown/"+p.itemId();
        if("equipment".equals(p.group()))return p.group()+"/"+p.purpose()+"/"+(p.hasMaterial()?p.material():p.legacyFamily());
        return p.group()+"/"+(p.hasMaterial()?p.material():p.legacyFamily());
    }
    private static int boundary(SemanticItemProfile a,SemanticItemProfile b){
        if(!a.group().equals(b.group()))return 4;
        if(!cluster(a).equals(cluster(b)))return 3;
        if(!a.form().equals(b.form()))return 2;
        if(!a.itemId().equals(b.itemId()))return 1;
        return 0;
    }

    /**
     * Adaptive packing. With spare capacity cut at the strongest semantic
     * boundary; with scarce capacity coalesce adjacent related groups. A
     * large material can span several full inventories. No fixed category
     * count or requirement for one dedicated chest per family.
     */
    private static List<Address> pack(List<Stack> items,List<Storage> boxes,
            List<Integer> sorted,List<Integer> order,int granularity){
        List<Address> target=new ArrayList<>(items.size());
        long remaining=boxes.stream().mapToLong(Storage::capacity).sum();
        int at=0;
        for(int chest:order){
            int cap=boxes.get(chest).capacity;remaining-=cap;
            int left=sorted.size()-at;
            if(left<=0)break;
            int max=Math.min(cap,left);
            int min=(int)Math.max(1,left-remaining);
            int take=max,bestStrength=0;
            // Never waste space on a weak boundary when a more meaningful
            // material/group boundary can fit in this inventory.
            for(int k=min;k<max;k++){
                int strength=boundary(items.get(sorted.get(at+k-1)).profile,
                        items.get(sorted.get(at+k)).profile);
                if(strength>=granularity && strength>=bestStrength){bestStrength=strength;take=k;}
            }
            // Even if all remaining stacks fit here, keep the selected
            // semantic cut when later storage has capacity. Otherwise a
            // newly added empty chest can never improve a small warehouse.
            if(max<left && boundary(items.get(sorted.get(at+max-1)).profile,
                    items.get(sorted.get(at+max)).profile)>=bestStrength)take=max;
            for(int slot=0;slot<take;slot++)target.add(new Address(chest,slot));
            at+=take;
        }
        return target;
    }

    /**
     * Move toward an ideal layout by legal virtual swaps. A target may be
     * currently occupied; accepts must evaluate validity when empty.
     * Direct swaps preserve feasible capacity even for restricted inventories.
     * A failed swap leaves those items at their known valid places.
     */
    private static Candidate repair(List<Stack> items,List<Storage> boxes,int[][] original,
            List<Integer> sorted,List<Address> desired,Accepts accepts,boolean spatial){
        int n=items.size();int[] chest=new int[n],slot=new int[n];
        int[][] layout=new int[original.length][];
        for(int c=0;c<original.length;c++)layout[c]=original[c].clone();
        for(int i=0;i<n;i++){chest[i]=items.get(i).chest;slot[i]=items.get(i).slot;}
        // Several deterministic passes resolve ordinary permutations and
        // some blocked direct swaps without an unbounded matching search.
        for(int pass=0;pass<3;pass++){
            int changed=0;
            for(int k=0;k<n;k++){
                int i=sorted.get(k);Address dst=desired.get(k);
                int a=chest[i],b=slot[i];
                if(a==dst.chest&&b==dst.slot)continue;
                int j=layout[dst.chest][dst.slot];
                if(!items.get(i).profile.moveAllowed() || !accepts.test(i,dst.chest,dst.slot))continue;
                if(j>=0&&(!items.get(j).profile.moveAllowed()||!accepts.test(j,a,b)))continue;
                layout[dst.chest][dst.slot]=i;chest[i]=dst.chest;slot[i]=dst.slot;
                layout[a][b]=j;
                if(j>=0){chest[j]=a;slot[j]=b;}
                changed++;
            }
            if(changed==0)break;
        }
        int moved=0;
        for(int i=0;i<n;i++)if(chest[i]!=items.get(i).chest||slot[i]!=items.get(i).slot)moved++;
        return new Candidate(chest,slot,cost(items,boxes,chest,spatial),moved);
    }

    /**
     * Quality is measured on actual semantic compatibility, not on the
     * number of fixed categories. Pair impurity is normalized per chest,
     * with a separate fragmentation penalty so scattering singletons is
     * not rewarded. Spatial cost cannot make unrelated groups equivalent.
     */
    private static long cost(List<Stack> items,List<Storage> boxes,int[] assigned,boolean spatial){
        List<List<SemanticItemProfile>> contents=new ArrayList<>();
        for(int i=0;i<boxes.size();i++)contents.add(new ArrayList<>());
        for(int i=0;i<items.size();i++)contents.get(assigned[i]).add(items.get(i).profile);
        Map<String,Set<Integer>> spread=new HashMap<>();
        Map<String,Set<SpatialWarehouseLayout.Column>> columns=new HashMap<>();
        Map<String,Set<SpatialWarehouseLayout.Column>> relatedColumns=new HashMap<>();
        long result=0;
        for(int c=0;c<contents.size();c++){
            List<SemanticItemProfile> row=contents.get(c);
            int n=row.size();if(n==0)continue;
            long impurity=0;
            // Repeated stacks of one semantic profile need one pair
            // evaluation with multiplicity, not O(n²) identical evaluations.
            Map<SemanticItemProfile,Integer> frequency=new LinkedHashMap<>();
            for(var p:row)frequency.merge(p,1,Integer::sum);
            List<Map.Entry<SemanticItemProfile,Integer>> distinct=new ArrayList<>(frequency.entrySet());
            for(int i=0;i<distinct.size();i++){
                SemanticItemProfile p=distinct.get(i).getKey();
                long count=distinct.get(i).getValue();
                String key=cluster(p);
                spread.computeIfAbsent(key,k->new HashSet<>()).add(c);
                if(spatial && !"unknown".equals(p.group())){
                    var column=SpatialWarehouseLayout.column(boxes.get(c));
                    columns.computeIfAbsent(key,k->new HashSet<>()).add(column);
                    relatedColumns.computeIfAbsent(p.group(),k->new HashSet<>()).add(column);
                }
                impurity+=count*(count-1)/2*(100-p.affinity(p));
                for(int j=0;j<i;j++)impurity+=count*distinct.get(j).getValue()*(100-p.affinity(distinct.get(j).getKey()));
            }
            if(n>1)result+=2*impurity/(n-1);
        }
        for(Set<Integer> set:spread.values())result+=12L*Math.max(0,set.size()-1);
        if(spatial){
            // Same material in one vertical column first; adjacent columns
            // next. A weaker family-wide term keeps related groups nearby.
            for(var set:columns.values())result+=SpatialWarehouseLayout.dispersion(set,24,3);
            for(var set:relatedColumns.values())result+=SpatialWarehouseLayout.dispersion(set,3,1);
        }
        return result;
    }

}
