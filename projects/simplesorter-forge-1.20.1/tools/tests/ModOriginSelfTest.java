package com.kirill.simplesorter.sorting;
import java.util.*;
import com.kirill.simplesorter.settings.SortingMode;
public final class ModOriginSelfTest {
    static void check(boolean value,String msg){if(!value)throw new AssertionError(msg);}
    static SemanticItemProfile p(String id,String group,boolean movable){
        return new SemanticItemProfile(id,group,group,group,"","",group,movable,
                SemanticItemProfile.Confidence.CATEGORY,List.of());
    }
    public static void main(String[] args){
        var iron=p("minecraft:iron_ingot","metals",true);
        var apple=p("minecraft:apple","food",true);
        var raw=p("tfc:ore/pyrite","metals",true);
        var crop=p("tfc:seed/oat","plants",true);
        var opaque=p("unlisted:tt_5","unknown",true);
        var mi=ModOriginProfile.of(iron);var ma=ModOriginProfile.of(apple);
        var tr=ModOriginProfile.of(raw);var tc=ModOriginProfile.of(crop);
        check(mi.affinity(ma)==100&&tr.affinity(tc)==100&&mi.affinity(tr)==0,"namespace purity");
        check(iron.affinity(apple)==0,"smart profile unchanged");
        check(ModOriginProfile.of(opaque).group().equals("mod/unlisted"),"unknown item namespace");
        check(!ModOriginProfile.of(p("tfc:protected","locked",false)).moveAllowed(),"protected item");
        check(SemanticChestQuality.evaluate(List.of(mi,ma)).label().equals("mod/minecraft"),"mod label");
        check(SemanticChestQuality.evaluate(List.of(mi,tr)).mixed(),"mixed mods");
        check(SortingMode.byIndex(4)==SortingMode.BY_MOD&&SortingMode.byId("by_mod")==SortingMode.BY_MOD,"mode persistence");
        check(SortingMode.SMART_CATEGORIES.next()==SortingMode.BY_MOD&&SortingMode.BY_MOD.next()==SortingMode.PLAYER_DEPOSIT,"mode cycling");
        check(SortingMode.byIndex(2)==SortingMode.SMART_CATEGORIES,"legacy index");
        List<AdaptiveWarehousePlanner.Storage> boxes=new ArrayList<>();
        List<AdaptiveWarehousePlanner.Stack> items=new ArrayList<>();
        for(int y=0;y<4;y++)for(int x=0;x<4;x++){
            int c=boxes.size();boxes.add(new AdaptiveWarehousePlanner.Storage(4,x*4,y,0));
            for(int slot=0;slot<4;slot++)items.add(new AdaptiveWarehousePlanner.Stack(c,slot,
                    List.of(mi,ma,tr,tc).get(slot)));
        }
        var result=AdaptiveWarehousePlanner.plan(items,boxes,(i,c,s)->true,true);
        int[] owner=result.chestByStack(),slot=result.slotByStack();
        Set<String> occupied=new HashSet<>();
        Map<Integer,Set<String>> columns=new HashMap<>();
        for(int i=0;i<items.size();i++){
            check(occupied.add(owner[i]+":"+slot[i]),"duplicate destination");
            columns.computeIfAbsent(boxes.get(owner[i]).x2(),k->new HashSet<>()).add(items.get(i).profile().group());
        }
        check(columns.size()==4&&columns.values().stream().allMatch(set->set.size()==1),"vertical mod purity");
        check(result.movedStacks()>0,"full warehouse not reorganized");
        System.out.println("Sort by Mod: namespace, modes, labels, locks and full vertical warehouse passed.");
    }
}
