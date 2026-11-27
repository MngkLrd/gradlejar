package dev.minemods.traumacraft.integration;

import dev.minemods.traumacraft.disease.DiseaseModel.Physiology;
import java.lang.reflect.Method;
import java.util.Optional;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;
import org.slf4j.LoggerFactory;

/** Public Hydration API revision 2 only. No foreign NBT or writes to hydration. */
public final class HydrationBridge {
    private static final org.slf4j.Logger LOG=LoggerFactory.getLogger("worldlogic_trauma");
    private static boolean warned;
    private HydrationBridge(){}
    private static class Endpoint {
        static final Access API=resolve();
    }
    private record Access(Method read,Method water,Method electrolytes,Method stage){}
    private static Access resolve(){
        if(!ModList.get().isLoaded("hydration"))return null;
        try{
            Class<?> api=Class.forName("dev.minemods.hydration.api.HydrationApi");
            if(api.getField("API_VERSION").getInt(null)!=1||api.getField("API_REVISION").getInt(null)<2)return null;
            Class<?> type=Class.forName("dev.minemods.hydration.api.HydrationApi$Physiology");
            return new Access(api.getMethod("physiology",ServerPlayer.class),type.getMethod("hydrationPercent"),type.getMethod("electrolyteIndex"),type.getMethod("dehydrationStage"));
        }catch(ReflectiveOperationException|LinkageError|RuntimeException ex){LOG.warn("Hydration physiology API unavailable",ex);return null;}
    }
    public static Optional<Physiology> read(ServerPlayer p){
        if(p==null||!p.serverLevel().getServer().isSameThread()||!WorldLogicBridge.active(p))return Optional.empty();
        var api=Endpoint.API;if(api==null)return Optional.empty();
        try{
            var snapshot=(Optional<?>)api.read().invoke(null,p);
            if(snapshot.isEmpty())return Optional.empty();
            Object value=snapshot.get();
            double water=((Number)api.water().invoke(value)).doubleValue(),electrolytes=((Number)api.electrolytes().invoke(value)).doubleValue();
            String stage=(String)api.stage().invoke(value);
            if(!Double.isFinite(water)||water<0||water>100||!Double.isFinite(electrolytes)||electrolytes<0||electrolytes>100||!java.util.Set.of("NORMAL","MILD","MODERATE","SEVERE","CRITICAL").contains(stage))return Optional.empty();
            return Optional.of(new Physiology(water,electrolytes,stage));
        }catch(ReflectiveOperationException|LinkageError|RuntimeException ex){
            if(!warned){warned=true;LOG.warn("Hydration physiology read failed",ex);}
            return Optional.empty();
        }
    }
}
