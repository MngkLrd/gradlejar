package dev.minemods.thermal.server;
import dev.minemods.thermal.ThermalModel;
import dev.minemods.thermal.Adaptation;
import dev.minemods.thermal.ThermalCondition;
import dev.minemods.thermal.WeatherReading;
import dev.minemods.thermal.network.ThermalSync;
import dev.minemods.worldlogic.api.WorldLogicApi;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import java.util.Locale;
import static dev.minemods.thermal.ClimateModel.clamp;
public final class ThermalEvents {
    public static final String KEY="worldlogic_thermal";
    static final ResourceLocation SPEED=ResourceLocation.fromNamespaceAndPath(KEY,"thermal_speed");
    private ThermalEvents() {}
    public static void tick(PlayerTickEvent.Post event) {
        if(event.getEntity() instanceof ServerPlayer p && p.tickCount%20==0 && p.isAlive()) update(p);
    }
    static void update(ServerPlayer p) {
        CompoundTag root=p.getPersistentData(); CompoundTag n=root.getCompound(KEY);
        boolean active=!p.isCreative()&&!p.isSpectator();
        if(!active) {
            penalty(p,0);
            n.remove("room");n.remove("physiology");n.remove("sampleTick");n.remove("sampleDimension");
            n.remove("coldSeconds");n.remove("hotSeconds");n.remove("sunSeconds");n.remove("condition");root.put(KEY,n);
            ThermalSync.send(p,new ThermalSync.Snapshot(p.getId(),false,37,22,22,0,0,0,0,false,ThermalCondition.Kind.NONE,WeatherReading.unavailable(),0,0,1,0));
            return;
        }
        double previous=n.contains("body",Tag.TAG_ANY_NUMERIC)?n.getDouble("body"):37;
        if(!Double.isFinite(previous)) previous=37;
        previous=clamp(previous,30,43);
        var sampled=EnvironmentSampler.sample(p,n.getDouble("skinWet"));
        if(sampled.isEmpty()) {
            penalty(p,0);n.remove("sampleTick");n.remove("sampleDimension");n.remove("room");n.remove("physiology");root.put(KEY,n);
            return;
        }
        var sample=sampled.get(); var e=sample.exposure();
        var state=ThermalModel.tick(new ThermalModel.State(previous,22,37,ThermalModel.stage(previous),0),e,1);
        var adapted=Adaptation.step(new Adaptation.State(e.coldAdaptation(),e.heatAdaptation()),state.feelsLikeC(),state.bodyC());
        n.putDouble("adaptCold",adapted.cold());n.putDouble("adaptHeat",adapted.heat());
        n.putDouble("mealWarmth",Math.max(0,e.mealWarmth()-1.0/180));
        var room=sample.room();
        if(room.indoor()){
            var data=new CompoundTag();data.putDouble("air",room.airC());data.putDouble("humidity",room.humidity());data.putDouble("ventilation",room.ventilation());data.putDouble("mass",room.massC());data.putInt("volume",room.volume());data.putLong("tick",p.serverLevel().getGameTime());data.putString("dimension",p.serverLevel().dimension().location().toString());n.put("room",data);
        }else n.remove("room");
        var ph=sample.physiology();
        var phTag=new CompoundTag();phTag.putBoolean("metabolism",ph.metabolism());phTag.putBoolean("hydration",ph.hydration());phTag.putBoolean("trauma",ph.trauma());
        phTag.putDouble("energy",ph.energy());phTag.putDouble("diet",ph.dietQuality());phTag.putDouble("water",ph.waterPercent());phTag.putDouble("salts",ph.electrolyteIndex());phTag.putDouble("blood",ph.bloodFraction());phTag.putDouble("perfusion",ph.perfusion());
        phTag.putDouble("nutrition",e.nutrition());phTag.putDouble("cooling",e.hydration());phTag.putLong("tick",p.serverLevel().getGameTime());phTag.putString("dimension",p.serverLevel().dimension().location().toString());n.put("physiology",phTag);
        n.putLong("sampleTick",p.serverLevel().getGameTime());n.putString("sampleDimension",p.serverLevel().dimension().location().toString());
        n.putDouble("body",state.bodyC()); n.putDouble("air",e.airC()); n.putDouble("felt",state.feelsLikeC());
        n.putDouble("skinWet",sample.skinWetness()); n.putDouble("wet",e.wetness()); n.putDouble("insulation",e.insulation());
        n.putDouble("trend",state.trendCPerMinute()); n.putDouble("radiant",e.radiantC()); n.putString("stage",state.stage().name());
        root.put(KEY,n);
        double slow=switch(state.stage()) {case HYPOTHERMIA -> .22; case VERY_COLD -> .12; case HYPERTHERMIA -> .16; case VERY_HOT -> .07; default -> 0;};
        penalty(p,slow);
        double fatigue=switch(state.stage()) {case HYPOTHERMIA,HYPERTHERMIA -> .24; case VERY_HOT,VERY_COLD -> .09; case WARM,COLD -> .02; default -> 0;};
        if(fatigue>0 && WorldLogicApi.fatigue(p).enabled()) WorldLogicApi.addExertion(p,fatigue);
        var prior=new ThermalCondition.Progress(n.getInt("coldSeconds"),n.getInt("hotSeconds"),n.getInt("sunSeconds"),ThermalCondition.parse(n.getString("condition")));
        var progress=ThermalCondition.advance(prior,state.stage(),sample.solarExposure(),e.submerged());
        n.putInt("coldSeconds",progress.coldSeconds());n.putInt("hotSeconds",progress.hotSeconds());
        n.putInt("sunSeconds",progress.sunSeconds());n.putString("condition",progress.kind().name());n.remove("criticalSeconds");
        ThermalInjury.apply(p,progress);
        LimbExposure.update(p,n,e,state);
        root.put(KEY,n);
        ThermalSync.send(p,new ThermalSync.Snapshot(p.getId(),true,state.bodyC(),e.airC(),state.feelsLikeC(),state.trendCPerMinute(),e.wetness(),e.insulation(),e.radiantC(),sample.sheltered(),progress.kind(),sample.weather(),adapted.cold(),adapted.heat(),e.nutrition(),e.roomDelta(),sample.room(),e.submerged()?e.waterC():Double.NaN));
        HistoryStore.tick(p,sample.weather(),state,e.airC());
    }
    /** Core admin coordinator resets only Thermal-owned transient physiology. */
    public static boolean adminRestore(ServerPlayer p){
        if(!p.serverLevel().getServer().isSameThread())throw new IllegalStateException("Thermal API requires server thread");
        if(!p.isAlive()||p.isCreative()||p.isSpectator())return false;
        var root=p.getPersistentData();var n=root.getCompound(KEY);
        n.putDouble("body",37);n.putDouble("trend",0);n.putDouble("skinWet",0);n.putDouble("wet",0);
        n.putString("stage",ThermalModel.Stage.COMFORTABLE.name());n.putString("condition",ThermalCondition.Kind.NONE.name());
        for(String key:new String[]{"coldSeconds","hotSeconds","sunSeconds","criticalSeconds"})n.remove(key);
        for(String limb:LimbExposure.LIMBS)n.remove("frost_"+limb);
        n.remove("physiology");n.remove("room");n.remove("sampleTick");n.remove("sampleDimension");
        penalty(p,0);
        root.put(KEY,n);
        var sample=EnvironmentSampler.sample(p,0);
        if(sample.isPresent()){
            var v=sample.get();var x=v.exposure();
            var state=ThermalModel.tick(ThermalModel.initial(),x,0);
            n.putDouble("air",x.airC());n.putDouble("felt",state.feelsLikeC());n.putDouble("insulation",x.insulation());n.putDouble("radiant",x.radiantC());
            n.putLong("sampleTick",p.serverLevel().getGameTime());n.putString("sampleDimension",p.serverLevel().dimension().location().toString());
            ThermalSync.send(p,new ThermalSync.Snapshot(p.getId(),true,37,x.airC(),state.feelsLikeC(),0,0,x.insulation(),x.radiantC(),v.sheltered(),ThermalCondition.Kind.NONE,v.weather(),n.getDouble("adaptCold"),n.getDouble("adaptHeat"),x.nutrition(),x.roomDelta(),v.room(),x.submerged()?x.waterC():Double.NaN));
        }else ThermalSync.send(p,new ThermalSync.Snapshot(p.getId(),false,37,22,22,0,0,0,0,false,ThermalCondition.Kind.NONE,WeatherReading.unavailable(),0,0,1,0));
        root.put(KEY,n);return true;
    }
    static void penalty(ServerPlayer p,double amount) {
        var attribute=p.getAttribute(Attributes.MOVEMENT_SPEED); if(attribute==null) return;
        var old=attribute.getModifier(SPEED);
        if(old!=null && Math.abs(old.amount()+amount)<.00001) return;
        if(old!=null) attribute.removeModifier(SPEED);
        if(amount>0) attribute.addTransientModifier(new AttributeModifier(SPEED,-amount,AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }
    public static void clonePlayer(PlayerEvent.Clone event) {
        HistoryStore.cloneHistory(event.getOriginal(),event.getEntity());
        if(!event.isWasDeath() && event.getOriginal().getPersistentData().contains(KEY))
            event.getEntity().getPersistentData().put(KEY,event.getOriginal().getPersistentData().getCompound(KEY).copy());
        else if(event.isWasDeath()) event.getEntity().getPersistentData().remove(KEY);
    }
    public static void meal(net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent.Finish event){
        if(!(event.getEntity() instanceof ServerPlayer p)||p.isCreative()||p.isSpectator())return;
        var food=event.getItem().get(net.minecraft.core.component.DataComponents.FOOD);
        if(food==null||food.nutrition()<=0)return;
        var root=p.getPersistentData();var n=root.getCompound(KEY);
        // Metabolic contribution only: no invented literal food temperature or duplicated core food state.
        n.putDouble("mealWarmth",Math.min(1,Math.max(n.getDouble("mealWarmth"),food.nutrition()/8.0)));
        root.put(KEY,n);
    }
    /** Covers fire, explosions, commands, support loss and other non-player block changes. */
    public static void insulationChanged(net.minecraft.server.level.ServerLevel level,net.minecraft.core.BlockPos pos){
        RoomHeat.markDirty(level,pos);
        EnvironmentSampler.invalidateLocal(level,pos);
    }
    public static void blockChanged(net.neoforged.neoforge.event.level.BlockEvent event){
        if(event.getLevel() instanceof net.minecraft.server.level.ServerLevel level){
            RoomHeat.markDirty(level,event.getPos());EnvironmentSampler.invalidateLocal(level,event.getPos());
        }
    }
    public static void registerCommands(RegisterCommandsEvent event) {
        var qa=Commands.literal("qa").requires(s->s.hasPermission(2));
        for(String name:new String[]{"climate","dry","heat","blocked","armor","effects","conditions","weather","history","comfort","fixtures","clothing","fiber_work","creative_tab","portable","advanced_clothes","devices","textile","adaptation","frostbite","nutrition","room_inertia","room_ventilation","room_humidity","room_persistence","room_cache","village_climate","integration","water_exposure","room_response","hud_signals","physical_materials","wall_insulation","insulation_fire","snow_shelter","leaf_canopy","polar_survival","admin_restore"}) {
            qa.then(Commands.literal(name).executes(c->{
                ThermalChecks.verify(c.getSource().getLevel(),name);
                c.getSource().sendSuccess(()->Component.literal("THERMAL_QA_"+name+"_PASS"),false);
                return 1;
            }));
        }
        event.getDispatcher().register(Commands.literal("thermal").then(qa).then(Commands.literal("status").executes(c->{
            ServerPlayer p=c.getSource().getPlayerOrException(); CompoundTag n=p.getPersistentData().getCompound(KEY);
            c.getSource().sendSuccess(()->Component.literal(n.contains("body")?String.format(Locale.ROOT,"body=%.2f air=%.2f felt=%.2f stage=%s wet=%.2f insulation=%.2f heat=%.2f condition=%s",n.getDouble("body"),n.getDouble("air"),n.getDouble("felt"),n.getString("stage"),n.getDouble("wet"),n.getDouble("insulation"),n.getDouble("radiant"),n.getString("condition")):"Thermal state not initialized"),false);return 1;
        })));
    }
}
