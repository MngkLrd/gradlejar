package dev.minemods.traumacraft.network;
import dev.minemods.traumacraft.client.ClientBodyState;
import dev.minemods.traumacraft.client.ClientTreatmentState;
import dev.minemods.traumacraft.health.BodyStates;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.*;
public final class ModNetwork {
    private static final SimpleChannel CHANNEL=ChannelBuilder.named("traumacraft:body").networkProtocolVersion(3).simpleChannel();
    private ModNetwork(){}
    public static void init() {
        CHANNEL.messageBuilder(SyncBodyStatePacket.class,NetworkDirection.PLAY_TO_CLIENT)
            .encoder(SyncBodyStatePacket::encode).decoder(SyncBodyStatePacket::decode)
            .consumerMainThread((m,c)->{if(FMLEnvironment.dist==Dist.CLIENT)ClientBodyState.accept(m);}).add();
        CHANNEL.messageBuilder(TreatmentProgressPacket.class,NetworkDirection.PLAY_TO_CLIENT)
            .encoder((m,b)->{b.writeVarInt(m.durationTicks());b.writeVarInt(m.elapsedTicks());})
            .decoder(b->new TreatmentProgressPacket(b.readVarInt(),b.readVarInt()))
            .consumerMainThread((m,c)->{if(FMLEnvironment.dist==Dist.CLIENT)ClientTreatmentState.accept(m);}).add();
        CHANNEL.messageBuilder(ApplyMedicalPacket.class,NetworkDirection.PLAY_TO_SERVER)
            .encoder((m,b)->b.writeVarInt(m.bodyPartOrdinal())).decoder(b->new ApplyMedicalPacket(b.readVarInt()))
            .consumerMainThread((m,c)->ApplyMedicalPacket.handle(m,c.getSender())).add();
        CHANNEL.build();
    }
    public static void request(ApplyMedicalPacket p){CHANNEL.send(p,PacketDistributor.SERVER.noArg());}
    public static void progress(ServerPlayer p,int duration,int elapsed){
        if(p.connection!=null) CHANNEL.send(new TreatmentProgressPacket(duration,elapsed),PacketDistributor.PLAYER.with(p));
    }
    public static void sync(ServerPlayer p){
        BodyStates.flush(p);
        if(p.connection!=null) CHANNEL.send(SyncBodyStatePacket.from(BodyStates.get(p)),PacketDistributor.PLAYER.with(p));
    }
}
