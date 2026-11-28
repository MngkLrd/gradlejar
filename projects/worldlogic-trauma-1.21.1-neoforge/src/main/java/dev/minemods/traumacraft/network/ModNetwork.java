package dev.minemods.traumacraft.network;
import dev.minemods.traumacraft.client.ClientBodyState;
import dev.minemods.traumacraft.client.ClientTreatmentState;
import dev.minemods.traumacraft.health.BodyStates;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
public final class ModNetwork {
    private ModNetwork(){}
    public static void register(RegisterPayloadHandlersEvent event){
        var registrar=event.registrar("15");
        registrar.playToClient(DeathReportPacket.TYPE,DeathReportPacket.CODEC,(m,c)->{
            if(FMLEnvironment.dist==Dist.CLIENT)dev.minemods.traumacraft.client.ClientDeathReport.accept(m);
        });
        registrar.playToClient(SyncBodyStatePacket.TYPE,SyncBodyStatePacket.STREAM_CODEC,(m,c)->{
            if(FMLEnvironment.dist==Dist.CLIENT)ClientBodyState.accept(m);
        });
        registrar.playToClient(TreatmentProgressPacket.TYPE,TreatmentProgressPacket.CODEC,(m,c)->{
            if(FMLEnvironment.dist==Dist.CLIENT)ClientTreatmentState.accept(m);
        });
        registrar.playToClient(GaitPacket.TYPE,GaitPacket.CODEC,(m,c)->{
            if(FMLEnvironment.dist==Dist.CLIENT)dev.minemods.traumacraft.client.ClientGaitState.accept(m);
        });
        registrar.playToServer(ClimbRequestPacket.TYPE,ClimbRequestPacket.CODEC,ClimbRequestPacket::handle);
        registrar.playToClient(ClimbPosePacket.TYPE,ClimbPosePacket.CODEC,(m,c)->{
            if(FMLEnvironment.dist==Dist.CLIENT)dev.minemods.traumacraft.client.ClientClimbState.accept(m);
        });
        registrar.playToServer(ApplyMedicalPacket.TYPE,ApplyMedicalPacket.STREAM_CODEC,ApplyMedicalPacket::handle);
    }
    public static void archivedDeath(ServerPlayer p,dev.minemods.traumacraft.health.DeathReport report){if(!(p instanceof FakePlayer)&&p.connection!=null)PacketDistributor.sendToPlayer(p,new DeathReportPacket(report,true));}
    public static void death(ServerPlayer p,dev.minemods.traumacraft.health.DeathReport report){
        if(!(p instanceof FakePlayer)&&p.connection!=null)PacketDistributor.sendToPlayer(p,new DeathReportPacket(report));
    }
    public static void request(ApplyMedicalPacket p){PacketDistributor.sendToServer(p);}
    public static void progress(ServerPlayer p,int duration,int elapsed){
        if(!(p instanceof FakePlayer)&&p.connection!=null)PacketDistributor.sendToPlayer(p,new TreatmentProgressPacket(duration,elapsed));
    }
    public static void climb(ServerPlayer p,int arms,int elapsed,int duration,net.minecraft.world.phys.Vec3 start,net.minecraft.world.phys.Vec3 end){
        if(!(p instanceof FakePlayer)&&p.connection!=null)PacketDistributor.sendToPlayersTrackingEntityAndSelf(p,new ClimbPosePacket(p.getUUID(),arms,elapsed,duration,start,end));
    }
    public static void syncGait(ServerPlayer p){
        if(p instanceof FakePlayer||p.connection==null)return;
        var b=BodyStates.get(p);
        boolean active=dev.minemods.traumacraft.integration.WorldLogicBridge.functionsEnabled(p);
        PacketDistributor.sendToPlayersTrackingEntity(p,new GaitPacket(p.getUUID(),
            active?b.limbFunction(dev.minemods.traumacraft.health.BodyPart.LEFT_LEG):1,
            active?b.limbFunction(dev.minemods.traumacraft.health.BodyPart.RIGHT_LEG):1));
    }
    public static void sync(ServerPlayer p){
        syncGait(p);
        BodyStates.flush(p);
        if(!(p instanceof FakePlayer)&&p.connection!=null)PacketDistributor.sendToPlayer(p,SyncBodyStatePacket.from(BodyStates.get(p),dev.minemods.traumacraft.integration.ThermalBridge.condition(p).wireId,dev.minemods.traumacraft.integration.WorldLogicBridge.active(p)?dev.minemods.traumacraft.disease.WaterIllness.load(p):dev.minemods.traumacraft.disease.DiseaseModel.State.HEALTHY));
    }
}
