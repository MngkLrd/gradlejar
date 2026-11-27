package dev.minemods.thermal.server;
import dev.minemods.thermal.ThermalModel;
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
            n.remove("coldSeconds");n.remove("hotSeconds");n.remove("sunSeconds");n.remove("condition");root.put(KEY,n);
            ThermalSync.send(p,new ThermalSync.Snapshot(p.getId(),false,37,22,22,0,0,0,0,false,ThermalCondition.Kind.NONE,WeatherReading.unavailable()));
            return;
        }
        double previous=n.contains("body",Tag.TAG_ANY_NUMERIC)?n.getDouble("body"):37;
        if(!Double.isFinite(previous)) previous=37;
        previous=clamp(previous,30,43);
        var sampled=EnvironmentSampler.sample(p,n.getDouble("skinWet"));
        if(sampled.isEmpty()) return;
        var sample=sampled.get(); var e=sample.exposure();
        var state=ThermalModel.tick(new ThermalModel.State(previous,22,37,ThermalModel.stage(previous),0),e,1);
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
        ThermalSync.send(p,new ThermalSync.Snapshot(p.getId(),true,state.bodyC(),e.airC(),state.feelsLikeC(),state.trendCPerMinute(),e.wetness(),e.insulation(),e.radiantC(),sample.sheltered(),progress.kind(),sample.weather()));
        HistoryStore.tick(p,sample.weather(),state,e.airC());
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
    public static void registerCommands(RegisterCommandsEvent event) {
        var qa=Commands.literal("qa").requires(s->s.hasPermission(2));
        for(String name:new String[]{"climate","dry","heat","blocked","armor","effects","conditions","weather","history","comfort","fixtures","clothing","fiber_work","creative_tab","portable","advanced_clothes","devices","textile"}) {
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
