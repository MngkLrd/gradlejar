package dev.minemods.worldlogicterrain.api;

import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

/** Stable consumer facade. Core owns all numerical material calculations.
 * On older Core, profiles/sample are absent, never fabricated. */
public final class WoodPhysicsApi {
    public static final int API_VERSION=3;
    private static final boolean AVAILABLE=detect();
    private static boolean detect(){
        try{
            return Class.forName("dev.minemods.worldlogic.api.PhysicalMaterials")
                .getField("REVISION").getInt(null)>=2;
        }catch(ClassNotFoundException|NoSuchFieldException|IllegalAccessException e){return false;}
    }
    public static boolean available(){return AVAILABLE;}
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
    public static Map<ResourceLocation,Profile> profiles(){return AVAILABLE?CoreWoodBridge.profiles():Map.of();}
    public static Optional<Profile> profile(BlockState state){return AVAILABLE?CoreWoodBridge.profile(state):Optional.empty();}
    public static Optional<Profile> profile(ResourceLocation id){return AVAILABLE?CoreWoodBridge.profile(id):Optional.empty();}
    public static Optional<Sample> sample(ServerLevel level,BlockPos pos){return AVAILABLE?CoreWoodBridge.sample(level,pos):Optional.empty();}
    public static Sample atMoisture(Profile p,double moisturePercent){
        if(!AVAILABLE)throw new UnsupportedOperationException("Core PhysicalMaterials revision 2 required");
        return CoreWoodBridge.atMoisture(p,moisturePercent);
    }
}
