package dev.minemods.worldlogicterrain.growth;

import dev.minemods.worldlogic.api.WorldLogicApi;
import dev.minemods.worldlogicterrain.soil.SoilExhaustion;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.FarmBlock;

/** Optional additive Core revision-11 bridge; no independent calendar, no writes into Core soil state. */
final class CorePlantClimate {
    private static final Method SEASON=method("season"), HYDRO=method("hydrology"), ECOLOGY=method("soilEcology");
    private static Method method(String name){
        try{return WorldLogicApi.class.getMethod(name,ServerLevel.class,BlockPos.class);}
        catch(NoSuchMethodException absent){return null;}
    }
    private static Optional<?> read(Method method,ServerLevel level,BlockPos pos){
        if(method==null)return Optional.empty();
        try{return (Optional<?>)method.invoke(null,level,pos);}
        catch(InvocationTargetException e){throw new IllegalStateException("Core plant API failed: "+method.getName(),e.getCause());}
        catch(ReflectiveOperationException e){throw new IllegalStateException("Core plant API mismatch",e);}
    }
    private static Object field(Object sample,String name){
        try{return sample.getClass().getMethod(name).invoke(sample);}
        catch(ReflectiveOperationException e){throw new IllegalStateException("Core plant sample field "+name,e);}
    }
    private static double number(Object sample,String name){return ((Number)field(sample,name)).doubleValue();}
    static String season(ServerLevel level,BlockPos pos){return read(SEASON,level,pos).map(v->field(v,"kind").toString()).orElse("none");}
    static SeasonalGrowth.Input sample(ServerLevel l,BlockPos pos){
        String season=season(l,pos);
        var env=WorldLogicApi.environment(l,pos);
        double temp=env.map(WorldLogicApi.Environment::ambientCelsiusEstimate).orElseGet(()->-5.0+18.0*l.getBiome(pos).value().getBaseTemperature());
        double moisture=env.flatMap(WorldLogicApi.Environment::regionalClimate).map(WorldLogicApi.RegionalClimate::moistureIndex)
            .orElse(l.getBiome(pos).value().hasPrecipitation()?.55:.20);
        double fertility=.65;
        var ground=l.getBlockState(pos.below());
        if(ground.getBlock() instanceof FarmBlock)moisture=.25+.65*ground.getValue(FarmBlock.MOISTURE)/7.0;
        var hydro=read(HYDRO,l,pos.below());
        if(hydro.isPresent()&&Boolean.TRUE.equals(field(hydro.get(),"simulated"))){
            moisture=number(hydro.get(),"saturation");fertility=number(hydro.get(),"fertility");
        }
        var ecology=read(ECOLOGY,l,pos.below());
        if(ecology.isPresent())fertility=number(ecology.get(),"effectiveFertility");
        return new SeasonalGrowth.Input(season,temp,PlantSpecies.clamp(moisture),PlantSpecies.clamp(fertility),l.getRawBrightness(pos,0),SoilExhaustion.exhausted(ground));
    }
}
