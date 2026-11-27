package dev.minemods.thermal.network;
import dev.minemods.thermal.ThermalHistory;
import dev.minemods.thermal.ThermalHistory.Point;
import java.util.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
final public class HistorySync {
    public record Journal(int entityId,List<Point> points) implements CustomPacketPayload {
        static final Type<Journal> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("worldlogic_thermal","journal"));
        @Override public Type<Journal> type(){return TYPE;}
        static final StreamCodec<RegistryFriendlyByteBuf,Journal> CODEC=StreamCodec.of((b,j)->{
            b.writeVarInt(j.entityId());b.writeVarInt(j.points().size());
            for(Point p:j.points()){
                b.writeLong(p.tick());b.writeLong(p.time());b.writeUtf(p.dimension(),256);b.writeUtf(p.biome(),256);
                b.writeInt(p.x());b.writeInt(p.y());b.writeInt(p.z());b.writeFloat(p.air());b.writeFloat(p.felt());b.writeFloat(p.body());
            }
        },b->{
            int id=b.readVarInt(),count=b.readVarInt();
            if(count<0||count>ThermalHistory.LIMIT)throw new IllegalArgumentException("Invalid thermal journal length");
            var list=new ArrayList<Point>(count);
            for(int i=0;i<count;i++){
                var p=new Point(b.readLong(),b.readLong(),b.readUtf(256),b.readUtf(256),b.readInt(),b.readInt(),b.readInt(),b.readFloat(),b.readFloat(),b.readFloat());
                if(!ThermalHistory.valid(p))throw new IllegalArgumentException("Invalid thermal journal point");
                list.add(p);
            }
            return new Journal(id,List.copyOf(list));
        });
    }
    private static volatile Journal latest;
    private HistorySync(){}
    static void register(RegisterPayloadHandlersEvent event){event.registrar("4").playToClient(Journal.TYPE,Journal.CODEC,(m,c)->latest=m);}
    public static void send(ServerPlayer p,List<Point> points){
        if(!(p instanceof net.neoforged.neoforge.common.util.FakePlayer))PacketDistributor.sendToPlayer(p,new Journal(p.getId(),points));
    }
    public static List<Point> current(int id){Journal j=latest;return j!=null&&j.entityId()==id?j.points():List.of();}
    static void clear(){latest=null;}
}
