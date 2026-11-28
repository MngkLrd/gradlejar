package dev.minemods.traumacraft.network;
import dev.minemods.traumacraft.TraumaCraft;
import dev.minemods.traumacraft.health.DeathReport;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record DeathReportPacket(DeathReport report) implements CustomPacketPayload {
    public static final Type<DeathReportPacket> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath(TraumaCraft.MOD_ID,"death_report"));
    public static final StreamCodec<RegistryFriendlyByteBuf,DeathReportPacket> CODEC=StreamCodec.of((buf,msg)->{
        var r=msg.report();buf.writeEnum(r.cause());buf.writeEnum(r.origin());buf.writeUtf(r.sourceId(),128);
        buf.writeVarInt(r.criticalRegions());buf.writeVarInt(r.criticalOrgans());buf.writeVarInt(r.bleedingRegions());buf.writeVarInt(r.bleedingOrgans());
        SyncBodyStatePacket.STREAM_CODEC.encode(buf,r.body());
    },buf->new DeathReportPacket(new DeathReport(buf.readEnum(DeathReport.Cause.class),buf.readEnum(DeathReport.Origin.class),buf.readUtf(128),
        buf.readVarInt()&127,buf.readVarInt()&511,buf.readVarInt()&127,buf.readVarInt()&511,SyncBodyStatePacket.STREAM_CODEC.decode(buf))));
    @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
