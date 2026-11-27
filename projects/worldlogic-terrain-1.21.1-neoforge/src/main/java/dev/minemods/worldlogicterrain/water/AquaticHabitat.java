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
