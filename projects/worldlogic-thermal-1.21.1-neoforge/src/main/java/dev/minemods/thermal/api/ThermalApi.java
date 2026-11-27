package dev.minemods.thermal.api;
import dev.minemods.thermal.ThermalCondition;
import net.minecraft.server.level.ServerPlayer;
import java.util.Optional;
/** Optional addon contract. Read on server thread; no mutation or private core/Trauma access. */
public final class ThermalApi {
    public static final int API_VERSION=1;
    public record Status(double bodyC,ThermalCondition.Kind condition,int coldSeconds,int hotSeconds,int sunSeconds){}
    private ThermalApi(){}
    /** Latest server-sampled indoor climate; empty outdoors, in creative, or when stale. */
    public static Optional<dev.minemods.thermal.RoomReading> roomClimate(ServerPlayer p){
        if(!p.serverLevel().getServer().isSameThread())throw new IllegalStateException("Thermal API requires server thread");
        if(!p.isAlive()||p.isCreative()||p.isSpectator())return Optional.empty();
        var root=p.getPersistentData().getCompound("worldlogic_thermal");if(!root.contains("room"))return Optional.empty();
        var n=root.getCompound("room");long age=p.serverLevel().getGameTime()-n.getLong("tick");if(age<0||age>40||!n.getString("dimension").equals(p.serverLevel().dimension().location().toString()))return Optional.empty();
        return Optional.of(new dev.minemods.thermal.RoomReading(true,n.getDouble("air"),n.getDouble("humidity"),n.getDouble("ventilation"),n.getDouble("mass"),n.getInt("volume")));
    }
    public static Optional<Status> status(ServerPlayer p){
        if(!p.serverLevel().getServer().isSameThread()) throw new IllegalStateException("Thermal API requires server thread");
        if(!p.isAlive()||p.isCreative()||p.isSpectator())return Optional.empty();
        var n=p.getPersistentData().getCompound("worldlogic_thermal");
        if(!n.contains("body"))return Optional.empty();
        return Optional.of(new Status(n.getDouble("body"),ThermalCondition.parse(n.getString("condition")),n.getInt("coldSeconds"),n.getInt("hotSeconds"),n.getInt("sunSeconds")));
    }
}
