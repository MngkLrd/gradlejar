package dev.minemods.traumacraft.network;
import dev.minemods.traumacraft.client.ClientBodyState;
import dev.minemods.traumacraft.health.BodyStates;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
public final class ModNetwork {
    private ModNetwork(){}
    public static void register(RegisterPayloadHandlersEvent e){
        var reg=e.registrar("1");
        reg.playToServer(ApplyMedicalPacket.TYPE,ApplyMedicalPacket.STREAM_CODEC,
          (p,ctx)->ctx.enqueueWork(()->ApplyMedicalPacket.handle(p,ctx)));
        reg.playToClient(SyncBodyStatePacket.TYPE,SyncBodyStatePacket.STREAM_CODEC,
          (p,ctx)->ctx.enqueueWork(()->ClientBodyState.accept(p.hp(),p.bleeding(),p.fracture())));
    }
    public static void sync(ServerPlayer player){
        PacketDistributor.sendToPlayer(player,SyncBodyStatePacket.from(BodyStates.get(player)));
    }
}
