package dev.minemods.worldlogicterrain.api;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import dev.minemods.worldlogic.api.WorldLogicApi;

/** Read-only, additive API for TreeChopper and other WorldLogic consumers.
 * Reference values are approximate representative material profiles at 12% moisture,
 * not engineering-grade species measurements. Moisture is percent of oven-dry mass.
 * No global block mutation, per-block ticking, or chunk loading. */
public final class WoodPhysicsApi {
    public static final int API_VERSION=1;
    public record Profile(ResourceLocation speciesId,double densityKgM3,double hardnessN,
                          double bendingStrengthMPa,double greenMoisturePercent,double dryFlammability) {}
    public record Sample(Profile profile,double moisturePercent,double densityKgM3,double hardnessN,
                         double bendingStrengthMPa,double flammability) {
        public double massKg(double volumeM3) {
            if(!Double.isFinite(volumeM3)||volumeM3<0)throw new IllegalArgumentException("Invalid volume");
            return densityKgM3*volumeM3;
        }
    }
    private record Material(Profile profile,boolean processed){}
    private static final Map<ResourceLocation,Profile> SPECIES;
    private static final Map<ResourceLocation,Material> BLOCKS;
    static {
        var species=new LinkedHashMap<ResourceLocation,Profile>();
        var blocks=new LinkedHashMap<ResourceLocation,Material>();
        add(species,blocks,"minecraft","oak",710,5900,100,65,0.66);
        add(species,blocks,"minecraft","spruce",430,1800,68,85,0.86);
        add(species,blocks,"minecraft","birch",650,4800,105,72,0.72);
        add(species,blocks,"minecraft","jungle",570,3500,85,90,0.7);
        add(species,blocks,"minecraft","acacia",790,7600,125,55,0.56);
        add(species,blocks,"minecraft","dark_oak",770,6500,110,68,0.59);
        add(species,blocks,"minecraft","mangrove",870,7800,120,95,0.48);
        add(species,blocks,"minecraft","cherry",560,4200,85,62,0.68);
        add(species,blocks,"minecraft","bamboo",680,6000,95,65,0.82);
        add(species,blocks,"minecraft","crimson",760,6500,105,60,0);
        add(species,blocks,"minecraft","warped",630,4400,82,70,0);
        add(species,blocks,"worldlogicterrain","willow",400,1550,50,105,0.67);
        add(species,blocks,"worldlogicterrain","larch",590,3300,90,75,0.83);
        add(species,blocks,"worldlogicterrain","pine",510,2700,80,85,0.88);
        add(species,blocks,"worldlogicterrain","fir",450,2000,72,95,0.79);
        add(species,blocks,"worldlogicterrain","cedar",380,1500,55,80,0.89);
        add(species,blocks,"worldlogicterrain","maple",705,6400,110,66,0.64);
        add(species,blocks,"worldlogicterrain","beech",720,5900,112,68,0.65);
        add(species,blocks,"worldlogicterrain","alder",500,2600,67,85,0.7);
        add(species,blocks,"worldlogicterrain","palm",550,4000,63,90,0.67);
        add(species,blocks,"worldlogicterrain","date_palm",480,2800,52,95,0.69);
        add(species,blocks,"worldlogicterrain","baobab",250,700,25,180,0.45);
        add(species,blocks,"worldlogicterrain","ebony",1000,13000,155,48,0.43);
        add(species,blocks,"worldlogicterrain","mahogany",590,3800,86,65,0.62);
        SPECIES=Map.copyOf(species); BLOCKS=Map.copyOf(blocks);
    }
    private WoodPhysicsApi(){}
    private static void add(Map<ResourceLocation,Profile> species,Map<ResourceLocation,Material> blocks,
                            String ns,String name,double density,double hardness,double bending,double moisture,double fire){
        var id=ResourceLocation.fromNamespaceAndPath(ns,name);
        var p=new Profile(id,density,hardness,bending,moisture,fire);
        species.put(id,p);
        boolean nether=ns.equals("minecraft")&&(name.equals("crimson")||name.equals("warped"));
        boolean bamboo=ns.equals("minecraft")&&name.equals("bamboo");
        String[] raw=nether?new String[]{name+"_stem",name+"_hyphae"}:
            bamboo?new String[]{"bamboo_block","bamboo"}:ns.equals("minecraft")?new String[]{name+"_log",name+"_wood"}:new String[]{name+"_log"};
        for(String path:raw)blocks.put(ResourceLocation.fromNamespaceAndPath(ns,path),new Material(p,false));
        if(ns.equals("minecraft"))for(String path:raw)if(!path.equals("bamboo"))
            blocks.put(ResourceLocation.fromNamespaceAndPath(ns,"stripped_"+path),new Material(p,false));
        String[] processed=ns.equals("minecraft")?new String[]{name+"_planks",name+"_stairs",name+"_slab",name+"_fence",name+"_fence_gate",name+"_door",name+"_trapdoor",name+"_button",name+"_pressure_plate",name+"_sign",name+"_wall_sign",name+"_hanging_sign",name+"_wall_hanging_sign"}:new String[]{name+"_planks"};
        for(String path:processed)blocks.put(ResourceLocation.fromNamespaceAndPath(ns,path),new Material(p,true));
        if(bamboo)for(String path:new String[]{"bamboo_mosaic","bamboo_mosaic_stairs","bamboo_mosaic_slab"})
            blocks.put(ResourceLocation.fromNamespaceAndPath(ns,path),new Material(p,true));
    }
    public static Map<ResourceLocation,Profile> profiles(){return SPECIES;}
    public static Optional<Profile> profile(ResourceLocation blockId){
        var m=BLOCKS.get(blockId);return m==null?Optional.empty():Optional.of(m.profile());
    }
    public static Optional<Profile> profile(BlockState state){return profile(BuiltInRegistries.BLOCK.getKey(state.getBlock()));}
    /** Pure calculation: density at constant reference volume, moisture softening below fiber saturation.
     * Flammability is a relative gameplay coefficient, not an ignition temperature. */
    public static Sample atMoisture(Profile p,double moisturePercent){
        if(!Double.isFinite(moisturePercent))throw new IllegalArgumentException("Non-finite moisture");
        double m=Math.max(0,Math.min(250,moisturePercent));
        double saturation=Math.min(30,m);
        double hardnessFactor=Math.max(.55,1-.015*(saturation-12));
        double strengthFactor=Math.max(.55,1-.020*(saturation-12));
        double fire=p.dryFlammability()*Math.max(0,Math.min(1,1-(m-12)/75));
        return new Sample(p,m,p.densityKgM3()*(100+m)/112,p.hardnessN()*hardnessFactor,
            p.bendingStrengthMPa()*strengthFactor,fire);
    }
    /** Server-thread query; empty for unloaded positions or unrecognized blocks.
     * Current wetness is an environmental estimate, not a persisted drying simulation.
     * Logs (including stripped logs) are assumed green; manufactured pieces are assumed seasoned. */
    public static Optional<Sample> sample(ServerLevel level,BlockPos pos){
        if(!level.hasChunkAt(pos))return Optional.empty();
        var state=level.getBlockState(pos);
        var m=BLOCKS.get(BuiltInRegistries.BLOCK.getKey(state.getBlock()));
        if(m==null)return Optional.empty();
        double regional=WorldLogicApi.environment(level,pos).flatMap(WorldLogicApi.Environment::regionalClimate)
            .map(WorldLogicApi.RegionalClimate::moistureIndex).orElse(level.getBiome(pos).value().hasPrecipitation()?.55:.20);
        regional=Double.isFinite(regional)?Math.max(0,Math.min(1,regional)):.5;
        double moisture=m.processed()?8+12*regional:m.profile().greenMoisturePercent()*(.8+.4*regional);
        if(level.isRainingAt(pos.above()))moisture+=m.processed()?8:12;
        for(Direction d:Direction.values()){
            var q=pos.relative(d);
            if(level.hasChunkAt(q)&&!level.getFluidState(q).isEmpty()
               &&level.getFluidState(q).is(net.minecraft.tags.FluidTags.WATER)){
                moisture=Math.max(moisture,m.processed()?45:100);break;
            }
        }
        return Optional.of(atMoisture(m.profile(),moisture));
    }
}
