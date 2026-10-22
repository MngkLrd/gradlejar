package dev.minemods.traumacraft.network;

import dev.minemods.traumacraft.client.ClientBodyState;
import dev.minemods.traumacraft.client.ClientTreatmentState;
import dev.minemods.traumacraft.registry.ModAttachments;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class ModNetwork {
    private ModNetwork() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("2");
        registrar.playToClient(TreatmentProgressPacket.TYPE, TreatmentProgressPacket.CODEC, (packet, context) -> {
            if (FMLEnvironment.dist == Dist.CLIENT) ClientTreatmentState.accept(packet);
        });
        registrar.playToServer(ApplyMedicalPacket.TYPE, ApplyMedicalPacket.STREAM_CODEC, ApplyMedicalPacket::handle);
        registrar.playToClient(SyncBodyStatePacket.TYPE, SyncBodyStatePacket.STREAM_CODEC, (packet, context) -> {
            // The client-only class is resolved only inside the physical client callback.
            if (FMLEnvironment.dist == Dist.CLIENT) ClientBodyState.accept(packet);
        });
    }

    public static void progress(ServerPlayer player, int duration, int elapsed) {
        PacketDistributor.sendToPlayer(player, new TreatmentProgressPacket(duration, elapsed));
    }

    public static void sync(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player,
                SyncBodyStatePacket.from(player.getData(ModAttachments.BODY.get())));
    }
}
