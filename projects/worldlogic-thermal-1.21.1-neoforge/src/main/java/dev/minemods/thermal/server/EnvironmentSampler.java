package dev.minemods.thermal.server;
import dev.minemods.thermal.ThermalModel;
import dev.minemods.thermal.WeatherReading;
import dev.minemods.thermal.ClimateComfort;
import dev.minemods.thermal.content.*;
import dev.minemods.worldlogic.api.WorldLogicApi;
import java.util.*;
import dev.minemods.thermal.RoomReading;
import dev.minemods.thermal.PhysiologyInputs;
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
    static double materialInsulation(ItemStack stack){
        if(stack.isEmpty())return 0;
        return PhysicalBridge.insulation(stack).orElseGet(()->stack.is(PLANT)?.25:stack.is(INSULATED)?.24:stack.is(LIGHT)?.13:stack.is(METAL)?.045:.06);
    }
    public record Sample(ThermalModel.Exposure exposure, double skinWetness, boolean sheltered, double solarExposure, WeatherReading weather,RoomReading room,PhysiologyInputs physiology) {}
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
        boolean rain = env.rainingAt()&&!sheltered;
        boolean leafCover=VentBlock.leafCover(level,pos);

        double outdoorHumidity=atmosphere.map(WorldLogicApi.Atmosphere::relativeHumidityPercent).orElse(100.0);
        double outdoorWind=atmosphere.map(a->clamp(a.outdoorWindSpeedMps()/12,0,1)).orElse(0.0);
        var roomReading=RoomHeat.sample(level,pos,air,outdoorHumidity,outdoorWind);
        double room=roomReading.indoor()?roomReading.airC()-air:0;
        var waterContact=WaterExposure.contact(p);
        boolean submerged=waterContact.isPresent()||p.isInWater();
        double waterC=waterContact.map(q->WaterExposure.sample(level,q,air).celsius()).orElseGet(()->water(air,air));
        double localAir=air+room,localHumidity=roomReading.indoor()?roomReading.humidity():outdoorHumidity;
        double thermalMedium=submerged?waterC:localAir;
        var local=localComfort(level,p,thermalMedium,localHumidity,sheltered||roomReading.indoor(),outdoorWind,candidates(level,pos));
        double radiant=local.heat();
        var regionalWeather=AddonBridge.weather(level,pos);
        double ins=0,ventilation=0,rainShield=0,windShield=0,sunShield=0;
        for (ItemStack stack : p.getArmorSlots()) {
            if (stack.isEmpty()) continue;
            if(stack.getItem() instanceof ClimateGarment garment){var profile=garment.profile();ins+=PhysicalBridge.garmentInsulation(stack,profile.insulation());ventilation+=profile.ventilation();rainShield+=profile.rainShield();windShield+=profile.windShield();sunShield+=profile.sunShield();}
            else if(stack.is(SUMMER)){ventilation+=.45;ins+=.02;}
            else ins += materialInsulation(stack);
        }
        // API v1 has no core gear moisture or hot-food accessor.
        var a=atmosphere.orElse(null);
        double previous=Double.isFinite(skin)?clamp(skin,0,1):0;
        skin=skinWetness(previous,submerged,rain&&!roomReading.indoor(),radiant);
        if(rain&&!roomReading.indoor()&&!submerged)skin=clamp(previous+(skin-previous)*(1-clamp(rainShield,0,.92))*(leafCover?.30:1),0,1);
        if((!rain||roomReading.indoor())&&!submerged&&a!=null)skin=previous+(skin-previous)*WeatherReading.dryingFactor(localHumidity);
        double wind=a!=null?WeatherReading.normalizedWind(a.outdoorWindSpeedMps(),sheltered):sheltered?.06:env.globalThunder()?.85:rain?.55:.22;
        if(!sheltered&&regionalWeather.isPresent())wind=Math.max(wind,WeatherReading.normalizedWind(regionalWeather.get().gust()*.65,false));
        if(roomReading.indoor())wind=Math.max(.06,roomReading.ventilation()*outdoorWind+.02);
        wind*=1-clamp(windShield,0,.8);
        if(leafCover&&!roomReading.indoor())wind*=.75;
        double shade=VentBlock.shades(level,pos)?1:clamp(sunShield,0,.95);
        double solar=env.solarExposure()*(1-shade);
        double shadeCooling=submerged?0:Math.max(0,air-24)*shade*env.solarExposure()*.25;
        double activity = p.isSprinting() ? 1 : p.isSwimming() ? .75 : p.getDeltaMovement().horizontalDistanceSqr()>.008 ? .35 : 0;
        double retained=clamp(ins,0,1)*(1-.75*skin);
        double warmLimit=28-activity*7-retained*5+clamp(ventilation,0,1)*7;
        double currentBody=p.getPersistentData().getCompound(ThermalEvents.KEY).contains("body")?p.getPersistentData().getCompound(ThermalEvents.KEY).getDouble("body"):37;
        double effective=submerged?waterC+radiant*.30-local.cooling()*.40:air+room+radiant-local.cooling()-shadeCooling-wind*(2+5*skin)-6*skin;
        var portable=ThermalPackItem.comfort(ThermalPackItem.hotbar(p,localHumidity),effective,warmLimit,currentBody);
        radiant=ClimateComfort.capRadiant(thermalMedium,radiant+portable.heat());
        double cooling=Math.min(18,local.cooling()+shadeCooling+portable.cool());
        // Core ambient includes this day's solar/rain offsets; invert only when not clamped.
        double reference=regionalWeather.isPresent()?Double.NaN:Math.abs(air)>=99.99?Double.NaN:air-(sheltered?0:4*env.solarExposure()-2)+(rain?2:0);
        String biome=level.getBiome(pos).unwrapKey().map(k->k.location().toString()).orElse("");
        var regional=env.regionalClimate().orElse(null);
        var weather=new WeatherReading(biome,level.dimension().location().toString(),env.elevationAboveSea(),a!=null,
            a==null?0:a.relativeHumidityPercent(),a==null?0:a.outdoorWindSpeedMps(),a==null?0:a.outdoorWindXMps(),a==null?0:a.outdoorWindZMps(),
            a==null?0:a.precipitation().ordinal(),a==null?0:a.precipitationIntensity(),env.globalThunder(),env.solarExposure(),
            a==null?Double.NaN:reference,regional==null?Double.NaN:regional.absoluteLatitude(),regional==null?Double.NaN:regional.moistureIndex(),regionalWeather.map(AddonBridge.Local::kind).orElse(""),regionalWeather.map(AddonBridge.Local::gust).orElse(Double.NaN),regionalWeather.map(AddonBridge.Local::pressure).orElse(Double.NaN),regionalWeather.map(AddonBridge.Local::cloud).orElse(Double.NaN),regionalWeather.map(AddonBridge.Local::fog).orElse(Double.NaN));
        var n=p.getPersistentData().getCompound(ThermalEvents.KEY);
        var adaptation=new dev.minemods.thermal.Adaptation.State(n.getDouble("adaptCold"),n.getDouble("adaptHeat"));
        var physiology=AddonBridge.physiology(p);
        double nutrition=physiology.nutrition(dev.minemods.thermal.Adaptation.nutrition(p.getFoodData().getFoodLevel(),p.getFoodData().getSaturationLevel()));
        double hydration=physiology.coolingCapacity();
        // Metabolism already accounts for digestion and energy. Keep the legacy meal bonus only without that provider.
        double meal=physiology.metabolism()?0:clamp(n.getDouble("mealWarmth"),0,1);
        // Share heat between immediate radiant exchange and bounded retained room heat.
        if(roomReading.indoor())radiant*=.8;
        double contact=0;
        var floor=level.getBlockState(pos.below());
        if(!submerged&&p.onGround()&&(floor.is(Blocks.ICE)||floor.is(Blocks.PACKED_ICE)||floor.is(Blocks.BLUE_ICE)||floor.is(Blocks.SNOW_BLOCK)))
            contact=(floor.is(Blocks.SNOW_BLOCK)?.6:2)*(1-LimbExposure.protection(p.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.FEET)));
        cooling=Math.min(18,cooling+contact);
        return Optional.of(new Sample(new ThermalModel.Exposure(air,radiant,wind,skin,clamp(ins,0,1),activity,submerged,waterC,meal,cooling,clamp(ventilation,0,1),adaptation.cold(),adaptation.heat(),nutrition,hydration,room,physiology.circulation()),skin,sheltered,solar,weather,roomReading,physiology));
    }
    record LocalComfort(double heat,double cooling){}
    static LocalComfort localComfort(ServerLevel level,ServerPlayer p,double air,double humidity,boolean sheltered) {
        return localComfort(level,p,air,humidity,sheltered,.5);
    }

    private record CandidateCell(long tick,List<BlockPos> positions){}
    private static final Map<ServerLevel,LinkedHashMap<BlockPos,CandidateCell>> SOURCES=new WeakHashMap<>();
    static void invalidateLocal(ServerLevel l){SOURCES.remove(l);}
    static void invalidateLocal(ServerLevel l,BlockPos pos){
        var map=SOURCES.get(l);if(map!=null)map.keySet().removeIf(k->pos.getX()>=k.getX()-4&&pos.getX()<=k.getX()+6&&pos.getY()>=k.getY()-2&&pos.getY()<=k.getY()+5&&pos.getZ()>=k.getZ()-4&&pos.getZ()<=k.getZ()+6);
    }
    static boolean potential(BlockState s){
        return s.getBlock() instanceof ClimateDeviceBlock||s.getBlock() instanceof FixtureBlock||
            s.is(Blocks.CAMPFIRE)||s.is(Blocks.SOUL_CAMPFIRE)||s.is(Blocks.FURNACE)||s.is(Blocks.BLAST_FURNACE)||s.is(Blocks.SMOKER)||output(s)>0;
    }
    static List<BlockPos> candidates(ServerLevel l,BlockPos at){
        var key=new BlockPos(Math.floorDiv(at.getX(),3)*3,Math.floorDiv(at.getY(),3)*3,Math.floorDiv(at.getZ(),3)*3);
        var map=SOURCES.computeIfAbsent(l,k->new LinkedHashMap<>(32,.75f,true));
        long tick=l.getGameTime();var old=map.get(key);
        if(old!=null&&tick>=old.tick()&&tick-old.tick()<40)return old.positions();
        var found=new ArrayList<BlockPos>();
        for(BlockPos q:BlockPos.betweenClosed(key.offset(-4,-2,-4),key.offset(6,5,6)))
            if(q.getY()>=l.getMinBuildHeight()&&q.getY()<l.getMaxBuildHeight()&&l.hasChunkAt(q)&&potential(l.getBlockState(q)))found.add(q.immutable());
        var result=List.copyOf(found);map.put(key,new CandidateCell(tick,result));
        while(map.size()>128)map.remove(map.keySet().iterator().next());
        return result;
    }
    static LocalComfort localComfort(ServerLevel level,ServerPlayer p,double air,double humidity,boolean sheltered,double outdoorWind) {
        BlockPos at=p.blockPosition();
        return localComfort(level,p,air,humidity,sheltered,outdoorWind,BlockPos.betweenClosed(at.offset(-4,-2,-4),at.offset(4,3,4)));
    }
    private static LocalComfort localComfort(ServerLevel level,ServerPlayer p,double air,double humidity,boolean sheltered,double outdoorWind,Iterable<BlockPos> positions) {
        BlockPos at=p.blockPosition(); double heat=0,cooling=0;
        for(BlockPos b:positions) {
            if(Math.abs(b.getX()-at.getX())>4||Math.abs(b.getZ()-at.getZ())>4||b.getY()<at.getY()-2||b.getY()>at.getY()+3)continue;
            if(!level.hasChunkAt(b)) continue;
            BlockState s=level.getBlockState(b);
            double output=output(s); boolean cooler=s.getBlock() instanceof FixtureBlock fixture&&fixture.cooling()&&s.getValue(FixtureBlock.ACTIVE); ClimateDeviceBlock device=s.getBlock() instanceof ClimateDeviceBlock d?d:null; if(output<=0&&!cooler&&device==null) continue;
            Vec3 center=Vec3.atCenterOf(b); double distance=center.distanceTo(p.position().add(0,.8,0));
            if(distance>5) continue;
            BlockHitResult hit=level.clip(new ClipContext(p.getEyePosition(),center,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p));
            if(hit.getType()!=HitResult.Type.MISS && !hit.getBlockPos().equals(b)) continue;
            heat+=ClimateComfort.heat(output*(device==null?1:VentBlock.adjustment(level,b)),distance);
            if(device!=null)cooling+=device.cooling(s,air,humidity,distance,sheltered,outdoorWind,level.canSeeSky(b.above()));
            if(cooler)cooling+=ClimateComfort.cooling(air,humidity,distance,sheltered,true);
        }
        return new LocalComfort(ClimateComfort.capRadiant(air,heat),Math.min(18,cooling));
    }
    static double output(BlockState s) {
        if(s.getBlock() instanceof ClimateDeviceBlock device)return device.heat(s);
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
