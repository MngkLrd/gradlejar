package dev.minemods.thermal.server;
import dev.minemods.thermal.ThermalModel;
import dev.minemods.thermal.WeatherReading;
import dev.minemods.thermal.ClimateComfort;
import dev.minemods.thermal.content.FixtureBlock;
import dev.minemods.worldlogic.api.WorldLogicApi;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import static dev.minemods.thermal.ClimateModel.*;
public final class EnvironmentSampler {
    private static TagKey<Item> gear(String path) {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("worldlogic_thermal",path));
    }
    private static final TagKey<Item> INSULATED = gear("insulated_gear"), LIGHT = gear("light_gear"), METAL = gear("metal_gear"), PLANT = gear("plant_insulation"), SUMMER = gear("summer_gear");
    public record Sample(ThermalModel.Exposure exposure, double skinWetness, boolean sheltered, double solarExposure, WeatherReading weather) {}
    private EnvironmentSampler() {}
    public static Optional<Sample> sample(ServerPlayer p, double skin) {
        ServerLevel level = p.serverLevel(); BlockPos pos = p.blockPosition();
        var atmosphere=WorldLogicApi.atmosphere(level,pos);
        var snapshot=atmosphere.map(WorldLogicApi.Atmosphere::environment).or(()->WorldLogicApi.environment(level,pos));
        if(snapshot.isEmpty()) return Optional.empty();
        var env = snapshot.get();
        double air = env.ambientCelsiusEstimate();
        if(!Double.isFinite(air)) return Optional.empty();
        boolean sheltered = env.covered();
        boolean rain = env.rainingAt();
        var local=localComfort(level,p,air,atmosphere.map(WorldLogicApi.Atmosphere::relativeHumidityPercent).orElse(100.0),sheltered);
        double radiant=local.heat();
        double ins=0,ventilation=0;
        for (ItemStack stack : p.getArmorSlots()) {
            if (stack.isEmpty()) continue;
            if(stack.is(SUMMER)){ventilation+=.45;ins+=.02;}
            else ins += stack.is(PLANT) ? .25 : stack.is(INSULATED) ? .24 : stack.is(LIGHT) ? .13 : stack.is(METAL) ? .045 : .06;
        }
        // API v1 has no core gear moisture or hot-food accessor.
        var a=atmosphere.orElse(null);
        double previous=Double.isFinite(skin)?clamp(skin,0,1):0;
        skin=skinWetness(previous,p.isInWater(),rain,radiant);
        if(!rain&&!p.isInWater()&&a!=null)skin=previous+(skin-previous)*WeatherReading.dryingFactor(a.relativeHumidityPercent());
        double wind=a!=null?WeatherReading.normalizedWind(a.outdoorWindSpeedMps(),sheltered):sheltered?.06:env.globalThunder()?.85:rain?.55:.22;
        // Core ambient includes this day's solar/rain offsets; invert only when not clamped.
        double reference=Math.abs(air)>=99.99?Double.NaN:air-(sheltered?0:4*env.solarExposure()-2)+(rain?2:0);
        String biome=level.getBiome(pos).unwrapKey().map(k->k.location().toString()).orElse("");
        var regional=env.regionalClimate().orElse(null);
        var weather=new WeatherReading(biome,level.dimension().location().toString(),env.elevationAboveSea(),a!=null,
            a==null?0:a.relativeHumidityPercent(),a==null?0:a.outdoorWindSpeedMps(),a==null?0:a.outdoorWindXMps(),a==null?0:a.outdoorWindZMps(),
            a==null?0:a.precipitation().ordinal(),a==null?0:a.precipitationIntensity(),env.globalThunder(),env.solarExposure(),
            a==null?Double.NaN:reference,regional==null?Double.NaN:regional.absoluteLatitude(),regional==null?Double.NaN:regional.moistureIndex());
        double activity = p.isSprinting() ? 1 : p.isSwimming() ? .75 : p.getDeltaMovement().horizontalDistanceSqr()>.008 ? .35 : 0;
        return Optional.of(new Sample(new ThermalModel.Exposure(air,radiant,wind,skin,clamp(ins,0,1),activity,p.isInWater(),water(air,air),0,local.cooling(),clamp(ventilation,0,1)),skin,sheltered,env.solarExposure(),weather));
    }
    record LocalComfort(double heat,double cooling){}
    static LocalComfort localComfort(ServerLevel level,ServerPlayer p,double air,double humidity,boolean sheltered) {
        BlockPos at=p.blockPosition(); double heat=0,cooling=0;
        for(BlockPos b:BlockPos.betweenClosed(at.offset(-4,-2,-4),at.offset(4,3,4))) {
            if(!level.hasChunkAt(b)) continue;
            BlockState s=level.getBlockState(b);
            double output=output(s); boolean cooler=s.getBlock() instanceof FixtureBlock fixture&&fixture.cooling()&&s.getValue(FixtureBlock.ACTIVE); if(output<=0&&!cooler) continue;
            Vec3 center=Vec3.atCenterOf(b); double distance=center.distanceTo(p.position().add(0,.8,0));
            if(distance>5) continue;
            BlockHitResult hit=level.clip(new ClipContext(p.getEyePosition(),center,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p));
            if(hit.getType()!=HitResult.Type.MISS && !hit.getBlockPos().equals(b)) continue;
            heat+=ClimateComfort.heat(output,distance);
            if(cooler)cooling+=ClimateComfort.cooling(air,humidity,distance,sheltered,true);
        }
        return new LocalComfort(ClimateComfort.capRadiant(air,heat),Math.min(18,cooling));
    }
    private static double output(BlockState s) {
        if(s.getBlock() instanceof FixtureBlock fixture)return !fixture.cooling()&&s.getValue(FixtureBlock.ACTIVE)?65:0;
        if(s.is(Blocks.LANTERN))return 7;
        if(s.is(Blocks.SOUL_LANTERN))return 4;
        if(s.is(Blocks.TORCH)||s.is(Blocks.WALL_TORCH))return 3;
        if(s.is(Blocks.LAVA)) return 42;
        if(s.is(Blocks.FIRE)||s.is(Blocks.SOUL_FIRE)) return 22;
        if(s.is(Blocks.MAGMA_BLOCK)) return 8;
        if(!s.hasProperty(BlockStateProperties.LIT)||!s.getValue(BlockStateProperties.LIT)) return 0;
        if(s.is(Blocks.CAMPFIRE)||s.is(Blocks.SOUL_CAMPFIRE)) return 60;
        if(s.is(Blocks.FURNACE)||s.is(Blocks.BLAST_FURNACE)||s.is(Blocks.SMOKER)) return 46;
        return 0;
    }
}
