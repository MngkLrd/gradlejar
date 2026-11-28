package dev.minemods.worldlogicterrain.growth;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Authoritative, bounded fungal colony ecology. Environmental inputs belong to Core.
 * The persistent colony is the block itself, not a second per-chunk simulation.
 * Public queries are read-only; state transitions are owned by the colony block. */
public final class FungalEcology {
    public static final int API_VERSION=1,API_REVISION=0;
    public static final int LOCAL_CAP=5;
    public enum Species {
        PORCINI("porcini","cool_woodland",false,4,14,30,.35,5),
        CHANTERELLE("chanterelle","temperate_woodland",false,5,17,32,.43,6),
        MOREL("morel","temperate_meadow",false,1,12,28,.25,8),
        OYSTER_MUSHROOM("oyster_mushroom","tree_habitat",true,0,15,30,.30,4);
        public final String id,habitat;
        public final boolean wood;
        public final double minTemp,optimalTemp,maxTemp,minMoisture;
        public final int rarity;
        Species(String id,String habitat,boolean wood,double minTemp,double optimalTemp,double maxTemp,double minMoisture,int rarity){
            this.id=id;this.habitat=habitat;this.wood=wood;this.minTemp=minTemp;this.optimalTemp=optimalTemp;this.maxTemp=maxTemp;this.minMoisture=minMoisture;this.rarity=rarity;
        }
        public ResourceLocation blockId(){return ResourceLocation.fromNamespaceAndPath("worldlogicterrain",id+"_colony");}
        public ResourceLocation itemId(){return ResourceLocation.fromNamespaceAndPath("worldlogicterrain",id);}
        public Block block(){return BuiltInRegistries.BLOCK.get(blockId());}
        public Item item(){return BuiltInRegistries.ITEM.get(itemId());}
        public boolean supports(BlockState floor){
            if(wood)return floor.is(BlockTags.LOGS)&&floor.hasProperty(RotatedPillarBlock.AXIS)
                &&floor.getValue(RotatedPillarBlock.AXIS)!=Direction.Axis.Y;
            return floor.is(BlockTags.DIRT)&&!SoilExhaustion.exhausted(floor);
        }
        public static Optional<Species> byId(String id){for(var s:values())if(s.id.equals(id))return Optional.of(s);return Optional.empty();}
    }
    public record Snapshot(Species species,int age,boolean substrateValid,double temperatureC,double moisture,
                           double growthRate,String status){
        public boolean mature(){return age==2;}
    }
    private FungalEcology(){}
    /** Local ecological fitness, not a separate climate simulation or season gate. */
    public static double rate(Species s,double temperatureC,double moisture,int light){
        if(!Double.isFinite(temperatureC)||!Double.isFinite(moisture)||temperatureC<=s.minTemp||
            temperatureC>=s.maxTemp||moisture<=s.minMoisture)return 0;
        double thermal=PlantSpecies.fit(temperatureC,s.minTemp,s.optimalTemp,s.maxTemp);
        double wet=PlantSpecies.clamp((moisture-s.minMoisture)/(.7-s.minMoisture));
        double shade=s==Species.MOREL?1:light>=13?.55:1;
        return PlantSpecies.clamp(thermal*wet*shade);
    }
    private static double localMoisture(SeasonalGrowth.Input e){return e.moisture();}
    private static String status(Species s,SeasonalGrowth.Input e,double r){
        if(e.temperature()<=s.minTemp)return "cold";
        if(e.temperature()>=s.maxTemp)return "hot";
        if(localMoisture(e)<=s.minMoisture)return "dry";
        return r==0?"dormant":r<.45?"slow":"growing";
    }
    private static Snapshot describe(ServerLevel l,BlockPos pos,BlockState state){
        var b=(FungalColonyBlock)state.getBlock();
        var e=CorePlantClimate.sample(l,pos);
        int age=state.getValue(FungalColonyBlock.AGE);
        boolean valid=state.canSurvive(l,pos);
        double r=valid?rate(b.species(),e.temperature(),localMoisture(e),e.light()):0;
        return new Snapshot(b.species(),age,valid,e.temperature(),localMoisture(e),r,
            !valid?"dormant":age==2?"mature":status(b.species(),e,r));
    }
    public static Optional<Snapshot> snapshot(ServerLevel l,BlockPos pos){
        if(!l.getServer().isSameThread())throw new IllegalStateException("server thread required");
        if(l.isOutsideBuildHeight(pos)||!l.hasChunkAt(pos))return Optional.empty();
        var state=l.getBlockState(pos);
        return state.getBlock() instanceof FungalColonyBlock?Optional.of(describe(l,pos,state)):Optional.empty();
    }
    static SeasonalGrowth.View view(ServerLevel l,BlockPos pos,BlockState state){
        var s=describe(l,pos,state);
        return new SeasonalGrowth.View(s.age()*50,s.mature()?0:(int)Math.round(s.growthRate()*100),-1,s.status(),"all",
            (int)Math.round(s.temperatureC()),(int)Math.round(s.moisture()*100));
    }
    static boolean advance(ServerLevel l,BlockPos pos,SeasonalGrowth.Input e,RandomSource random){
        var state=l.getBlockState(pos);
        if(!(state.getBlock() instanceof FungalColonyBlock b)||state.getValue(FungalColonyBlock.AGE)>=2||
            !state.canSurvive(l,pos)||random.nextDouble()>=rate(b.species(),e.temperature(),localMoisture(e),e.light()))return false;
        return l.setBlock(pos,state.setValue(FungalColonyBlock.AGE,state.getValue(FungalColonyBlock.AGE)+1),Block.UPDATE_ALL);
    }
    /** One candidate within two blocks, capped local population; never loads a chunk. */
    static boolean colonize(ServerLevel l,BlockPos source,BlockPos target,SeasonalGrowth.Input e,RandomSource random){
        if(!l.hasChunkAt(source)||!l.hasChunkAt(target)||l.isOutsideBuildHeight(target)||target.getY()!=source.getY()
            ||source.distManhattan(target)<1||source.distManhattan(target)>2)return false;
        var state=l.getBlockState(source);
        if(!(state.getBlock() instanceof FungalColonyBlock b)||state.getValue(FungalColonyBlock.AGE)!=2||!state.canSurvive(l,source))return false;
        if(!l.getBlockState(target).isAir()||!l.getFluidState(target).isEmpty())return false;
        var young=state.setValue(FungalColonyBlock.AGE,0);
        if(!young.canSurvive(l,target)||random.nextDouble()>=rate(b.species(),e.temperature(),localMoisture(e),e.light()))return false;
        int count=0;
        for(int dx=-2;dx<=2;dx++)for(int dz=-2;dz<=2;dz++){
            var q=target.offset(dx,0,dz);
            if(l.hasChunkAt(q)&&l.getBlockState(q).getBlock() instanceof FungalColonyBlock&&++count>=LOCAL_CAP)return false;
        }
        return l.setBlock(target,young,Block.UPDATE_ALL);
    }
    static void tick(ServerLevel l,BlockPos pos,RandomSource random){
        var state=l.getBlockState(pos);
        if(!(state.getBlock() instanceof FungalColonyBlock))return;
        if(!state.canSurvive(l,pos)){l.removeBlock(pos,false);return;}
        if(state.getValue(FungalColonyBlock.AGE)<2){
            if(random.nextInt(5)==0)advance(l,pos,CorePlantClimate.sample(l,pos),random);
            return;
        }
        if(random.nextInt(50)!=0)return;
        var d=Direction.Plane.HORIZONTAL.getRandomDirection(random);
        var target=pos.relative(d,1+random.nextInt(2));
        if(l.hasChunkAt(target)&&!l.isOutsideBuildHeight(target))
            colonize(l,pos,target,CorePlantClimate.sample(l,target),random);
    }
}
