package dev.minemods.riverweir.logic;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.Collection;

/**
 * Lightweight compatibility shim for TerraFirmaCraft flowing river water.
 * No compile-time dependency on TFC's Flow enum or registry classes.
 * Preserves the fluid and directional flow inside every waterlogged weir panel.
 */
public final class WeirWater {
    public static final ResourceLocation RIVER_ID=new ResourceLocation("tfc","river_water");
    private static final String[] FLOW_NAMES={
        "e","nee","ne","nne","n","nnw","nw","nww","w","sww","sw","ssw","s","sse","se","see","none"
    };
    private WeirWater(){}
    public static int kind(FluidState state){
        Fluid fluid=state.getType();
        if(fluid==Fluids.WATER)return 0;
        return RIVER_ID.equals(ForgeRegistries.FLUIDS.getKey(fluid))?1:-1;
    }
    public static int flow(FluidState state){
        for(Property<?> property:state.getProperties()){
            if(property.getName().equals("flow")){
                String current=nameOf(state,property);
                for(int i=0;i<FLOW_NAMES.length;i++)if(FLOW_NAMES[i].equals(current))return i;
            }
        }
        return 16;
    }
    private static <T extends Comparable<T>> String nameOf(FluidState state,Property<T> property){
        return property.getName(state.getValue(property));
    }
    private static <T extends Comparable<T>> FluidState apply(FluidState state,Property<T> property,String value){
        return property.getValue(value).map(v->state.setValue(property,v)).orElse(state);
    }
    public static FluidState fluid(int kind,int flow){
        if(kind!=1)return Fluids.WATER.getSource(false);
        Fluid fromRegistry=ForgeRegistries.FLUIDS.getValue(RIVER_ID);
        if(fromRegistry==null)return Fluids.WATER.getSource(false);
        FluidState state=fromRegistry.defaultFluidState();
        for(Property<?> property:state.getProperties()){
            if(property.getName().equals("flow")){
                state=apply(state,property,FLOW_NAMES[Math.max(0,Math.min(16,flow))]);
                break;
            }
        }
        return state;
    }
}
