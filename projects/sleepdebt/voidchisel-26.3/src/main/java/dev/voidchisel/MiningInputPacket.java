package dev.voidchisel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
public record MiningInputPacket(long pos, boolean active) implements CustomPacketPayload {
    public static final Type<MiningInputPacket> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(VoidChisel.MOD_ID, "mining_input"));
    public static final StreamCodec<FriendlyByteBuf, MiningInputPacket> CODEC = StreamCodec.of(
            (buf, p) -> {buf.writeVarLong(p.pos);buf.writeBoolean(p.active);},
            buf -> new MiningInputPacket(buf.readVarLong(), buf.readBoolean()));
    @Override public Type<? extends CustomPacketPayload> type() {return TYPE;}
    public static void receive(MiningInputPacket p, IPayloadContext ctx) {
        if (ctx.player() instanceof ServerPlayer player)
            BedrockMiningServer.receive(player, p.active ? BlockPos.of(p.pos) : null);
    }
}
