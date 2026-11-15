package dev.quickstacknearby.network;

import dev.quickstacknearby.server.QuickStackService;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class QuickStackNetwork {
    private QuickStackNetwork() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(
            QuickStackRequest.TYPE,
            QuickStackRequest.CODEC,
            (request, context) -> {
                if (context.player() instanceof ServerPlayer player) {
                    QuickStackService.execute(player);
                }
            }
        );
    }

    public static void request() {
        PacketDistributor.sendToServer(QuickStackRequest.INSTANCE);
    }
}
