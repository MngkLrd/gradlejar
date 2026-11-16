package dev.bearback;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.animal.polarbear.PolarBear;
import net.minecraftforge.network.Channel;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.NetworkProtocol;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public final class BearNetwork {
    private BearNetwork(){}
    private static Channel<CustomPacketPayload> channel;
    public static void register(){
        channel=ChannelBuilder.named(Identifier.fromNamespaceAndPath("bearback","main"))
             .payloadChannel()
             .protocol(NetworkProtocol.PLAY)
             .serverbound()
                .add(Ride.TYPE,Ride.CODEC,(packet,context)->{
                    context.setPacketHandled(true);
                    context.enqueueWork(()->{
                        ServerPlayer player=context.getSender();
                        if(player!=null)BearEvents.ride(player,packet.forward,packet.strafe,packet.jump,packet.attack,packet.yaw);
                    });
                })
                .add(OpenCargo.TYPE,OpenCargo.CODEC,(packet,context)->{
                    context.setPacketHandled(true);
                    context.enqueueWork(()->{
                        ServerPlayer player=context.getSender();
                        if(player!=null&&player.getVehicle() instanceof PolarBear bear&&bear.getId()==packet.bearEntityId)
                            BearCargo.open(player,bear);
                    });
                })
             .clientbound()
                .add(Saddle.TYPE,Saddle.CODEC,(packet,context)->{
                    context.setPacketHandled(true);
                    context.enqueueWork(()->dev.bearback.client.ClientHooks.syncEquipment(
                            packet.id,packet.saddled,packet.bags));
                })
             .build();
    }
    public static void openCargo(int entityId){
        channel.send(new OpenCargo(entityId),PacketDistributor.SERVER.noArg());
    }
    public static void ride(float forward,float strafe,boolean jump,boolean attack,float yaw){
        channel.send(new Ride(forward,strafe,jump,attack,yaw),PacketDistributor.SERVER.noArg());
    }
    public static void sync(PolarBear bear){
        if(!bear.level().isClientSide())
            for(ServerPlayer player:((net.minecraft.server.level.ServerLevel)bear.level()).players())
                if(player.distanceToSqr(bear)<16384)syncTo(bear,player);
    }
    public static void syncTo(PolarBear bear,ServerPlayer player){
        channel.send(new Saddle(bear.getId(),BearEvents.isSaddled(bear),BearEvents.hasBags(bear)),
                PacketDistributor.PLAYER.with(player));
    }
    public record Ride(float forward,float strafe,boolean jump,boolean attack,float yaw) implements CustomPacketPayload{
        public static final Type<Ride> TYPE=new Type<>(Identifier.fromNamespaceAndPath(Bearback.ID,"ride"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Ride> CODEC=StreamCodec.of(
            (buf,p)->{buf.writeFloat(p.forward);buf.writeFloat(p.strafe);buf.writeBoolean(p.jump);buf.writeBoolean(p.attack);buf.writeFloat(p.yaw);},
            buf->new Ride(buf.readFloat(),buf.readFloat(),buf.readBoolean(),buf.readBoolean(),buf.readFloat()));
        @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    public record OpenCargo(int bearEntityId) implements CustomPacketPayload{
        public static final Type<OpenCargo> TYPE=new Type<>(Identifier.fromNamespaceAndPath(Bearback.ID,"open_cargo"));
        public static final StreamCodec<RegistryFriendlyByteBuf,OpenCargo> CODEC=StreamCodec.of(
            (buf,p)->buf.writeVarInt(p.bearEntityId),buf->new OpenCargo(buf.readVarInt()));
        @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    public record Saddle(int id,boolean saddled,boolean bags) implements CustomPacketPayload{
        public static final Type<Saddle> TYPE=new Type<>(Identifier.fromNamespaceAndPath(Bearback.ID,"saddle"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Saddle> CODEC=StreamCodec.of(
            (buf,p)->{buf.writeVarInt(p.id);buf.writeBoolean(p.saddled);buf.writeBoolean(p.bags);},
            buf->new Saddle(buf.readVarInt(),buf.readBoolean(),buf.readBoolean()));
        @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
}
