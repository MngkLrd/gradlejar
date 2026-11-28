package dev.minemods.worldlogicterrain.water;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.Direction;

/** Habitat and morphology, independent of Core's authoritative water classifier. */
public enum BenthicSpecies {
    EELGRASS("eelgrass","SEA BRACKISH",-0.4,1.55,1,16,"SOFT",3,16,7),
    SEA_LETTUCE("sea_lettuce","SEA BRACKISH",0.1,2.1,1,10,"ANY",1,15,0),
    SARGASSUM("sargassum","SEA",0.65,2.1,2,20,"ROCK",4,9,10),
    GIANT_KELP("giant_kelp","SEA",-0.15,0.9,3,28,"ROCK",6,13,7),
    RED_DULSE("red_dulse","SEA",-0.4,1.2,2,22,"ROCK",2,12,11),
    CORALLINE_ALGAE("coralline_algae","SEA",0.1,2.1,2,30,"ROCK",1,12,0),
    RED_SEA_STAR("red_sea_star","SEA",-0.3,2.1,1,35,"ANY",1,7,0),
    BLUE_SEA_STAR("blue_sea_star","SEA",0.75,2.1,2,25,"ANY",1,3,0),
    SEA_URCHIN("sea_urchin","SEA",-0.35,2.1,2,38,"ROCK",1,6,0),
    BARREL_SPONGE("barrel_sponge","SEA",0.7,2.1,4,36,"ROCK",1,5,0),
    SEA_ANEMONE("sea_anemone","SEA",0.45,2.1,1,22,"ROCK",1,7,0),
    SEA_FAN("sea_fan","SEA",0.7,2.1,3,32,"ROCK",1,5,0),
    MUSSEL_BED("mussel_bed","SEA BRACKISH",-0.35,1.7,1,18,"ROCK",1,10,0),
    WIDGEON_GRASS("widgeon_grass","BRACKISH LAKE POND",0.1,1.9,1,9,"SOFT",3,14,9),
    WATER_MILFOIL("water_milfoil","LAKE POND WETLAND FRESH_HINT",0.05,1.6,1,12,"SOFT",4,17,8),
    HORNWORT("hornwort","LAKE POND WETLAND FRESH_HINT",-0.1,1.7,1,12,"ANY",3,16,8),
    PONDWEED("pondweed","LAKE POND RIVER FRESH_HINT",-0.2,1.65,1,10,"SOFT",3,17,7),
    WATER_CROWFOOT("water_crowfoot","RIVER SPRING",-0.15,1.5,1,7,"ANY",2,18,8),
    RIVER_MOSS("river_moss","RIVER SPRING",-0.4,1.65,1,9,"ROCK",1,22,0),
    WATER_STARWORT("water_starwort","RIVER SPRING POND WETLAND",-0.15,1.6,1,5,"SOFT",2,14,8),
    IRISH_MOSS("irish_moss","SEA",-0.35,1.30,1,9,"ROCK",1,12,0);
    public final String id,waters,substrate;
    public final double minTemp,maxTemp;
    public final int minDepth,maxDepth,height,weight,growth;
    BenthicSpecies(String id,String waters,double minTemp,double maxTemp,int minDepth,int maxDepth,String substrate,int height,int weight,int growth){
        this.id=id;this.waters=waters;this.minTemp=minTemp;this.maxTemp=maxTemp;this.minDepth=minDepth;this.maxDepth=maxDepth;this.substrate=substrate;this.height=height;this.weight=weight;this.growth=growth;
    }
    public boolean accepts(AquaticHabitat.Kind kind){return Arrays.asList(waters.split(" ")).contains(kind.name());}
    public boolean climate(double temperature,int depth){return temperature>=minTemp&&temperature<=maxTemp&&depth>=minDepth&&depth<=maxDepth;}
    public boolean supports(BlockGetter l,BlockPos p){
        BlockState s=l.getBlockState(p);
        if(!s.isFaceSturdy(l,p,Direction.UP)||!s.getFluidState().isEmpty())return false;
        boolean soft=s.is(BlockTags.SAND)||s.is(BlockTags.DIRT)||s.is(Blocks.GRAVEL)||s.is(Blocks.CLAY)||s.is(Blocks.MUD)
            ||net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(s.getBlock()).getNamespace().equals("worldlogic")&&
            java.util.Set.of("loam","silt","sandy_loam","peat","clay_soil","silty_loam","sandy_soil").contains(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(s.getBlock()).getPath());
        return substrate.equals("ANY")||substrate.equals("SOFT")&&soft||substrate.equals("ROCK")&&!soft;
    }
}
