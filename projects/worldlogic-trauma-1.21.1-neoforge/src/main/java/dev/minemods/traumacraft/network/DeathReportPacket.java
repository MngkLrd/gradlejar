package dev.minemods.traumacraft.network;
import dev.minemods.traumacraft.TraumaCraft;
import dev.minemods.traumacraft.health.DeathReport;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record DeathReportPacket(DeathReport report,boolean archived) implements CustomPacketPayload {
    public DeathReportPacket(DeathReport report){this(report,false);}
    public static final Type<DeathReportPacket> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath(TraumaCraft.MOD_ID,"death_report"));
    public static final StreamCodec<RegistryFriendlyByteBuf,DeathReportPacket> CODEC=StreamCodec.of((buf,msg)->{
        buf.writeBoolean(msg.archived());var r=msg.report();buf.writeEnum(r.cause());buf.writeEnum(r.origin());buf.writeUtf(r.sourceId(),128);
        buf.writeVarInt(r.criticalRegions());buf.writeVarInt(r.criticalOrgans());buf.writeVarInt(r.bleedingRegions());buf.writeVarInt(r.bleedingOrgans());
        SyncBodyStatePacket.STREAM_CODEC.encode(buf,r.body());
        buf.writeVarInt(r.history().size());for(var e:r.history()){buf.writeVarInt(e.ageSeconds());buf.writeEnum(e.kind());buf.writeEnum(e.origin());buf.writeVarInt(e.regions());buf.writeVarInt(e.organs());buf.writeFloat(e.hpLoss());buf.writeFloat(e.bloodLoss());buf.writeVarInt(e.shock());}
        buf.writeBoolean(r.material().isPresent());if(r.material().isPresent()){var m=r.material().get();buf.writeEnum(m.kind());buf.writeUtf(m.objectId(),128);buf.writeUtf(m.evidence(),32);buf.writeDouble(m.massKg());buf.writeDouble(m.volumeM3());buf.writeDouble(m.densityKgM3());}
        if(r.material().isPresent()){var surface=r.material().get().surface();buf.writeBoolean(surface.isPresent());if(surface.isPresent()){var s=surface.get();buf.writeUtf(s.materialId(),128);buf.writeUtf(s.evidence(),32);buf.writeDouble(s.stoppingDistanceM());buf.writeDouble(s.moistureKgKg());}}
    },buf->{
        boolean archived=buf.readBoolean();
        var report=new DeathReport(buf.readEnum(DeathReport.Cause.class),buf.readEnum(DeathReport.Origin.class),buf.readUtf(128),buf.readVarInt()&127,buf.readVarInt()&511,buf.readVarInt()&127,buf.readVarInt()&511,SyncBodyStatePacket.STREAM_CODEC.decode(buf));
        int count=buf.readVarInt();if(count<0||count>dev.minemods.traumacraft.health.TraumaHistory.LIMIT)throw new IllegalArgumentException("Invalid death history size");
        var entries=new java.util.ArrayList<dev.minemods.traumacraft.health.TraumaHistory.Entry>();
        for(int i=0;i<count;i++){
            int age=buf.readVarInt();var kind=buf.readEnum(dev.minemods.traumacraft.health.TraumaHistory.Kind.class);var origin=buf.readEnum(DeathReport.Origin.class);
            int regions=buf.readVarInt()&127,organs=buf.readVarInt()&511;float hp=buf.readFloat(),blood=buf.readFloat();int shock=buf.readVarInt();
            if(age<0||age>120||!Float.isFinite(hp)||hp<0||hp>440||!Float.isFinite(blood)||blood<0||blood>5000||shock<0||shock>3)throw new IllegalArgumentException("Invalid death history entry");
            entries.add(new dev.minemods.traumacraft.health.TraumaHistory.Entry(age,kind,origin,regions,organs,hp,blood,shock));
        }
        // Existing 0.8.8 saved reports have no trailing material field.
        if(buf.isReadable()&&buf.readBoolean()){var material=new dev.minemods.traumacraft.health.ImpactMaterial(buf.readEnum(dev.minemods.traumacraft.health.ImpactMaterial.Kind.class),buf.readUtf(128),buf.readUtf(32),buf.readDouble(),buf.readDouble(),buf.readDouble());report=report.withMaterial(java.util.Optional.of(material));}
        // Revision 14 archives ended after the original material fields.
        if(report.material().isPresent()&&buf.isReadable()&&buf.readBoolean()){var s=new dev.minemods.traumacraft.integration.ContactPhysics.Surface(buf.readUtf(128),buf.readUtf(32),buf.readDouble(),buf.readDouble());report=report.withMaterial(java.util.Optional.of(report.material().get().withSurface(java.util.Optional.of(s))));}
        return new DeathReportPacket(report.withHistory(entries),archived);
    });
    @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
