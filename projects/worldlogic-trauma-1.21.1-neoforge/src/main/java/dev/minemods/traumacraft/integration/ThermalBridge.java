package dev.minemods.traumacraft.integration;

import dev.minemods.traumacraft.health.BodyState;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;

/** Optional read-only adapter for Thermal 0.3.0-rc1.
 * ThermalEvents.KEY writes stage/body on the server. No core NBT or thermal state mutation.
 * Without Thermal, all recovery and UI behavior are unchanged.
 */
public final class ThermalBridge {
    public static final String MOD_ID="worldlogic_thermal";
    private ThermalBridge(){}
    public static ThermalCondition condition(ServerPlayer p){
        if(!ModList.get().isLoaded(MOD_ID)||!WorldLogicBridge.active(p))return ThermalCondition.NONE;
        var root=p.getPersistentData();
        if(!root.contains(MOD_ID,Tag.TAG_COMPOUND))return ThermalCondition.NONE;
        var n=root.getCompound(MOD_ID);
        if(!n.contains("body",Tag.TAG_ANY_NUMERIC)||!n.contains("stage",Tag.TAG_STRING))return ThermalCondition.NONE;
        return ThermalCondition.read(true,true,n.getString("stage"),n.getDouble("body"));
    }
    /** Only Trauma's natural repair is affected; no second movement, damage or fatigue penalty. */
    public static void recover(BodyState b,ThermalCondition s,boolean healTissue){
        int ticks=s.recoveryTicks(20);
        if(ticks>0)b.recoverBones(ticks);
        if(healTissue&&s.recoveryFactor>0&&!b.hasBleeding())b.healMostDamaged(.75f*s.recoveryFactor);
    }
}
