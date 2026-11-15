package dev.fieldkit.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Handle hotkeys on the main client thread. */
public final class ClientInput {
    private ClientInput() {}

    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.gui.isPausing()) return;

        while (ClientRegistration.TOGGLE_HUD.consumeClick()) {
            ClientState.toggleHud();
            notifyPlayer(mc, "message.fieldkit.hud", ClientState.isHudVisible()
                ? Component.translatable("message.fieldkit.on")
                : Component.translatable("message.fieldkit.off"));
        }
        while (ClientRegistration.COMPACT_MODE.consumeClick()) {
            ClientState.toggleCompact();
            notifyPlayer(mc, "message.fieldkit.compact", ClientState.isCompact()
                ? Component.translatable("message.fieldkit.on")
                : Component.translatable("message.fieldkit.off"));
        }
        while (ClientRegistration.MARK_POINT.consumeClick()) {
            BlockPos pos = mc.player.blockPosition();
            if (mc.hitResult instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK) {
                pos = hit.getBlockPos();
            }
            ClientState.mark(pos, mc.level.dimension());
            mc.player.sendOverlayMessage(Component.translatable("message.fieldkit.marked", pos.getX(), pos.getY(), pos.getZ()));
        }
        while (ClientRegistration.CLEAR_POINT.consumeClick()) {
            ClientState.clearWaypoint();
            mc.player.sendOverlayMessage(Component.translatable("message.fieldkit.cleared"));
        }
    }

    public static void onDisconnect(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientState.clearWaypoint();
    }

    private static void notifyPlayer(Minecraft mc, String key, Component status) {
        if (mc.player != null) {
            mc.player.sendOverlayMessage(Component.translatable(key, status));
        }
    }
}
