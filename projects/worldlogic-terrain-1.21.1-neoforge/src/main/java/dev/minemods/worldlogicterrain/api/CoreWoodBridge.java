package dev.minemods.worldlogicterrain.api;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import dev.minemods.worldlogic.api.PhysicalMaterials;
import dev.minemods.worldlogic.api.WorldLogicApi;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import dev.minemods.worldlogicterrain.api.WoodPhysicsApi.Profile;
import dev.minemods.worldlogicterrain.api.WoodPhysicsApi.Sample;

/** Linked only when PhysicalMaterials revision 2 is available. */
final class CoreWoodBridge {
    private static final Set<String> VANILLA=Set.of("oak","spruce","birch","jungle","acacia","dark_oak","mangrove","cherry","bamboo","crimson","warped");
    private CoreWoodBridge(){}
    private static boolean wood(PhysicalMaterials.MaterialProfile p){
        return (p.id().getNamespace().equals("worldlogicterrain")||
            p.id().getNamespace().equals("worldlogic")&&VANILLA.contains(p.id().getPath()))
            &&PhysicalMaterials.woodProperties(p.id()).isPresent();
    }
    private static ResourceLocation externalId(ResourceLocation coreId){
        return coreId.getNamespace().equals("worldlogic")?
            ResourceLocation.fromNamespaceAndPath("minecraft",coreId.getPath()):coreId;
    }
    private static ResourceLocation coreId(ResourceLocation speciesId){
        return speciesId.getNamespace().equals("minecraft")?
            ResourceLocation.fromNamespaceAndPath("worldlogic",speciesId.getPath()):speciesId;
    }
    private static Profile adapt(PhysicalMaterials.MaterialProfile p){
        var sample=PhysicalMaterials.woodAtMoisture(p.id(),p.referenceMoistureKgKg()).orElseThrow();
        return new Profile(externalId(p.id()),sample.densityKgM3(),sample.jankaKN()*1000,
            sample.bendingStrengthMPa(),sample.properties().greenMoistureKgKg()*100,p.flammability());
    }
    static Map<ResourceLocation,Profile> profiles(){
        var result=new LinkedHashMap<ResourceLocation,Profile>();
        PhysicalMaterials.materials().values().stream().filter(CoreWoodBridge::wood)
            .forEach(p->result.put(externalId(p.id()),adapt(p)));
        return Map.copyOf(result);
    }
    private static Optional<PhysicalMaterials.MaterialProfile> material(BlockState state){
        var p=PhysicalMaterials.block(state);
        if(p.components().size()!=1)return Optional.empty();
        return PhysicalMaterials.material(p.components().getFirst().materialId()).filter(CoreWoodBridge::wood);
    }
    static Optional<Profile> profile(BlockState state){return material(state).map(CoreWoodBridge::adapt);}
    static Optional<Profile> profile(ResourceLocation blockId){
        if(!BuiltInRegistries.BLOCK.containsKey(blockId))return Optional.empty();
        return profile(BuiltInRegistries.BLOCK.get(blockId).defaultBlockState());
    }
    private static Sample evaluate(PhysicalMaterials.MaterialProfile p,double moisture){
        var value=PhysicalMaterials.woodAtMoisture(p.id(),moisture).orElseThrow();
        return new Sample(adapt(p),value.moistureKgKg()*100,value.densityKgM3(),
            value.jankaKN()*1000,value.bendingStrengthMPa(),value.flammability());
    }
    static Sample atMoisture(Profile p,double moisturePercent){
        if(!Double.isFinite(moisturePercent)||moisturePercent<0||moisturePercent>250)
            throw new IllegalArgumentException("Moisture must be 0..250 percent");
        var material=PhysicalMaterials.material(coreId(p.speciesId())).filter(CoreWoodBridge::wood).orElseThrow();
        return evaluate(material,moisturePercent/100);
    }
    static Optional<Sample> sample(ServerLevel level,BlockPos pos){
        if(!level.getServer().isSameThread())throw new IllegalStateException("server thread required");
        if(level.isOutsideBuildHeight(pos)||!level.hasChunkAt(pos))return Optional.empty();
        var state=level.getBlockState(pos);
        String path=BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
        boolean raw=path.endsWith("_log")||path.endsWith("_wood")||path.endsWith("_stem")||
            path.endsWith("_hyphae")||path.equals("bamboo")||path.endsWith("bamboo_block");
        return material(state).map(p->{
            // Terrarian owns local ecological exposure; Core owns the moisture response.
            double regional=WorldLogicApi.environment(level,pos).flatMap(WorldLogicApi.Environment::regionalClimate)
                .map(WorldLogicApi.RegionalClimate::moistureIndex).orElse(level.getBiome(pos).value().hasPrecipitation()?.55:.20);
            regional=Double.isFinite(regional)?Math.max(0,Math.min(1,regional)):.5;
            double green=PhysicalMaterials.woodProperties(p.id()).orElseThrow().greenMoistureKgKg();
            double moisture=raw&&Double.isFinite(green)?green*(.8+.4*regional):p.referenceMoistureKgKg()*(.65+.9*regional);
            if(level.isRainingAt(pos.above()))moisture+=raw?.12:.08;
            for(Direction d:Direction.values()){
                var q=pos.relative(d);
                if(!level.isOutsideBuildHeight(q)&&level.hasChunkAt(q)&&level.getFluidState(q).is(net.minecraft.tags.FluidTags.WATER)){
                    moisture=Math.max(moisture,raw?1:.45);break;
                }
            }
            return evaluate(p,Math.max(0,Math.min(2.5,moisture)));
        });
    }
}
