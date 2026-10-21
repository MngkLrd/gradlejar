package dev.bearback;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.animal.polarbear.PolarBear;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class BearNetwork {
    private BearNetwork(){}
    public static void register(RegisterPayloadHandlersEvent e){
        var registrar=e.registrar("1");
        registrar.playToServer(Ride.TYPE,Ride.CODEC,(payload,context)->{
            if(context.player() instanceof ServerPlayer player)
                BearEvents.ride(player,payload.forward,payload.strafe,payload.jump,payload.attack,payload.yaw);
        });
        registrar.playToClient(Saddle.TYPE,Saddle.CODEC,(payload,context)->
                dev.bearback.client.ClientHooks.syncSaddle(payload.id,payload.saddled));
    }
    public static void ride(float forward,float strafe,boolean jump,boolean attack,float yaw){
        PacketDistributor.sendToServer(new Ride(forward,strafe,jump,attack,yaw));
    }
    public static void sync(PolarBear bear){
        if(!bear.level().isClientSide())
            PacketDistributor.sendToPlayersTrackingEntityAndSelf(bear,new Saddle(bear.getId(),BearEvents.isSaddled(bear)));
    }
    public static void syncTo(PolarBear bear,ServerPlayer player){
        PacketDistributor.sendToPlayer(player,new Saddle(bear.getId(),BearEvents.isSaddled(bear)));
    }
    public record Ride(float forward,float strafe,boolean jump,boolean attack,float yaw) implements CustomPacketPayload{
        public static final Type<Ride> TYPE=new Type<>(Identifier.fromNamespaceAndPath(Bearback.MOD_ID,"ride"));
        public static final StreamCodec<FriendlyByteBuf,Ride> CODEC=StreamCodec.of(
            (buf,p)->{buf.writeFloat(p.forward);buf.writeFloat(p.strafe);buf.writeBoolean(p.jump);buf.writeBoolean(p.attack);buf.writeFloat(p.yaw);},
            buf->new Ride(buf.readFloat(),buf.readFloat(),buf.readBoolean(),buf.readBoolean(),buf.readFloat()));
        @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    public record Saddle(int id,boolean saddled) implements CustomPacketPayload{
        public static final Type<Saddle> TYPE=new Type<>(Identifier.fromNamespaceAndPath(Bearback.MOD_ID,"saddle"));
        public static final StreamCodec<FriendlyByteBuf,Saddle> CODEC=StreamCodec.of(
            (buf,p)->{buf.writeVarInt(p.id);buf.writeBoolean(p.saddled);},
            buf->new Saddle(buf.readVarInt(),buf.readBoolean()));
        @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
}
