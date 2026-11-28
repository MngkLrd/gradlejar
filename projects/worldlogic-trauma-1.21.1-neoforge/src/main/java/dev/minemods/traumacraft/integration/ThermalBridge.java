package dev.minemods.traumacraft.integration;

import dev.minemods.traumacraft.health.BodyState;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;
import java.lang.reflect.Method;
import java.util.Optional;

/** Optional read-only adapter for Thermal 0.3.0-rc1.
 * ThermalEvents.KEY writes stage/body on the server. No core NBT or thermal state mutation.
 * Without Thermal, all recovery and UI behavior are unchanged.
 */
public final class ThermalBridge {
    public static final String MOD_ID="worldlogic_thermal";
    private ThermalBridge(){}
    private static final class OptionalApi {
        static final Access ACCESS=Access.find();
    }
    private record Access(Method status,Method body,Method condition,Method stage){
        static Access find(){
            try{
                Class<?> api=Class.forName("dev.minemods.thermal.api.ThermalApi");
                if(api.getField("API_VERSION").getInt(null)!=1)return null;
                Class<?> value=Class.forName("dev.minemods.thermal.api.ThermalApi$Status");
                Class<?> model=Class.forName("dev.minemods.thermal.ThermalModel");
                return new Access(api.getMethod("status",ServerPlayer.class),value.getMethod("bodyC"),value.getMethod("condition"),model.getMethod("stage",double.class));
            }catch(ReflectiveOperationException|LinkageError ex){return null;}
        }
        ThermalCondition read(ServerPlayer p){
            try{
                Optional<?> snapshot=(Optional<?>)status.invoke(null,p);
                if(snapshot.isEmpty())return ThermalCondition.NONE;
                Object value=snapshot.get();double temp=(Double)body.invoke(value);
                if(!Double.isFinite(temp)||temp<30||temp>43)return ThermalCondition.NONE;
                String phase=((Enum<?>)stage.invoke(null,temp)).name();
                String clinical=((Enum<?>)condition.invoke(value)).name();
                return ThermalCondition.actual(phase,clinical,temp);
            }catch(ReflectiveOperationException|LinkageError|ClassCastException ex){return ThermalCondition.NONE;}
        }
    }
    public static ThermalCondition condition(ServerPlayer p){
        if(!ModList.get().isLoaded(MOD_ID)||!WorldLogicBridge.active(p))return ThermalCondition.NONE;
        var api=OptionalApi.ACCESS;
        if(api!=null)return api.read(p);
        // Legacy RC1 provider without the public API.
        var root=p.getPersistentData();
        if(!root.contains(MOD_ID,Tag.TAG_COMPOUND))return ThermalCondition.NONE;
        var n=root.getCompound(MOD_ID);
        if(!n.contains("body",Tag.TAG_ANY_NUMERIC)||!n.contains("stage",Tag.TAG_STRING))return ThermalCondition.NONE;
        return ThermalCondition.actual(n.getString("stage"),n.getString("condition"),n.getDouble("body"));
    }
    /** Only Trauma's natural repair is affected; no second movement, damage or fatigue penalty. */
    public static void recover(BodyState b,ThermalCondition s,boolean healTissue){
        recover(b,s,healTissue,1,1);
    }
    public static void recover(BodyState b,ThermalCondition s,boolean healTissue,float tissue,float bone){
        if(b.hasAnyBleeding()||b.organs().unconscious()||b.vitalDestroyed()||b.fatalBloodLoss())return;
        float perfusion=b.organs().repairFactor(b.bloodFraction());
        int ticks=Math.round(s.recoveryTicks(20)*bone*perfusion);
        if(ticks>0)b.recoverBones(ticks);
        if(healTissue&&s.recoveryFactor>0)b.healMostDamaged(.75f*s.recoveryFactor*tissue*perfusion);
    }
}
