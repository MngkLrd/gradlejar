package com.kirill.simplesorter.sorting;
import java.util.List;
public final class SemanticChestQualitySelfTest {
    static void check(boolean b,String msg){if(!b)throw new AssertionError(msg);}
    static SemanticItemProfile p(String id,String family,String group,String material,String form){
        return new SemanticItemProfile(id,family,family,group,material,form,family,true,
                SemanticItemProfile.Confidence.TAG,List.of());
    }
    public static void main(String[] args){
        var iron=p("x:iron_ingot","finished_metals","metals_minerals","iron","ingot");
        var ore=p("x:iron_ore","mineral_resources","metals_minerals","iron","ore");
        var copper=p("x:copper_ingot","finished_metals","metals_minerals","copper","ingot");
        var apple=p("x:apple","food","food","","");
        var unknown=p("x:a","misc","unknown","","");
        var other=p("x:b","misc","unknown","","");
        var same=SemanticChestQuality.evaluate(List.of(iron,ore));
        check(same.label().equals("material/iron")&&!same.mixed()&&same.score()==85,"iron forms");
        check(SemanticChestQuality.evaluate(List.of(ore,copper)).label().equals("group/metals_minerals"),"related material groups");
        check(SemanticChestQuality.evaluate(List.of(iron,apple)).mixed(),"unrelated contents");
        check(SemanticChestQuality.evaluate(List.of(unknown,other)).mixed(),"unknowns not related by namespace");
        check(SemanticChestQuality.evaluate(List.of(unknown,unknown)).label().equals("item/x:a"),"unknown exact ID");
        check(SemanticChestQuality.evaluate(List.of()).occupied()==0,"empty");
        System.out.println("Semantic chest quality passed.");
    }
}
