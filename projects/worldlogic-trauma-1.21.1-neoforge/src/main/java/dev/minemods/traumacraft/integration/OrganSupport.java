package dev.minemods.traumacraft.integration;
import net.minecraft.server.level.ServerPlayer;
/** Read-only foreign inputs. No writes to temperature, nutrient, salt or water stores. */
public record OrganSupport(float circulation,float repair){
    public static final OrganSupport NEUTRAL=new OrganSupport(1,1);
    public static OrganSupport read(ServerPlayer p,ThermalCondition thermal,MetabolismBridge.Factors nutrition){
        var hydration=HydrationBridge.read(p);
        if(hydration.isEmpty())return factors(100,50,thermal.recoveryFactor,nutrition.tissue());
        var h=hydration.get();return factors(h.hydration(),h.electrolytes(),thermal.recoveryFactor,nutrition.tissue());
    }
    public static OrganSupport factors(double hydration,double electrolytes,float thermal,float tissue){
        if(!Double.isFinite(hydration)||!Double.isFinite(electrolytes)||!Float.isFinite(thermal)||!Float.isFinite(tissue))return NEUTRAL;
        float water=(float)Math.max(0,Math.min(1,hydration/100)),salts=(float)Math.max(0,1-Math.abs(electrolytes-50)/50);
        float temp=Math.max(0,Math.min(1,thermal)),nutrition=Math.max(0,Math.min(1,tissue));
        return new OrganSupport((.70f+.30f*water)*(.85f+.15f*salts)*(.85f+.15f*temp),(.35f+.65f*water)*(.65f+.35f*salts)*temp*nutrition);
    }
}
