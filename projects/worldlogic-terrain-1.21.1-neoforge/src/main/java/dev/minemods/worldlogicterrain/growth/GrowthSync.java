package dev.minemods.worldlogicterrain.growth;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** Bounded server raycast, no client-supplied location or arbitrary environment queries. */
public final class GrowthSync {
    private GrowthSync(){}
    public record State(String dimension,BlockPos pos,int progress,int speed,int quality,String status,String season,int temperature,int moisture) implements CustomPacketPayload{
        public static final Type<State> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("worldlogicterrain","growth"));
        @Override public Type<State> type(){return TYPE;}
    }
    public static final StreamCodec<RegistryFriendlyByteBuf,State> CODEC=StreamCodec.of((b,s)->{
        b.writeUtf(s.dimension(),256);b.writeBlockPos(s.pos());b.writeVarInt(s.progress());b.writeVarInt(s.speed());b.writeVarInt(s.quality());
        b.writeUtf(s.status(),24);b.writeUtf(s.season(),16);b.writeVarInt(s.temperature());b.writeVarInt(s.moisture());
    },b->new State(b.readUtf(256),b.readBlockPos(),b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readUtf(24),b.readUtf(16),b.readVarInt(),b.readVarInt()));
    private static volatile State client;
    private static volatile long received;
    public static State client(){return System.nanoTime()-received<1_500_000_000L?client:null;}
    public static void register(RegisterPayloadHandlersEvent e){
        e.registrar("1").playToClient(State.TYPE,CODEC,(s,ctx)->{client=s;received=System.nanoTime();});
    }
    public static void tick(PlayerTickEvent.Post e){
        if(!(e.getEntity() instanceof ServerPlayer p)||p instanceof FakePlayer||p.isSpectator()||p.tickCount%10!=0)return;
        double reach=Math.min(6,p.blockInteractionRange());
        var hit=p.pick(reach,1.0f,false);
        if(!(hit instanceof BlockHitResult b)||b.getType()!=HitResult.Type.BLOCK)return;
        var l=p.serverLevel();var pos=b.getBlockPos();
        if(!l.hasChunkAt(pos))return;
        var v=SeasonalGrowth.view(l,pos,l.getBlockState(pos));if(v==null)return;
        PacketDistributor.sendToPlayer(p,new State(l.dimension().location().toString(),pos,v.progress(),v.speed(),v.quality(),v.status(),v.season(),v.temperature(),v.moisture()));
    }
}
