package dev.minemods.worldlogicterrain.tree;

import dev.minemods.worldlogicterrain.api.WoodPhysicsApi;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class WoodPhysicsChecks {
    private WoodPhysicsChecks(){}
    private static void require(boolean value,String reason){if(!value)throw new IllegalStateException(reason);}
    private static ResourceLocation id(String namespace,String name){return ResourceLocation.fromNamespaceAndPath(namespace,name);}
    public static void register(RegisterCommandsEvent e){
        e.getDispatcher().register(Commands.literal("worldlogicterrain").requires(s->s.hasPermission(2))
          .then(Commands.literal("wood_check").executes(c->{
            try{
                require(WoodPhysicsApi.profiles().size()==24,"Expected 24 material profiles");
                int registered=0;
                for(var entry:WoodPhysicsApi.profiles().entrySet()){
                    var species=entry.getKey();var profile=entry.getValue();
                    String name=species.getPath(),ns=species.getNamespace();
                    String raw=name.equals("bamboo")?"bamboo_block":name.equals("crimson")||name.equals("warped")?name+"_stem":name+"_log";
                    for(String path:new String[]{raw,name+"_planks"}){
                        var key=id(ns,path);
                        require(BuiltInRegistries.BLOCK.containsKey(key),"Missing registered material "+key);
                        require(WoodPhysicsApi.profile(BuiltInRegistries.BLOCK.get(key).defaultBlockState()).orElseThrow().equals(profile),"Wrong profile "+key);
                        registered++;
                    }
                    if(ns.equals("minecraft"))require(WoodPhysicsApi.profile(id(ns,"stripped_"+raw)).orElseThrow().equals(profile),"Stripped identity "+species);
                    var dry=WoodPhysicsApi.atMoisture(profile,12);
                    var wet=WoodPhysicsApi.atMoisture(profile,100);
                    require(dry.densityKgM3()>0&&dry.hardnessN()>0&&dry.bendingStrengthMPa()>0,"Invalid base "+species);
                    require(wet.densityKgM3()>dry.densityKgM3()&&wet.hardnessN()<dry.hardnessN()&&wet.bendingStrengthMPa()<dry.bendingStrengthMPa(),"Wet response "+species);
                    require(wet.flammability()==0&&dry.flammability()>=0&&dry.flammability()<=1,"Fire response "+species);
                    require(dry.massKg(.5)==dry.densityKgM3()*.5,"Mass conversion "+species);
                }
                require(registered==48,"Registered coverage");
                require(WoodPhysicsApi.profile(Blocks.STONE.defaultBlockState()).isEmpty(),"Stone misclassified");
                require(WoodPhysicsApi.profile(id("other","oak_log")).isEmpty(),"Foreign namespace misclassified");
                var ebony=WoodPhysicsApi.profile(id("worldlogicterrain","ebony_log")).orElseThrow();
                var willow=WoodPhysicsApi.profile(id("worldlogicterrain","willow_log")).orElseThrow();
                require(ebony.densityKgM3()>willow.densityKgM3()&&ebony.hardnessN()>willow.hardnessN()&&ebony.bendingStrengthMPa()>willow.bendingStrengthMPa(),"Species ordering");
                for(String n:new String[]{"crimson_stem","warped_stem"})
                    require(WoodPhysicsApi.atMoisture(WoodPhysicsApi.profile(id("minecraft",n)).orElseThrow(),0).flammability()==0,"Nether fire immunity");
                var l=c.getSource().getLevel();
                var pos=new BlockPos(8,225,8);
                require(l.hasChunkAt(pos),"Fixture not loaded");
                var before=l.getBlockState(pos);var neighbor=l.getBlockState(pos.east());
                try{
                    l.setBlock(pos,Blocks.OAK_PLANKS.defaultBlockState(),2);
                    l.setBlock(pos.east(),Blocks.AIR.defaultBlockState(),2);
                    var seasoned=WoodPhysicsApi.sample(l,pos).orElseThrow();
                    l.setBlock(pos.east(),Blocks.WATER.defaultBlockState(),2);
                    var damp=WoodPhysicsApi.sample(l,pos).orElseThrow();
                    require(damp.moisturePercent()>seasoned.moisturePercent()&&damp.densityKgM3()>seasoned.densityKgM3()&&damp.flammability()<seasoned.flammability(),"Wet world fixture");
                    l.setBlock(pos,Blocks.STONE.defaultBlockState(),2);
                    require(WoodPhysicsApi.sample(l,pos).isEmpty(),"Unknown world block");
                }finally{l.setBlock(pos,before,2);l.setBlock(pos.east(),neighbor,2);}
                c.getSource().sendSuccess(()->Component.literal("Wood physics verified: 24 profiles, 48 registered log/plank blocks, stripped identity, moisture, mass, hardness, bending, fire immunity and wet fixture"),false);
                return 1;
            }catch(RuntimeException ex){c.getSource().sendFailure(Component.literal("Wood physics failed: "+ex));return 0;}
          })));
    }
}
