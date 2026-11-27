package dev.minemods.worldlogicterrain.water;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.core.registries.BuiltInRegistries;

/** Actual registered fluid identity, or explicitly provisional biome hint before core conversion. */
public final class AquaticHabitat {
    public enum Kind { SEA,BRACKISH,RIVER,LAKE,POND,SPRING,WETLAND,GROUNDWATER,FRESH_HINT,UNKNOWN,NONE }
    private AquaticHabitat(){}
    public static boolean water(BlockState state){
        return state.getBlock() instanceof LiquidBlock && state.getFluidState().is(FluidTags.WATER);
    }
    private static boolean tagged(Holder<Biome> b,String ns,String name){
        return b.is(TagKey.create(Registries.BIOME,ResourceLocation.fromNamespaceAndPath(ns,name)));
    }
    public static Kind intrinsic(BlockState state){
        if(!water(state))return Kind.NONE;
        Fluid fluid=state.getFluidState().getType();
        var id=BuiltInRegistries.FLUID.getKey(fluid);
        if(!id.getNamespace().equals("worldlogic"))return Kind.UNKNOWN;
        return switch(id.getPath().replaceFirst("^flowing_","")){
            case "sea_water" -> Kind.SEA;
            case "brackish_water" -> Kind.BRACKISH;
            case "river_water" -> Kind.RIVER;
            case "lake_water" -> Kind.LAKE;
            case "pond_water" -> Kind.POND;
            case "spring_water" -> Kind.SPRING;
            case "wetland_water" -> Kind.WETLAND;
            case "groundwater" -> Kind.GROUNDWATER;
            default -> Kind.UNKNOWN;
        };
    }
    public static Kind hint(BlockState state,Holder<Biome> biome){
        Kind k=intrinsic(state);
        if(k!=Kind.UNKNOWN)return k;
        if(!state.is(net.minecraft.world.level.block.Blocks.WATER))return Kind.UNKNOWN;
        if(tagged(biome,"minecraft","is_ocean")||tagged(biome,"worldlogic","water/marine"))return Kind.SEA;
        if(tagged(biome,"worldlogic","water/estuary")||tagged(biome,"worldlogicterrain","salt_coast"))return Kind.BRACKISH;
        if(tagged(biome,"minecraft","is_river")||tagged(biome,"worldlogic","water/river"))return Kind.RIVER;
        if(tagged(biome,"worldlogic","water/wetland")||tagged(biome,"worldlogicterrain","fresh_marsh"))return Kind.WETLAND;
        return Kind.FRESH_HINT;
    }
    // RC31 additive method; reflection keeps the RC20 compile/runtime baseline.
    public static final class Prediction {
        private final Object sampler;
        private final java.lang.reflect.Method method;
        private Prediction(Object sampler,java.lang.reflect.Method method){this.sampler=sampler;this.method=method;}
        public boolean available(){return sampler!=null;}
        public Kind classify(net.minecraft.world.level.WorldGenLevel level,BlockPos waterPos,BlockState waterState){
            Kind exact=intrinsic(waterState);
            if(exact!=Kind.UNKNOWN||!waterState.is(net.minecraft.world.level.block.Blocks.WATER))return exact;
            if(!available())return hint(waterState,level.getBiome(waterPos));
            int x=waterPos.getX(),z=waterPos.getZ();
            int floor=level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.OCEAN_FLOOR_WG,x,z);
            int top=Math.min(level.getMaxBuildHeight()-1,Math.max(waterPos.getY(),level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE_WG,x,z)-1));
            BlockPos surface=new BlockPos(x,top,z);
            BlockPos bed=new BlockPos(x,Math.max(level.getMinBuildHeight(),Math.min(waterPos.getY()-1,floor)),z);
            if(!level.ensureCanWrite(surface)||!level.ensureCanWrite(bed))return Kind.UNKNOWN;
            return predict(waterPos,surface,floor,level.getBiome(surface),level.getBlockState(bed));
        }
        Kind predict(BlockPos waterPos,BlockPos surface,int floor,Holder<Biome> biome,BlockState bed){
            try{
                Object profile=method.invoke(sampler,waterPos,surface,floor,biome,bed);
                return Kind.valueOf(profile.getClass().getMethod("kind").invoke(profile).toString());
            }catch(ReflectiveOperationException e){throw new IllegalStateException("World Logic RC31 water prediction failed",e);}
        }
    }
    public static Prediction prediction(net.minecraft.world.level.WorldGenLevel level,net.minecraft.world.level.chunk.ChunkGenerator generator){
        try{
            Class<?> api=dev.minemods.worldlogic.api.WorldLogicApi.class;
            var factory=api.getMethod("worldgenSampler",net.minecraft.world.level.WorldGenLevel.class,net.minecraft.world.level.chunk.ChunkGenerator.class);
            Class<?> type=Class.forName(api.getName()+"$WorldgenSampler");
            var method=type.getMethod("predictWater",BlockPos.class,BlockPos.class,int.class,Holder.class,BlockState.class);
            var sampler=(java.util.Optional<?>)factory.invoke(null,level,generator);
            return new Prediction(sampler.orElse(null),method);
        }catch(NoSuchMethodException|ClassNotFoundException oldCore){return new Prediction(null,null);}
        catch(ReflectiveOperationException e){throw new IllegalStateException("World Logic water predictor initialization failed",e);}
    }
    public static boolean floating(Kind k){
        return k==Kind.LAKE||k==Kind.POND||k==Kind.WETLAND||k==Kind.FRESH_HINT;
    }
    public static boolean saline(Kind k){return k==Kind.SEA||k==Kind.BRACKISH;}
    public static boolean supportsFloating(BlockState state){
        if(!water(state)||!state.getFluidState().isSource())return false;
        Kind k=intrinsic(state);
        return floating(k)||(k==Kind.UNKNOWN&&state.is(net.minecraft.world.level.block.Blocks.WATER));
    }
}
