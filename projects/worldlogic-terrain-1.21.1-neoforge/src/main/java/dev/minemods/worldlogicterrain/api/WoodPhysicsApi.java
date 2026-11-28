package dev.minemods.worldlogicterrain.api;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import dev.minemods.worldlogic.api.PhysicalMaterials;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

/** Compatibility facade for TreeChopper. No local physical material registry.
 * All mass/density and nominal material properties come from Core revision 14.
 * Legacy bending/green-moisture fields are unknown until a proper Core contract exists.
 * No global block mutation, weight restrictions or carry penalties. */
public final class WoodPhysicsApi {
    public static final int API_VERSION=2;
    private static final Set<String> VANILLA=Set.of("oak","spruce","birch","jungle","acacia","dark_oak","mangrove","cherry","bamboo","crimson","warped");
    public record Profile(ResourceLocation speciesId,double densityKgM3,double hardnessN,
                          double bendingStrengthMPa,double greenMoisturePercent,double dryFlammability) {}
    public record Sample(Profile profile,double moisturePercent,double densityKgM3,double hardnessN,
                         double bendingStrengthMPa,double flammability) {
        public double massKg(double volumeM3){
            if(!Double.isFinite(volumeM3)||volumeM3<0)throw new IllegalArgumentException("Invalid volume");
            return densityKgM3*volumeM3;
        }
    }
    private WoodPhysicsApi(){}
    private static boolean wood(PhysicalMaterials.MaterialProfile p){
        return p.id().getNamespace().equals("worldlogicterrain")||
            p.id().getNamespace().equals("worldlogic")&&VANILLA.contains(p.id().getPath());
    }
    private static ResourceLocation externalId(ResourceLocation coreId){
        return coreId.getNamespace().equals("worldlogic")?
            ResourceLocation.fromNamespaceAndPath("minecraft",coreId.getPath()):coreId;
    }
    private static ResourceLocation coreId(ResourceLocation speciesId){
        return speciesId.getNamespace().equals("minecraft")?
            ResourceLocation.fromNamespaceAndPath("worldlogic",speciesId.getPath()):speciesId;
    }
    private static double hardness(PhysicalMaterials.MaterialProfile p){
        return PhysicalMaterials.woodHardnessKN(p.id()).orElse(Double.NaN)*1000;
    }
    private static Profile adapt(PhysicalMaterials.MaterialProfile p){
        var c=new PhysicalMaterials.Component(p.id(),1);
        double density=PhysicalMaterials.componentProperties(c,PhysicalMaterials.State.reference()).orElseThrow().massKg();
        return new Profile(externalId(p.id()),density,hardness(p),Double.NaN,Double.NaN,p.flammability());
    }
    public static Map<ResourceLocation,Profile> profiles(){
        var result=new LinkedHashMap<ResourceLocation,Profile>();
        PhysicalMaterials.materials().values().stream().filter(WoodPhysicsApi::wood)
            .forEach(p->result.put(externalId(p.id()),adapt(p)));
        return Map.copyOf(result);
    }
    public static Optional<PhysicalMaterials.MaterialProfile> material(BlockState state){
        var p=PhysicalMaterials.block(state);
        if(p.components().size()!=1)return Optional.empty();
        return PhysicalMaterials.material(p.components().getFirst().materialId()).filter(WoodPhysicsApi::wood);
    }
    public static Optional<Profile> profile(BlockState state){return material(state).map(WoodPhysicsApi::adapt);}
    public static Optional<Profile> profile(ResourceLocation blockId){
        if(!BuiltInRegistries.BLOCK.containsKey(blockId))return Optional.empty();
        return profile(BuiltInRegistries.BLOCK.get(blockId).defaultBlockState());
    }
    private static Sample evaluate(PhysicalMaterials.MaterialProfile p,PhysicalMaterials.State state){
        var component=new PhysicalMaterials.Component(p.id(),1);
        var effective=PhysicalMaterials.componentProperties(component,state).orElseThrow();
        // One cubic metre: component mass in kg is bulk density in kg/m3.
        // Core does not provide effective Janka/bending or wet flammability curves.
        return new Sample(adapt(p),effective.moistureKgKg()*100,effective.massKg(),
            hardness(p),Double.NaN,p.flammability());
    }
    public static Sample atMoisture(Profile p,double moisturePercent){
        if(!Double.isFinite(moisturePercent)||moisturePercent<0||moisturePercent>500)
            throw new IllegalArgumentException("Invalid moisture percent");
        var material=PhysicalMaterials.material(coreId(p.speciesId())).filter(WoodPhysicsApi::wood).orElseThrow();
        return evaluate(material,new PhysicalMaterials.State(20,moisturePercent/100));
    }
    public static Optional<Sample> sample(ServerLevel level,BlockPos pos){
        if(!level.getServer().isSameThread())throw new IllegalStateException("server thread required");
        if(level.isOutsideBuildHeight(pos)||!level.hasChunkAt(pos))return Optional.empty();
        return material(level.getBlockState(pos)).map(p->{
            // Local exposure is a Terrarian estimate supplied to Core, not another mass formula.
            // Core reference moisture is used unless there is actual rain or adjacent water.
            double moisture=p.referenceMoistureKgKg();
            if(Double.isFinite(moisture)&&level.isRainingAt(pos.above()))moisture+=.08;
            for(Direction d:Direction.values()){
                var q=pos.relative(d);
                if(!level.isOutsideBuildHeight(q)&&level.hasChunkAt(q)&&
                    level.getFluidState(q).is(net.minecraft.tags.FluidTags.WATER)){
                    moisture=Double.isFinite(moisture)?Math.max(moisture,.45):.45;break;
                }
            }
            return evaluate(p,new PhysicalMaterials.State(20,moisture));
        });
    }
}
