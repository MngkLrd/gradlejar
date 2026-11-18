package com.kirill.towerveil.gameplay;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.Locale;

/**
 * Bridge to Immersive Portals' registered server command.
 *
 * No direct Immersive Portals class linkage: this mod still compiles on stock
 * NeoForge. If the portal runtime is unavailable we refuse to fall back to a
 * vanilla dimension teleport, since that would visibly show the loading screen.
 *
 * API reference: Immersive Portals /portal tpme <dimension> <x> <y> <z>.
 */
public final class SeamlessTeleportBridge {
    private SeamlessTeleportBridge() { }

    public static boolean isAvailable(ServerPlayer player) {
        var root = player.getServer().getCommands().getDispatcher().getRoot();
        var portal = root.getChild("portal");
        return portal != null && portal.getChild("tpme") != null;
    }

    public static boolean move(ServerPlayer player, ResourceKey<Level> destination,
                               double x, double y, double z) {
        if (!isAvailable(player)) return false;

        String command = String.format(Locale.ROOT,
                "portal tpme %s %.6f %.6f %.6f",
                destination.location(), x, y, z);

        // Commands are invoked by this mod internally, not exposed to players as op.
        var source = player.createCommandSourceStack()
                .withPermission(4).withSuppressedOutput();
        return player.getServer().getCommands().performPrefixedCommand(source, command) > 0;
    }
}
