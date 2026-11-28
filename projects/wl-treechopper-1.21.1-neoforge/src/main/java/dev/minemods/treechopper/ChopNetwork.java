package dev.minemods.treechopper;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
final class ChopNetwork {
    record Strike(BlockPos pos) implements CustomPacketPayload {
        static final Type<Strike> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath(TreeChopper.ID,"strike"));
        static final StreamCodec<ByteBuf,Strike> CODEC=StreamCodec.composite(BlockPos.STREAM_CODEC,Strike::pos,Strike::new);
        @Override public Type<Strike> type(){return TYPE;}
    }
    record Swing(int entityId) implements CustomPacketPayload {
        static final Type<Swing> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath(TreeChopper.ID,"swing"));
        static final StreamCodec<ByteBuf,Swing> CODEC=StreamCodec.composite(ByteBufCodecs.VAR_INT,Swing::entityId,Swing::new);
        @Override public Type<Swing> type(){return TYPE;}
    }
    static void register(RegisterPayloadHandlersEvent event){
        event.registrar("2").playToServer(Strike.TYPE,Strike.CODEC,(message,context)->{
            if(context.player() instanceof ServerPlayer player)TreeChopper.networkStrike(player,message.pos());
        }).playToClient(Swing.TYPE,Swing.CODEC,(message,context)->{
            var entity=context.player().level().getEntity(message.entityId());
            if(entity instanceof ChopSwing swing)swing.wl$markSwing();
        });
    }
}
