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
        wood("willow",400,0.67);
        wood("larch",590,0.83);
        wood("pine",510,0.88);
        wood("fir",450,0.79);
        wood("cedar",380,0.89);
        wood("maple",705,0.64);
        wood("beech",720,0.65);
        wood("alder",500,0.7);
        wood("palm",550,0.67);
        wood("date_palm",480,0.69);
        wood("baobab",250,0.45);
        wood("ebony",1000,0.43);
        wood("mahogany",590,0.62);
    }
    private static void wood(String name,double densityAtTwelvePercent,double flammability){
        var material=id(name);
        // Existing nominal Terrarian reference density is at 12% moisture.
        // Convert once to Core's dry reference at constant nominal volume.
        PhysicalMaterials.registerMaterial(new PhysicalMaterials.MaterialProfile(material,
            PhysicalMaterials.Phase.SOLID,densityAtTwelvePercent/1.12,N,N,N,N,N,.12,N,N,N,N,flammability,N,N,N));
        for(String suffix:new String[]{"_log","_planks"}){
            var block=id(name+suffix);
            PhysicalMaterials.registerObject(PhysicalMaterials.Domain.BLOCK,block,
                List.of(new PhysicalMaterials.Component(material,1)));
        }
        // BlockItems inherit the registered block compositions via Core.item.
        // Janka, compression, tension, bending, and other unavailable values are not invented.
    }
}
