package dev.bearback;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.animal.PolarBear;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class BearNetwork {
    private static final String VERSION = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(Bearback.MOD_ID, "control"),
            () -> VERSION, VERSION::equals, VERSION::equals);

    private BearNetwork() {}
    public static void init() {
        CHANNEL.registerMessage(0, Ride.class,
                (m,b)->{b.writeFloat(m.forward);b.writeFloat(m.strafe);b.writeBoolean(m.jump);b.writeBoolean(m.attack);b.writeFloat(m.yaw);},
                b->new Ride(b.readFloat(),b.readFloat(),b.readBoolean(),b.readBoolean(),b.readFloat()),
                (m,s)->{
                    NetworkEvent.Context ctx=s.get();
                    ServerPlayer player=ctx.getSender();
                    if(player!=null)ctx.enqueueWork(()->BearEvents.ride(player,m.forward,m.strafe,m.jump,m.attack,m.yaw));
                    ctx.setPacketHandled(true);
                });
        CHANNEL.registerMessage(1, Equipment.class,
                (m,b)->{b.writeVarInt(m.id);b.writeBoolean(m.saddled);b.writeBoolean(m.bags);},
                b->new Equipment(b.readVarInt(),b.readBoolean(),b.readBoolean()),
                (m,s)->{
                    NetworkEvent.Context ctx=s.get();
                    ctx.enqueueWork(()->net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(
                        net.minecraftforge.api.distmarker.Dist.CLIENT,()->()->dev.bearback.client.ClientHooks.syncEquipment(m.id,m.saddled,m.bags)));
                    ctx.setPacketHandled(true);
                });
        CHANNEL.registerMessage(2, OpenCargo.class,
                (m,b)->b.writeVarInt(m.entityId),
                b->new OpenCargo(b.readVarInt()),
                (m,s)->{
                    NetworkEvent.Context context=s.get();
                    ServerPlayer player=context.getSender();
                    if(player!=null)context.enqueueWork(()->{
                        if(player.getVehicle() instanceof PolarBear bear && bear.getId()==m.entityId)
                            BearCargo.open(player,bear);
                    });
                    context.setPacketHandled(true);
                });
    }
    public static void openCargo(int entityId) {
        CHANNEL.sendToServer(new OpenCargo(entityId));
    }
    public static void ride(float forward,float strafe,boolean jump,boolean attack,float yaw){
        CHANNEL.sendToServer(new Ride(forward,strafe,jump,attack,yaw));
    }
    public static void sync(PolarBear bear) {
        if(!bear.level().isClientSide)
            CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(()->bear),
                    new Equipment(bear.getId(),BearEvents.isSaddled(bear),BearEvents.hasBags(bear)));
    }
    public static void syncTo(PolarBear bear,ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(()->player),
                new Equipment(bear.getId(),BearEvents.isSaddled(bear),BearEvents.hasBags(bear)));
    }
    public record Ride(float forward,float strafe,boolean jump,boolean attack,float yaw){}
    public record Equipment(int id,boolean saddled,boolean bags){}
    public record OpenCargo(int entityId){}
}
