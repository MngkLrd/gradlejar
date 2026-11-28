package dev.minemods.worldlogicterrain.growth;

import dev.minemods.worldlogicterrain.block.BerryBushBlock;
import dev.minemods.worldlogicterrain.block.HangingFruitBlock;
import dev.minemods.worldlogicterrain.soil.SoilExhaustion;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/** One evaluated rate feeds actual growth and the server-authoritative HUD. */
public final class SeasonalGrowth {
    public record Input(String season,double temperature,double moisture,double fertility,int light,boolean exhausted){}
    public record Result(String season,String status,double rate,double quality,double temperature,double moisture){}
    public record View(int progress,int speed,int quality,String status,String season,int temperature,int moisture){}
    private SeasonalGrowth(){}
    public static Result evaluate(PlantSpecies.Profile p,Input e,boolean legacyNative){
        double soil=e.exhausted()?.65:1;
        if(legacyNative&&e.season().equals("none"))
            return new Result("none","legacy",soil,-1,e.temperature(),e.moisture());
        double t=p.thermal(e.temperature()),m=p.moisture(e.moisture());
        double quality=PlantSpecies.clamp(Math.sqrt(t*m)*(.45+.55*e.fertility())*soil);
        String status=null;
        if(!p.seasonAllowed(e.season()))status="dormant";
        else if(e.light()<9)status="dark";
        else if(e.temperature()<=p.minTemp())status="cold";
        else if(e.temperature()>=p.maxTemp())status="hot";
        else if(e.moisture()<=p.minMoisture())status="dry";
        else if(e.moisture()>=p.maxMoisture()&&p.maxMoisture()<1)status="wet";
        if(status!=null)return new Result(e.season(),status,0,quality,e.temperature(),e.moisture());
        double rate=PlantSpecies.clamp(t*m*(.5+.5*e.fertility())*soil);
        return new Result(e.season(),rate<.45?"slow":"growing",rate,quality,e.temperature(),e.moisture());
    }
    static Result at(ServerLevel l,BlockPos pos,BlockState state){
        var p=PlantSpecies.profile(state);
        if(p==null)return null;
        return evaluate(p,CorePlantClimate.sample(l,pos),!(state.getBlock() instanceof SeasonalPlantBlock));
    }
    /** Existing vanilla/berry soil gate already applies exhaustion, so do not roll it twice. */
    public static boolean allowSeason(ServerLevel l,BlockPos pos,BlockState state,RandomSource random){
        var p=PlantSpecies.profile(state);if(p==null)return true;
        var e=CorePlantClimate.sample(l,pos);
        if(e.season().equals("none"))return true;
        var withoutSoil=new Input(e.season(),e.temperature(),e.moisture(),e.fertility(),e.light(),false);
        var v=evaluate(p,withoutSoil,true);
        return random.nextDouble()<v.rate();
    }
    private static int percent(double v){return v<0?-1:(int)Math.round(PlantSpecies.clamp(v)*100);}
    public static View view(ServerLevel l,BlockPos pos,BlockState state){
        if(state.getBlock() instanceof HangingFruitBlock){
            int age=state.getValue(HangingFruitBlock.AGE);
            return new View(age*50,age==2?0:100,-1,age==2?"mature":"perennial","all",-999,-1);
        }
        if(state.getBlock() instanceof AttachedStemBlock)return new View(100,0,-1,"mature",CorePlantClimate.season(l,pos),-999,-1);
        var result=at(l,pos,state);if(result==null)return null;
        int progress=-1;
        if(state.getBlock() instanceof SeasonalPlantBlock)progress=state.getValue(SeasonalPlantBlock.AGE)*100/3;
        else if(state.getBlock() instanceof BerryBushBlock)progress=state.getValue(BerryBushBlock.RIPE)?100:0;
        else if(state.hasProperty(BlockStateProperties.AGE_7))progress=state.getValue(BlockStateProperties.AGE_7)*100/7;
        else if(state.hasProperty(BlockStateProperties.AGE_3))progress=state.getValue(BlockStateProperties.AGE_3)*100/3;
        if(progress<0)return null;
        int quality=percent(result.quality());
        if(state.getBlock() instanceof SeasonalPlantBlock&&progress==100)quality=state.getValue(SeasonalPlantBlock.QUALITY)*100/3;
        return new View(progress,progress==100?0:percent(result.rate()),quality,progress==100?"mature":result.status(),result.season(),
            (int)Math.round(result.temperature()),percent(result.moisture()));
    }
}
