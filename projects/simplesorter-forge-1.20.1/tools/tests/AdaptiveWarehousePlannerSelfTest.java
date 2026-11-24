package com.kirill.simplesorter.sorting;
import java.util.*;

public final class AdaptiveWarehousePlannerSelfTest {
    private static void check(boolean ok,String msg){if(!ok)throw new AssertionError(msg);}
    private static SemanticItemProfile profile(String id,String material,String form,String group){
        return new SemanticItemProfile(id,group,group,group,material,form,group,true,
                SemanticItemProfile.Confidence.TAG,List.of("test"));
    }
    private static final SemanticItemProfile IRON=profile("test:iron_ingot","iron","ingot","metals_minerals");
    private static final SemanticItemProfile COPPER=profile("test:copper_ingot","copper","ingot","metals_minerals");
    private static final SemanticItemProfile OAK=profile("test:oak_log","oak","log","wood");
    private static final SemanticItemProfile FOOD=profile("test:apple","","","food");
    private static void verify(List<AdaptiveWarehousePlanner.Stack> items,List<AdaptiveWarehousePlanner.Storage> boxes,
                               AdaptiveWarehousePlanner.Plan result,AdaptiveWarehousePlanner.Accepts accepts){
        int[] chest=result.chestByStack(),slot=result.slotByStack();
        check(chest.length==items.size()&&slot.length==items.size(),"lost item");
        Set<String> occupied=new HashSet<>();
        for(int i=0;i<items.size();i++){
            check(chest[i]>=0&&chest[i]<boxes.size()&&slot[i]>=0&&slot[i]<boxes.get(chest[i]).capacity(),"capacity");
            check(occupied.add(chest[i]+":"+slot[i]),"duplicate destination");
            var old=items.get(i);
            check((chest[i]==old.chest()&&slot[i]==old.slot())||accepts.test(i,chest[i],slot[i]),"restricted slot");
        }
        check(result.afterCost()<=result.beforeCost(),"degraded quality");
    }
    private static void largeFullWarehouse(){
        List<AdaptiveWarehousePlanner.Storage> boxes=new ArrayList<>();
        List<AdaptiveWarehousePlanner.Stack> items=new ArrayList<>();
        // Input deliberately interleaves vertical columns, as distance scans can.
        for(int y=0;y<4;y++)for(int col=0;col<10;col++){
            int c=boxes.size();boxes.add(new AdaptiveWarehousePlanner.Storage(4,col*4,y,0));
            for(int slot=0;slot<4;slot++)items.add(new AdaptiveWarehousePlanner.Stack(c,slot,slot%2==0?IRON:COPPER));
        }
        var accept=(AdaptiveWarehousePlanner.Accepts)(i,c,s)->true;
        var plan=AdaptiveWarehousePlanner.plan(items,boxes,accept,true);verify(items,boxes,plan,accept);
        check(plan.movedStacks()>0&&plan.afterCost()<plan.beforeCost(),"full 40-chest warehouse unchanged");
        Map<Integer,Set<String>> byColumn=new HashMap<>();
        for(int i=0;i<items.size();i++){
            int col=boxes.get(plan.chestByStack()[i]).x2();
            byColumn.computeIfAbsent(col,k->new HashSet<>()).add(items.get(i).profile().material());
        }
        check(byColumn.size()==10&&byColumn.values().stream().allMatch(set->set.size()==1),"vertical columns split materials");
        check(Arrays.stream(plan.chestByStack()).anyMatch(i->i>=32),"later chests omitted");
        List<AdaptiveWarehousePlanner.Stack> after=new ArrayList<>();
        int[] dst=plan.chestByStack(),slots=plan.slotByStack();
        for(int i=0;i<items.size();i++)after.add(new AdaptiveWarehousePlanner.Stack(dst[i],slots[i],items.get(i).profile()));
        var repeat=AdaptiveWarehousePlanner.plan(after,boxes,accept,true);
        check(repeat.movedStacks()==0,"unchanged full warehouse should be stable");
        // A new type in one occupied slot makes a fresh plan legal and
        // the cost must never increase even when a full reshuffle is needed.
        after.set(0,new AdaptiveWarehousePlanner.Stack(dst[0],slots[0],FOOD));
        verify(after,boxes,AdaptiveWarehousePlanner.plan(after,boxes,accept,true),accept);
    }
    private static void capacityAndNewStorage(){
        List<AdaptiveWarehousePlanner.Stack> items=List.of(
                new AdaptiveWarehousePlanner.Stack(0,0,IRON),new AdaptiveWarehousePlanner.Stack(0,1,COPPER),
                new AdaptiveWarehousePlanner.Stack(0,2,OAK),new AdaptiveWarehousePlanner.Stack(0,3,FOOD));
        var one=List.of(new AdaptiveWarehousePlanner.Storage(4,0,0,0));
        var two=List.of(one.get(0),new AdaptiveWarehousePlanner.Storage(4,0,1,0));
        var accept=(AdaptiveWarehousePlanner.Accepts)(i,c,s)->true;
        var first=AdaptiveWarehousePlanner.plan(items,one,accept,true);verify(items,one,first,accept);
        check(first.activeInventories()==1,"one chest must be feasible");
        var next=AdaptiveWarehousePlanner.plan(items,two,accept,true);verify(items,two,next,accept);
        check(next.activeInventories()==2&&next.afterCost()<first.afterCost(),"new capacity ignored");
        var restricted=(AdaptiveWarehousePlanner.Accepts)(i,c,s)->c==0||i==3;
        verify(items,two,AdaptiveWarehousePlanner.plan(items,two,restricted,true),restricted);
    }
    private static void components(){
        var a=new WarehouseTransferComponents.Slot(0,0);
        var b=new WarehouseTransferComponents.Slot(1,0);
        var c=new WarehouseTransferComponents.Slot(2,0);
        var d=new WarehouseTransferComponents.Slot(3,0);
        var e=new WarehouseTransferComponents.Slot(4,0);
        var f=new WarehouseTransferComponents.Slot(5,0);
        var g=new WarehouseTransferComponents.Slot(6,0);
        var parts=WarehouseTransferComponents.decompose(List.of(a,b,c,d,e,f),List.of(b,c,a,e,g,f));
        check(parts.size()==2&&parts.get(0).size()==3&&parts.get(1).size()==2,"cycle/path decomposition");
        Set<Integer> all=new HashSet<>();
        for(var group:parts)for(int index:group)check(all.add(index),"duplicate transfer");
        check(all.equals(Set.of(0,1,2,3,4)),"missing transfer or stationary stack");
    }
    public static void main(String[] args){
        largeFullWarehouse();capacityAndNewStorage();components();
        System.out.println("Adaptive warehouse: 40 full chests, vertical material columns, scarce/new capacity, restrictions and transaction components passed.");
    }
}
