package com.kirill.simplesorter.sorting;

import java.util.List;

public final class SemanticItemClassifierSelfTest {
    private static void check(boolean ok,String msg) {
        if(!ok) throw new AssertionError(msg);
    }
    private static SemanticItemProfile item(String id,String category,String group,String family,String... tags) {
        return SemanticItemClassifier.classify(new SemanticItemClassifier.Input(
                id,category,group,family,true,false,"test",List.of(tags)));
    }
    private static void expect(SemanticItemProfile p,String group,String material,String form) {
        check(p.group().equals(group) && p.material().equals(material) && p.form().equals(form),
                "Unexpected semantic profile: "+p);
    }
    public static void main(String[] args) {
        var ingot=item("minecraft:iron_ingot","ingots","resources","finished_metals","forge:ingots/iron");
        var ore=item("minecraft:raw_iron","raw_ores","resources","mineral_resources","forge:raw_materials/iron");
        var copper=item("unknownmod:copper_plate","plates","resources","finished_metals","forge:plates/copper");
        var pick=item("minecraft:iron_pickaxe","tools","equipment","tools");
        expect(ingot,"metals_minerals","iron","ingot");
        expect(ore,"metals_minerals","iron","ore");
        expect(copper,"metals_minerals","copper","plate");
        expect(pick,"equipment","iron","pickaxe");
        check(ingot.affinity(ore)>ingot.affinity(copper),"material affinity must beat shared legacy family");
        check(ingot.affinity(pick)==0,"tools cannot be stored as iron materials");
        var mythril=item("example:mythril_ingot","misc","misc","misc");
        expect(mythril,"metals_minerals","mythril","ingot");
        check(mythril.legacyFamily().equals("finished_metals"),"unlisted material form inference");
        var tagged=item("newmod:unusual_widget","misc","misc","misc","c:ingots/adamantite");
        expect(tagged,"metals_minerals","adamantite","ingot");
        check(tagged.confidence()==SemanticItemProfile.Confidence.TAG,"tag evidence");
        expect(item("gtceu:tiny_iron_dust","tiny_dust","resources","mineral_resources"),"metals_minerals","iron","dust");
        expect(item("tfc:metal/ingot/copper","ingots","resources","finished_metals"),"metals_minerals","copper","ingot");
        expect(item("tfc:ore/poor_goethite","raw_ores","resources","mineral_resources"),"metals_minerals","goethite","ore");
        expect(item("minecraft:iron_block","blocks","building","building_blocks","forge:storage_blocks/iron"),"metals_minerals","iron","block");
        expect(item("minecraft:stone_pickaxe","tools","equipment","tools"),"equipment","stone","pickaxe");
        var oakLog=item("minecraft:oak_log","wood","building","wood","minecraft:logs");
        var oakPlank=item("minecraft:oak_planks","wood","building","wood","minecraft:planks");
        expect(oakLog,"wood","oak","log");
        expect(oakPlank,"wood","oak","plank");
        check(oakLog.affinity(oakPlank)>=85,"wood species affinity");
        var conflicting=item("other:iron_ingot","ingots","resources","finished_metals","forge:ingots/copper","forge:ingots/iron");
        expect(conflicting,"metals_minerals","","ingot");
        check(conflicting.evidence().stream().anyMatch(e->e.startsWith("ambiguous-material:")),"tag conflict diagnostic");
        var a=item("other:weird_object","misc","misc","misc");
        var b=item("other:unrelated_object","misc","misc","misc");
        expect(a,"unknown","","");
        check(a.affinity(b)==0,"namespace alone is not semantic similarity");
        check(a.hierarchy().contains("unknown/other") && a.hierarchy().contains("item/other:weird_object"),"safe unknown keys");
        check(!a.hierarchy().equals(b.hierarchy()),"unique unknown identity");
        var water=item("minecraft:potion","potions","consumables","water_containers");
        var potion=item("minecraft:potion","potions","consumables","potions");
        check(water.affinity(potion)<100,"NBT-sensitive water identity");
        var locked=SemanticItemClassifier.classify(new SemanticItemClassifier.Input(
                "other:quest_ingot","quest","custom","quest",false,true,"user lock",List.of("forge:ingots/iron")));
        check(!locked.moveAllowed() && locked.group().equals("locked"),"lock must win");
        var custom=SemanticItemClassifier.classify(new SemanticItemClassifier.Input(
                "minecraft:iron_ingot","my_category","my_group","my_category",true,true,"user rule",List.of("forge:ingots/iron")));
        check(custom.group().equals("custom/my_group") && custom.legacyFamily().equals("my_category"),"user rule preservation");
        check(ingot.hierarchy().equals(List.of("metals_minerals","metals_minerals/material/iron",
                "metals_minerals/material/iron/ingot","item/minecraft:iron_ingot")),"hierarchical grouping keys");
        check(item("any:wooden_bucket","wood","building","wood").group().equals("containers"),"bucket is not wood");
        check(item("any:metal/unfinished_lamp/bronze","misc","misc","misc").group().equals("lighting"),"unfinished lamp");
        check(item("any:jute_fiber","misc","misc","misc").group().equals("textiles"),"fiber");
        check(item("any:handstone","misc","misc","misc").group().equals("equipment"),"handstone");
        check(item("any:blubber","misc","misc","misc").group().equals("animal_resources"),"animal fat");
        check(item("any:plant/syngonium","blocks","building","building_blocks").group().equals("plants"),"plant block");
        check(item("any:humus","blocks","building","building_blocks").group().equals("soil"),"soil");
        check(item("any:tt_5","misc","misc","misc").group().equals("unknown"),"opaque ID remains unknown");
        System.out.println("Semantic classification: unknown mod materials, tags, forms, purpose, hierarchy, conflicts and locks passed.");
    }
}
