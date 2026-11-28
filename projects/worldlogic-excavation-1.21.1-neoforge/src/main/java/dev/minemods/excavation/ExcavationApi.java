package dev.minemods.excavation;
import java.util.Optional;
import dev.minemods.worldlogic.api.WorldLogicCapabilities;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/** Public server-thread contract. Face order is DOWN, UP, NORTH, SOUTH, WEST, EAST. */
public final class ExcavationApi {
    public static final int REVISION=1;
    private ExcavationApi(){}
    public enum Status { ACCEPTED, BROKEN, NOT_TARGET, WRONG_TOOL, COOLDOWN, DENIED, CORE_UNAVAILABLE, INVALID_STATE }
    public record Snapshot(ResourceLocation source,int work,int depthKey,int remainingOreUnits,double incidentEnergyPerAreaJm2){}
    public record Result(Status status,int emittedOreUnits,Optional<Snapshot> remaining){}
    public interface Contract{
        Optional<Snapshot> inspect(ServerLevel level,BlockPos pos);
        Result strike(ServerPlayer player,BlockPos pos);
        default Optional<FractureMath.Timing> timing(ServerPlayer player,BlockPos pos){return Optional.empty();}
    }
    public static final WorldLogicCapabilities.Key<Contract> KEY=new WorldLogicCapabilities.Key<>(ResourceLocation.fromNamespaceAndPath(Excavation.ID,"fracture"),1,Contract.class);
    static void register(){WorldLogicCapabilities.register(KEY,REVISION,Excavation.ID,new Contract(){
        public Optional<Snapshot> inspect(ServerLevel l,BlockPos p){return Excavation.inspect(l,p);}
        public Result strike(ServerPlayer player,BlockPos pos){return Excavation.strike(player,pos);}
        public Optional<FractureMath.Timing> timing(ServerPlayer player,BlockPos pos){return Excavation.timing(player,pos);}
    });}
}
