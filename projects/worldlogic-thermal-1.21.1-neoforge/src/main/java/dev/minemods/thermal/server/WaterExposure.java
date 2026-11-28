package dev.minemods.thermal.server;
import dev.minemods.thermal.ClimateModel;
import dev.minemods.worldlogic.api.WorldLogicApi;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import java.util.Optional;

/** Bounded, read-only thermal sampling; Core owns water identity and climate. */
final class WaterExposure {
    record Reading(String kind,int depth,double celsius) {}
    private WaterExposure(){}
    static boolean water(ServerLevel l,BlockPos p){
        return p.getY()>=l.getMinBuildHeight()&&p.getY()<l.getMaxBuildHeight()&&l.hasChunkAt(p)&&l.getFluidState(p).is(FluidTags.WATER);
    }
    static Optional<BlockPos> contact(ServerPlayer p){
        ServerLevel l=p.serverLevel();
        BlockPos at=p.blockPosition();
        if(water(l,at)&&p.getY()<at.getY()+l.getFluidState(at).getHeight(l,at))return Optional.of(at);
        if(p.isInWater()){
            for(BlockPos b:BlockPos.betweenClosed(at.offset(-1,0,-1),at.offset(1,1,1))){
                if(water(l,b)&&p.getBoundingBox().intersects(b.getX(),b.getY(),b.getZ(),b.getX()+1,b.getY()+l.getFluidState(b).getHeight(l,b),b.getZ()+1))return Optional.of(b.immutable());
            }
        }
        return Optional.empty();
    }
    static Reading sample(ServerLevel l,BlockPos at,double fallbackAir){
        String kind=AddonBridge.waterKind(l,at);
        if(kind.equals("NONE")||kind.equals("UNKNOWN")){
            var biome=l.getBiome(at);
            if(biome.is(net.minecraft.tags.BiomeTags.IS_OCEAN))kind="SEA";
            else if(biome.is(net.minecraft.tags.BiomeTags.IS_RIVER))kind="RIVER";
            else kind="UNKNOWN";
        }
        BlockPos surface=at;
        for(int i=0;i<48&&water(l,surface.above());i++)surface=surface.above();
        int depth=surface.getY()-at.getY();
        var env=WorldLogicApi.environment(l,surface.above()).or(()->WorldLogicApi.environment(l,at));
        double air=env.map(WorldLogicApi.Environment::ambientCelsiusEstimate).orElse(fallbackAir);
        final BlockPos top=surface;
        double base=env.flatMap(WorldLogicApi.Environment::regionalClimate)
            .map(r->14+28*r.thermalIndex()-Math.max(-80,Math.min(160,top.getY()-r.modeledSurfaceY()))*.065+AddonBridge.seasonalOffset(l,top))
            .orElseGet(()->ClimateModel.biomeAir(l.getBiome(top).value().getBaseTemperature(),top.getY(),l.getSeaLevel())+AddonBridge.seasonalOffset(l,top));
        return new Reading(kind,depth,ClimateModel.naturalWater(air,base,kind,depth));
    }
}
