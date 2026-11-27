package dev.minemods.traumacraft.network;
import dev.minemods.traumacraft.client.ClientBodyState;
import dev.minemods.traumacraft.client.ClientTreatmentState;
import dev.minemods.traumacraft.health.BodyStates;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
public final class ModNetwork {
    private ModNetwork(){}
    public static void register(RegisterPayloadHandlersEvent event){
        var registrar=event.registrar("5");
        registrar.playToClient(SyncBodyStatePacket.TYPE,SyncBodyStatePacket.STREAM_CODEC,(m,c)->{
            if(FMLEnvironment.dist==Dist.CLIENT)ClientBodyState.accept(m);
        });
        registrar.playToClient(TreatmentProgressPacket.TYPE,TreatmentProgressPacket.CODEC,(m,c)->{
            if(FMLEnvironment.dist==Dist.CLIENT)ClientTreatmentState.accept(m);
        });
        registrar.playToServer(ApplyMedicalPacket.TYPE,ApplyMedicalPacket.STREAM_CODEC,ApplyMedicalPacket::handle);
    }
    public static void request(ApplyMedicalPacket p){PacketDistributor.sendToServer(p);}
    public static void progress(ServerPlayer p,int duration,int elapsed){
        if(!(p instanceof FakePlayer)&&p.connection!=null)PacketDistributor.sendToPlayer(p,new TreatmentProgressPacket(duration,elapsed));
    }
    public static void sync(ServerPlayer p){
        BodyStates.flush(p);
        if(!(p instanceof FakePlayer)&&p.connection!=null)PacketDistributor.sendToPlayer(p,SyncBodyStatePacket.from(BodyStates.get(p),dev.minemods.traumacraft.integration.ThermalBridge.condition(p).wireId));
    }
}
