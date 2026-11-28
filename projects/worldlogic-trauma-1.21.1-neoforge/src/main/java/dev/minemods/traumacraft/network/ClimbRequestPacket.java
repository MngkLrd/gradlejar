package dev.minemods.traumacraft.network;

import dev.minemods.traumacraft.TraumaCraft;
import dev.minemods.traumacraft.climb.ClimbManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ClimbRequestPacket() implements CustomPacketPayload {
    public static final Type<ClimbRequestPacket> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath(TraumaCraft.MOD_ID,"climb_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf,ClimbRequestPacket> CODEC=StreamCodec.of((buf,m)->{},buf->new ClimbRequestPacket());
    @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    public static void handle(ClimbRequestPacket packet,IPayloadContext context){
        if(context.player() instanceof ServerPlayer p)ClimbManager.request(p);
    }
}
