package dev.bearback;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.animal.polarbear.PolarBear;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
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
                dev.bearback.client.ClientHooks.syncEquipment(payload.id,payload.saddled,payload.bags));
        registrar.playToServer(OpenCargo.TYPE,OpenCargo.CODEC,(payload,context)->{
            if(context.player() instanceof ServerPlayer player
                    &&player.getVehicle() instanceof PolarBear bear
                    &&bear.getId()==payload.bearEntityId)BearCargo.open(player,bear);
        });
    }
    public static void openCargo(int entityId){
        ClientPacketDistributor.sendToServer(new OpenCargo(entityId));
    }
    public static void ride(float forward,float strafe,boolean jump,boolean attack,float yaw){
        ClientPacketDistributor.sendToServer(new Ride(forward,strafe,jump,attack,yaw));
    }
    public static void sync(PolarBear bear){
        if(!bear.level().isClientSide())
            PacketDistributor.sendToPlayersTrackingEntityAndSelf(bear,new Saddle(bear.getId(),BearEvents.isSaddled(bear),BearEvents.hasBags(bear)));
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
    public record OpenCargo(int bearEntityId) implements CustomPacketPayload{
        public static final Type<OpenCargo> TYPE=new Type<>(Identifier.fromNamespaceAndPath(Bearback.MOD_ID,"open_cargo"));
        public static final StreamCodec<FriendlyByteBuf,OpenCargo> CODEC=StreamCodec.of(
            (buf,p)->buf.writeVarInt(p.bearEntityId),buf->new OpenCargo(buf.readVarInt()));
        @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    public record Saddle(int id,boolean saddled,boolean bags) implements CustomPacketPayload{
        public static final Type<Saddle> TYPE=new Type<>(Identifier.fromNamespaceAndPath(Bearback.MOD_ID,"saddle"));
        public static final StreamCodec<FriendlyByteBuf,Saddle> CODEC=StreamCodec.of(
            (buf,p)->{buf.writeVarInt(p.id);buf.writeBoolean(p.saddled);buf.writeBoolean(p.bags);},
            buf->new Saddle(buf.readVarInt(),buf.readBoolean(),buf.readBoolean()));
        @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
}
