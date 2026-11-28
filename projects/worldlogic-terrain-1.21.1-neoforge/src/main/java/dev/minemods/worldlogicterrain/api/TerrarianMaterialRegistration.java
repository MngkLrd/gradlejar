package dev.minemods.worldlogicterrain.api;

import java.util.List;
import dev.minemods.worldlogic.api.PhysicalMaterials;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

/** Only Terrarian-owned materials. Core owns the sole material/composition registry. */
public final class TerrarianMaterialRegistration {
    private static final double N=Double.NaN;
    private TerrarianMaterialRegistration(){}
    public static void setup(FMLCommonSetupEvent event){event.enqueueWork(TerrarianMaterialRegistration::register);}
    private static ResourceLocation id(String path){return ResourceLocation.fromNamespaceAndPath("worldlogicterrain",path);}
    private static void register(){
        wood("willow",400,1.55,50,1.05,0.67);
        wood("larch",590,3.3,90,0.75,0.83);
        wood("pine",510,2.7,80,0.85,0.88);
        wood("fir",450,2,72,0.95,0.79);
        wood("cedar",380,1.5,55,0.8,0.89);
        wood("maple",705,6.4,110,0.66,0.64);
        wood("beech",720,5.9,112,0.68,0.65);
        wood("alder",500,2.6,67,0.85,0.7);
        wood("palm",550,4,63,0.9,0.67);
        wood("date_palm",480,2.8,52,0.95,0.69);
        wood("baobab",250,0.7,25,1.8,0.45);
        wood("ebony",1000,13,155,0.48,0.43);
        wood("mahogany",590,3.8,86,0.65,0.62);
    }
    private static void wood(String name,double densityAtTwelvePercent,double jankaKN,double bendingMPa,double greenMoistureKgKg,double flammability){
        var material=id(name);
        // Existing nominal Terrarian reference density is at 12% moisture.
        // Convert once to Core's dry reference at constant nominal volume.
        PhysicalMaterials.registerMaterial(new PhysicalMaterials.MaterialProfile(material,
            PhysicalMaterials.Phase.SOLID,densityAtTwelvePercent/1.12,N,N,N,N,N,.12,N,N,N,N,flammability,N,N,N));
        PhysicalMaterials.registerWoodProperties(material,new PhysicalMaterials.WoodProperties(jankaKN,bendingMPa,greenMoistureKgKg));
        for(String suffix:new String[]{"_log","_planks"}){
            var block=id(name+suffix);
            PhysicalMaterials.registerObject(PhysicalMaterials.Domain.BLOCK,block,
                List.of(new PhysicalMaterials.Component(material,1)));
        }
        // BlockItems inherit the registered block compositions via Core.item.
        // Compression, tension and other unavailable independent characteristics remain unknown.
    }
}
