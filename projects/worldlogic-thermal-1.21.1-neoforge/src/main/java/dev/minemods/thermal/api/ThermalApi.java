package dev.minemods.thermal.api;
import dev.minemods.thermal.ThermalCondition;
import net.minecraft.server.level.ServerPlayer;
import java.util.Optional;
/** Optional addon contract. Read on server thread; no mutation or private core/Trauma access. */
public final class ThermalApi {
    public static final int API_VERSION=1;
    public static final int API_REVISION=2;
    public record Physiology(dev.minemods.thermal.PhysiologyInputs inputs,double nutritionFactor,double coolingCapacity){}
    /** Last completed server thermal sample; empty before update, across dimensions or after 40 ticks. No resource mutation. */
    public static Optional<Physiology> physiology(ServerPlayer p){
        if(!p.serverLevel().getServer().isSameThread())throw new IllegalStateException("Thermal API requires server thread");
        if(!p.isAlive()||p.isCreative()||p.isSpectator())return Optional.empty();
        var root=p.getPersistentData().getCompound("worldlogic_thermal");if(!root.contains("physiology"))return Optional.empty();
        var n=root.getCompound("physiology");long age=p.serverLevel().getGameTime()-n.getLong("tick");
        if(age<0||age>40||!n.getString("dimension").equals(p.serverLevel().dimension().location().toString()))return Optional.empty();
        return Optional.of(new Physiology(new dev.minemods.thermal.PhysiologyInputs(n.getBoolean("metabolism"),n.getBoolean("hydration"),n.getBoolean("trauma"),n.getDouble("energy"),n.getDouble("diet"),n.getDouble("water"),n.getDouble("salts"),n.getDouble("blood")),n.getDouble("nutrition"),n.getDouble("cooling")));
    }
    /** Additive room contract; legacy API_VERSION remains compatible. */
    public static final int ROOM_API_VERSION=1;
    public record Status(double bodyC,ThermalCondition.Kind condition,int coldSeconds,int hotSeconds,int sunSeconds){}
    private ThermalApi(){}
    /** Current modeled room at a loaded coordinate, independent of player presence.
     * Server thread only; no forced chunk loads. Empty for outdoors, unknown Core weather,
     * or unsupported/unbounded geometry. Reading advances the shared room simulation at most
     * once per second (not per API call). Temperature in Celsius, RH percent, ventilation 0..1,
     * volume in interior blocks. The result is a snapshot, not mutable state.
     */
    public static Optional<dev.minemods.thermal.RoomReading> roomClimate(net.minecraft.server.level.ServerLevel level,net.minecraft.core.BlockPos pos){
        return dev.minemods.thermal.server.RoomHeat.query(level,pos);
    }
    /** Current modeled indoor climate at a living survival player's position. Empty outdoors. */
    public static Optional<dev.minemods.thermal.RoomReading> roomClimate(ServerPlayer p){
        if(!p.serverLevel().getServer().isSameThread())throw new IllegalStateException("Thermal API requires server thread");
        if(!p.isAlive()||p.isCreative()||p.isSpectator())return Optional.empty();
        return roomClimate(p.serverLevel(),p.blockPosition());
    }
    public static Optional<Status> status(ServerPlayer p){
        if(!p.serverLevel().getServer().isSameThread()) throw new IllegalStateException("Thermal API requires server thread");
        if(!p.isAlive()||p.isCreative()||p.isSpectator())return Optional.empty();
        var n=p.getPersistentData().getCompound("worldlogic_thermal");
        if(!n.contains("body")||!n.contains("sampleTick"))return Optional.empty();
        long age=p.serverLevel().getGameTime()-n.getLong("sampleTick");
        if(age<0||age>40||!n.getString("sampleDimension").equals(p.serverLevel().dimension().location().toString())||!Double.isFinite(n.getDouble("body")))return Optional.empty();
        return Optional.of(new Status(n.getDouble("body"),ThermalCondition.parse(n.getString("condition")),n.getInt("coldSeconds"),n.getInt("hotSeconds"),n.getInt("sunSeconds")));
    }
}
