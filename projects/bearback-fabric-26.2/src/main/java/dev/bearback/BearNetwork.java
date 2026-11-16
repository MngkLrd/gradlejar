package dev.bearback;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.animal.PolarBear;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public final class BearNetwork {
 private BearNetwork(){}
 public static void register(){
   PayloadTypeRegistry.playC2S().register(Ride.TYPE,Ride.CODEC);
   PayloadTypeRegistry.playC2S().register(OpenCargo.TYPE,OpenCargo.CODEC);
   PayloadTypeRegistry.playS2C().register(Equipment.TYPE,Equipment.CODEC);
   ServerPlayNetworking.registerGlobalReceiver(Ride.TYPE,(packet,context)->
           context.server().execute(()->BearEvents.ride(context.player(),packet.f,packet.s,packet.jump,packet.attack,packet.yaw)));
   ServerPlayNetworking.registerGlobalReceiver(OpenCargo.TYPE,(packet,context)->
           context.server().execute(()->{
               ServerPlayer p=context.player();
               if(p.getVehicle() instanceof PolarBear bear&&bear.getId()==packet.id)BearCargo.open(p,bear);
           }));
 }
 public static void sync(PolarBear bear){
   if(bear.level().isClientSide())return;
   for(ServerPlayer p:((net.minecraft.server.level.ServerLevel)bear.level()).players())
       if(p.distanceToSqr(bear)<16384)syncTo(bear,p);
 }
 public static void syncTo(PolarBear bear,ServerPlayer p){
   ServerPlayNetworking.send(p,new Equipment(bear.getId(),BearData.saddled(bear),BearData.bags(bear)));
 }
 public record Ride(float f,float s,boolean jump,boolean attack,float yaw) implements CustomPacketPayload{
   public static final Type<Ride> TYPE=new Type<>(Identifier.fromNamespaceAndPath("bearback","ride"));
   public static final StreamCodec<FriendlyByteBuf,Ride> CODEC=StreamCodec.composite(
      ByteBufCodecs.FLOAT,Ride::f,ByteBufCodecs.FLOAT,Ride::s,
      ByteBufCodecs.BOOL,Ride::jump,ByteBufCodecs.BOOL,Ride::attack,
      ByteBufCodecs.FLOAT,Ride::yaw,Ride::new);
   @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
 }
 public record OpenCargo(int id) implements CustomPacketPayload{
   public static final Type<OpenCargo> TYPE=new Type<>(Identifier.fromNamespaceAndPath("bearback","open_cargo"));
   public static final StreamCodec<FriendlyByteBuf,OpenCargo> CODEC=
       StreamCodec.composite(ByteBufCodecs.VAR_INT,OpenCargo::id,OpenCargo::new);
   @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
 }
 public record Equipment(int id,boolean saddle,boolean bags) implements CustomPacketPayload{
   public static final Type<Equipment> TYPE=new Type<>(Identifier.fromNamespaceAndPath("bearback","equipment"));
   public static final StreamCodec<FriendlyByteBuf,Equipment> CODEC=StreamCodec.composite(
       ByteBufCodecs.VAR_INT,Equipment::id,ByteBufCodecs.BOOL,Equipment::saddle,
       ByteBufCodecs.BOOL,Equipment::bags,Equipment::new);
   @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
 }
}
