package dev.minemods.worldlogicterrain.tree;

import dev.minemods.worldlogic.api.WorldLogicApi;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.block.state.BlockState;

/** Optional additive RC26 contract. No duplicate core classes and no live-server API on worldgen threads. */
public final class CoreHabitat {
    public record Sample(double temperature,double moisture,double fertility,double dryness,double waterlogging,
        double coldness,double strength,String zone,String surface,double slope,double wind,
        double windX,double windZ,double riverbank) {}
    public interface Batch { Optional<Sample> sample(BlockPos pos,Holder<Biome> biome,BlockState ground); }
    private record Methods(Method factory,Method sample,Method[] fields) {}
    private static final Methods API=discover();
    private static Methods discover() {
        Method factory;
        try { factory=WorldLogicApi.class.getMethod("worldgenSampler",WorldGenLevel.class,ChunkGenerator.class); }
        catch(NoSuchMethodException oldCore){return null;}
        try{
            Class<?> sampler=Class.forName("dev.minemods.worldlogic.api.WorldLogicApi$WorldgenSampler");
            Class<?> env=Class.forName("dev.minemods.worldlogic.api.WorldLogicApi$WorldgenEnvironment");
            String[] names={"temperatureCelsius","moisture","fertility","dryness","waterlogging","coldness","environmentStrength","zone","surfaceFamily","slopeDegrees","windExposure","prevailingWindX","prevailingWindZ","riverbankPotential"};
            Method[] fields=new Method[names.length];
            for(int i=0;i<names.length;i++)fields[i]=env.getMethod(names[i]);
            return new Methods(factory,sampler.getMethod("sample",BlockPos.class,Holder.class,BlockState.class),fields);
        }catch(ReflectiveOperationException e){throw new IllegalStateException("Unsupported World Logic worldgen API revision",e);}
    }
    static boolean available(){return API!=null;}
    public static Batch batch(WorldGenLevel level,ChunkGenerator generator) {
        if(API==null)return (p,b,g)->Optional.empty();
        Optional<?> value=(Optional<?>)invoke(API.factory(),null,level,generator);
        if(value.isEmpty())return (p,b,g)->Optional.empty();
        Object sampler=value.orElseThrow();
        return (p,b,g)->{
            Object env=invoke(API.sample(),sampler,p,b,g);
            Object[] f=new Object[API.fields().length];
            for(int i=0;i<f.length;i++)f[i]=invoke(API.fields()[i],env);
            return Optional.of(new Sample(n(f,0),n(f,1),n(f,2),n(f,3),n(f,4),n(f,5),n(f,6),f[7].toString(),f[8].toString(),n(f,9),n(f,10),n(f,11),n(f,12),n(f,13)));
        };
    }
    private static double n(Object[] f,int i){return ((Number)f[i]).doubleValue();}
    private static Object invoke(Method method,Object receiver,Object... args) {
        try{return method.invoke(receiver,args);}
        catch(InvocationTargetException e){throw new IllegalStateException("Core habitat sampling failed",e.getCause());}
        catch(ReflectiveOperationException e){throw new IllegalStateException("Core habitat API mismatch",e);}
    }
}
