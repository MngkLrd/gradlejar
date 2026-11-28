package dev.minemods.treechopper;

import java.lang.reflect.*;
import java.util.*;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.fml.ModList;

/** Optional public contracts only. SI data are never synthesized from legacy weight coefficients. */
final class MaterialPhysics {
    private static final Logger LOG=LogUtils.getLogger();
    private static final Set<String> WARNED=new HashSet<>();
    private static boolean initialized;
    private static Core core;
    private static Wood wood;
    private static CanonicalWood canonical;
    private record CanonicalWood(Method properties,Method atMoisture,Method green,Method janka,Method density){}
    private record Core(Method block,Method placed,Method evaluate,Constructor<?> state,Method reference,
                        Method components,Method evidence,Method volume,Method materialId,
                        Method mass,Method hardness){}
    private record Wood(Method profile,Method sample,Method atMoisture,Method green,Method moisture,
                        Method hardness,Method mass){}
    record Data(double volumeM3,double massKg,double hardnessN,double moisturePercent,String source){
        boolean known(){return positive(volumeM3)&&positive(massKg);}
    }
    private MaterialPhysics(){}
    private static boolean positive(double v){return Double.isFinite(v)&&v>0;}
    private static double number(Object v){return ((Number)v).doubleValue();}
    private static void warn(String key,Throwable e){
        if(WARNED.add(key))LOG.warn("TreeChopper physical material contract unavailable: {}",key,e);
    }
    private static void init(){
        if(initialized)return;
        initialized=true;
        if(ModList.get().isLoaded("worldlogic"))try{
            Class<?> api=Class.forName("dev.minemods.worldlogic.api.WorldLogicApi");
            Class<?> p=Class.forName("dev.minemods.worldlogic.api.PhysicalMaterials");
            int revision=p.getField("REVISION").getInt(null), apiRevision=api.getField("API_REVISION").getInt(null);
            if(api.getField("API_VERSION").getInt(null)!=1||apiRevision<14||revision<1||revision>2
                ||(revision==2&&apiRevision<15))throw new IllegalStateException("Expected Core API 1 rev14+/PhysicalMaterials rev1 or API rev15+/PhysicalMaterials rev2");
            Class<?> object=Class.forName(p.getName()+"$ObjectProfile"),state=Class.forName(p.getName()+"$State");
            Class<?> component=Class.forName(p.getName()+"$Component"),result=Class.forName(p.getName()+"$Result");
            core=new Core(p.getMethod("block",BlockState.class),p.getMethod("placed",ServerLevel.class,BlockPos.class),
                p.getMethod("evaluate",object,state),state.getConstructor(double.class,double.class),state.getMethod("reference"),
                object.getMethod("components"),object.getMethod("evidence"),object.getMethod("volumeM3"),component.getMethod("materialId"),
                result.getMethod("massKg"),p.getMethod("woodHardnessKN",ResourceLocation.class));
            if(revision==2){
                Class<?> properties=Class.forName(p.getName()+"$WoodProperties"), sample=Class.forName(p.getName()+"$WoodSample");
                canonical=new CanonicalWood(p.getMethod("woodProperties",ResourceLocation.class),p.getMethod("woodAtMoisture",ResourceLocation.class,double.class),
                    properties.getMethod("greenMoistureKgKg"),sample.getMethod("jankaKN"),sample.getMethod("densityKgM3"));
            }
        }catch(ReflectiveOperationException|LinkageError|RuntimeException e){core=null;canonical=null;warn("Core PhysicalMaterials",e);}
        if(ModList.get().isLoaded("worldlogicterrain"))try{
            Class<?> p=Class.forName("dev.minemods.worldlogicterrain.api.WoodPhysicsApi");
            if(p.getField("API_VERSION").getInt(null)!=1)throw new IllegalStateException("Expected WoodPhysicsApi v1");
            Class<?> profile=Class.forName(p.getName()+"$Profile"),sample=Class.forName(p.getName()+"$Sample");
            wood=new Wood(p.getMethod("profile",BlockState.class),p.getMethod("sample",ServerLevel.class,BlockPos.class),
                p.getMethod("atMoisture",profile,double.class),profile.getMethod("greenMoisturePercent"),
                sample.getMethod("moisturePercent"),sample.getMethod("hardnessN"),sample.getMethod("massKg",double.class));
        }catch(ReflectiveOperationException|LinkageError|RuntimeException e){warn("Terrarian WoodPhysicsApi",e);}
    }
    static boolean coreAvailable(){init();return core!=null;}
    static boolean woodAvailable(){init();return wood!=null;}
    static boolean canonicalAvailable(){init();return canonical!=null;}
    private static Object woodSample(ServerLevel l,BlockPos pos,BlockState original,double saved) throws ReflectiveOperationException{
        if(wood==null)return null;
        if(Double.isFinite(saved)){
            var profile=(Optional<?>)wood.profile.invoke(null,original);
            return profile.isPresent()?wood.atMoisture.invoke(null,profile.get(),saved):null;
        }
        if(l.hasChunkAt(pos)&&l.getBlockState(pos).getBlock()==original.getBlock()){
            var sample=(Optional<?>)wood.sample.invoke(null,l,pos);
            if(sample.isPresent())return sample.get();
        }
        // Old cut blocks lack a sample; use the provider's green reference, not a local moisture table.
        var profile=(Optional<?>)wood.profile.invoke(null,original);
        return profile.isPresent()?wood.atMoisture.invoke(null,profile.get(),number(wood.green.invoke(profile.get()))):null;
    }
    static Data read(ServerLevel l,BlockPos pos,BlockState original,boolean log,double saved){
        init();
        double v=Double.NaN,mass=Double.NaN,hardness=Double.NaN,moisture=saved;
        Object sample=null;
        if(log&&wood!=null)try{
            sample=woodSample(l,pos,original,saved);
            if(sample!=null){moisture=number(wood.moisture.invoke(sample));if(canonical==null)hardness=number(wood.hardness.invoke(sample));}
        }catch(ReflectiveOperationException|LinkageError|RuntimeException e){warn("WoodPhysicsApi sample",e);}
        String source="unknown";
        if(core!=null)try{
            Object object=core.block.invoke(null,original);
            v=number(core.volume.invoke(object));
            String evidence=core.evidence.invoke(object).toString();
            List<?> parts=(List<?>)core.components.invoke(object);
            ResourceLocation material=parts.size()==1?(ResourceLocation)core.materialId.invoke(parts.get(0)):null;
            // Exact registrations always win. A generic tag estimate is not species-specific.
            boolean generic=material==null||material.getPath().equals("generic_wood");
            boolean specific=!evidence.equals("UNKNOWN")&&(evidence.equals("EXPLICIT")||!generic||!log);
            if(log&&canonical!=null){
                // RC54 owns all physical wood properties. Terrarian supplies local moisture only.
                // Unregistered custom wood remains unknown rather than using a conflicting species table.
                if(material==null||generic||evidence.equals("UNKNOWN"))return new Data(v,Double.NaN,Double.NaN,moisture,"unknown");
                var properties=(Optional<?>)canonical.properties.invoke(null,material);
                if(properties.isEmpty())return new Data(v,Double.NaN,Double.NaN,moisture,"unknown");
                if(!Double.isFinite(moisture))moisture=number(canonical.green.invoke(properties.get()))*100d;
                var effective=(Optional<?>)canonical.atMoisture.invoke(null,material,moisture/100d);
                if(effective.isEmpty())return new Data(v,Double.NaN,Double.NaN,moisture,"unknown");
                double density=number(canonical.density.invoke(effective.get()));
                double janka=number(canonical.janka.invoke(effective.get()));
                // Volume is Core occupied geometry; no local moisture/density response.
                return new Data(v,positive(v)&&positive(density)?v*density:Double.NaN,positive(janka)?janka*1000d:Double.NaN,moisture,"core");
            }
            if(log&&!positive(hardness)&&material!=null){
                OptionalDouble h=(OptionalDouble)core.hardness.invoke(null,material);
                if(h.isPresent())hardness=h.getAsDouble()*1000; // kN -> N
            }
            if(specific||sample==null){
                Object result=null;
                if(Double.isFinite(moisture)&&log){
                    // Terrarian uses percent of dry mass, Core kg water/kg dry material.
                    result=core.evaluate.invoke(null,object,core.state.newInstance(20d,Math.min(5,Math.max(0,moisture/100d))));
                }else if(l.hasChunkAt(pos)&&l.getBlockState(pos).getBlock()==original.getBlock()){
                    result=((Optional<?>)core.placed.invoke(null,l,pos)).orElse(null);
                }
                if(result==null)result=core.evaluate.invoke(null,object,core.reference.invoke(null));
                mass=number(core.mass.invoke(result));if(positive(mass))source="core";
            }
        }catch(ReflectiveOperationException|LinkageError|RuntimeException e){warn("PhysicalMaterials read",e);}
        // Core may only know generic_wood/unknown for a new species. Terrarian owns that profile.
        // Full uncut log geometry is one nominal cubic metre if Core has not mapped it yet.
        if(log&&canonical==null&&sample!=null&&!positive(mass))try{
            if(!positive(v))v=1d;
            mass=number(wood.mass.invoke(sample,v));if(positive(mass))source="terrarian";
        }catch(ReflectiveOperationException|LinkageError|RuntimeException e){warn("WoodPhysicsApi massKg",e);}
        if(!positive(mass))mass=Double.NaN;
        if(!positive(v))v=Double.NaN;
        if(!positive(hardness))hardness=Double.NaN;
        return new Data(v,mass,hardness,moisture,source);
    }
    /** Canonical oak reference for gameplay normalization, not a hardcoded physical value. */
    static double oakMassKg(){
        init();if(core==null)return Double.NaN;
        try{
            Object object=core.block.invoke(null,Blocks.OAK_LOG.defaultBlockState());
            return number(core.mass.invoke(core.evaluate.invoke(null,object,core.reference.invoke(null))));
        }catch(ReflectiveOperationException|LinkageError|RuntimeException e){warn("Core oak reference mass",e);return Double.NaN;}
    }
    static double oakHardnessN(){
        init();if(core==null)return Double.NaN;
        try{
            OptionalDouble v=(OptionalDouble)core.hardness.invoke(null,ResourceLocation.fromNamespaceAndPath("worldlogic","oak"));
            return v.isPresent()?v.getAsDouble()*1000:Double.NaN;
        }catch(ReflectiveOperationException|LinkageError|RuntimeException e){warn("Core oak reference hardness",e);return Double.NaN;}
    }
    static double resistance(double hardnessN,double referenceN){
        return positive(hardnessN)&&positive(referenceN)?Math.max(.25,Math.min(3,hardnessN/referenceN)):1;
    }
    static double equivalentLogs(double kg,double oakKg,int fallback){
        return positive(kg)&&positive(oakKg)?kg/oakKg:Math.max(1,fallback);
    }
}
