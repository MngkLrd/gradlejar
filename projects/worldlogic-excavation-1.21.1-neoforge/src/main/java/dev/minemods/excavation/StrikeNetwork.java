package dev.minemods.excavation;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
final class StrikeNetwork {
    record Strike(BlockPos pos) implements CustomPacketPayload{
        static final Type<Strike> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath(Excavation.ID,"strike"));
        static final StreamCodec<ByteBuf,Strike> CODEC=StreamCodec.composite(BlockPos.STREAM_CODEC,Strike::pos,Strike::new);
        @Override public Type<Strike> type(){return TYPE;}
    }
    static void register(RegisterPayloadHandlersEvent event){
        event.registrar("1").playToServer(Strike.TYPE,Strike.CODEC,(message,context)->{
            if(context.player() instanceof ServerPlayer player)Excavation.strike(player,message.pos());
        });
    }
}
