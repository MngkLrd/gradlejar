package dev.minemods.worldlogicterrain.tree;

import dev.minemods.worldlogic.api.PhysicalMaterials;
import dev.minemods.worldlogicterrain.api.WoodPhysicsApi;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.server.level.ServerLevel;

public final class WoodPhysicsChecks {
    private WoodPhysicsChecks(){}
    private static void require(boolean value,String reason){if(!value)throw new IllegalStateException(reason);}
    private static ResourceLocation id(String namespace,String name){return ResourceLocation.fromNamespaceAndPath(namespace,name);}
    public static void verify(ServerLevel l){
        require(WoodPhysicsApi.profiles().size()==24,"Expected 24 material profiles");
        int registered=0;
        for(var entry:WoodPhysicsApi.profiles().entrySet()){
            var species=entry.getKey();var profile=entry.getValue();
            String name=species.getPath(),ns=species.getNamespace();
            String raw=name.equals("bamboo")?"bamboo_block":name.equals("crimson")||name.equals("warped")?name+"_stem":name+"_log";
            for(String path:new String[]{raw,name+"_planks"}){
                var key=id(ns,path);
                require(BuiltInRegistries.BLOCK.containsKey(key),"Missing registered material "+key);
                var b=BuiltInRegistries.BLOCK.get(key);
                var state=b.defaultBlockState();
                var core=PhysicalMaterials.block(state);
                require(core.components().size()==1,"Core composition "+key);
                require(core.components().getFirst().materialId().equals(id(ns.equals("minecraft")?"worldlogic":ns,name)),"Core material "+key);
                if(ns.equals("worldlogicterrain"))require(core.evidence()==PhysicalMaterials.Evidence.EXPLICIT,"Addon registration "+key);
                require(WoodPhysicsApi.profile(state).orElseThrow().equals(profile),"Facade identity "+key);
                require(PhysicalMaterials.item(new ItemStack(b.asItem())).components().equals(core.components()),"BlockItem composition "+key);
                registered++;
            }
            if(ns.equals("minecraft"))require(WoodPhysicsApi.profile(id(ns,"stripped_"+raw)).orElseThrow().equals(profile),"Stripped identity "+species);
            var dry=WoodPhysicsApi.atMoisture(profile,0);
            var wet=WoodPhysicsApi.atMoisture(profile,100);
            var m=PhysicalMaterials.material(id(ns.equals("minecraft")?"worldlogic":ns,name)).orElseThrow();
            var expected=PhysicalMaterials.componentProperties(new PhysicalMaterials.Component(m.id(),1),new PhysicalMaterials.State(20,1)).orElseThrow();
            require(dry.densityKgM3()==m.dryDensityKgM3()&&wet.densityKgM3()==expected.massKg(),"Core SI mass "+species);
            require(wet.densityKgM3()>dry.densityKgM3()&&wet.massKg(.5)==expected.massKg()*.5,"Wet mass conversion "+species);
            require(Double.compare(dry.hardnessN(),PhysicalMaterials.woodHardnessKN(m.id()).orElse(Double.NaN)*1000)==0,"Janka units "+species);
            require(Double.isNaN(dry.bendingStrengthMPa())&&Double.isNaN(profile.greenMoisturePercent()),"Unknown characteristics "+species);
            require(dry.flammability()==m.flammability(),"Nominal fire "+species);
        }
        require(registered==48,"Registered coverage");
        require(WoodPhysicsApi.profile(Blocks.STONE.defaultBlockState()).isEmpty(),"Stone misclassified");
        require(WoodPhysicsApi.profile(id("other","oak_log")).isEmpty(),"Unknown block misclassified");
        for(String n:new String[]{"crimson_stem","warped_stem"})
            require(WoodPhysicsApi.atMoisture(WoodPhysicsApi.profile(id("minecraft",n)).orElseThrow(),0).flammability()==0,"Nether fire immunity");
        var pos=new BlockPos(8,225,8);
        require(l.hasChunkAt(pos),"Fixture not loaded");
        var before=l.getBlockState(pos);var neighbor=l.getBlockState(pos.east());
        try{
            l.setBlock(pos,Blocks.OAK_PLANKS.defaultBlockState(),2);
            l.setBlock(pos.east(),Blocks.AIR.defaultBlockState(),2);
            var seasoned=WoodPhysicsApi.sample(l,pos).orElseThrow();
            l.setBlock(pos.east(),Blocks.WATER.defaultBlockState(),2);
            var damp=WoodPhysicsApi.sample(l,pos).orElseThrow();
            require(damp.moisturePercent()>seasoned.moisturePercent()&&damp.densityKgM3()>seasoned.densityKgM3(),"Core wet world fixture");
            l.setBlock(pos,Blocks.STONE.defaultBlockState(),2);
            require(WoodPhysicsApi.sample(l,pos).isEmpty(),"Unknown world block");
        }finally{l.setBlock(pos,before,2);l.setBlock(pos.east(),neighbor,2);}
    }
}
