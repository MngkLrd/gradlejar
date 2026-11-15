package dev.fieldkit.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** Persist HUD settings, but keep waypoints session-only. */
public final class ClientState {
    private static boolean loaded;
    private static boolean hudVisible = true;
    private static boolean compact;
    private static BlockPos waypoint;
    private static ResourceKey<Level> waypointDimension;

    private ClientState() {}

    public static boolean isHudVisible() {
        loadIfNecessary();
        return hudVisible;
    }

    public static boolean isCompact() {
        loadIfNecessary();
        return compact;
    }

    public static void toggleHud() {
        loadIfNecessary();
        hudVisible = !hudVisible;
        save();
    }

    public static void toggleCompact() {
        loadIfNecessary();
        compact = !compact;
        save();
    }

    public static void mark(BlockPos position, ResourceKey<Level> dimension) {
        waypoint = position.immutable();
        waypointDimension = dimension;
    }

    public static void clearWaypoint() {
        waypoint = null;
        waypointDimension = null;
    }

    public static BlockPos getWaypoint() {
        return waypoint;
    }

    public static boolean isWaypointIn(ResourceKey<Level> dimension) {
        return waypoint != null && dimension.equals(waypointDimension);
    }

    private static Path getConfigPath() {
        return Minecraft.getInstance().gameDirectory.toPath()
            .resolve("config").resolve("fieldkit.properties");
    }

    private static void loadIfNecessary() {
        if (loaded) return;
        loaded = true;
        Path file = getConfigPath();
        if (!Files.isRegularFile(file)) return;

        Properties preferences = new Properties();
        try (InputStream input = Files.newInputStream(file)) {
            preferences.load(input);
            hudVisible = Boolean.parseBoolean(preferences.getProperty("hudVisible", "true"));
            compact = Boolean.parseBoolean(preferences.getProperty("compact", "false"));
        } catch (IOException ignored) {
            // Bad settings should not break the client.
        }
    }

    private static void save() {
        Properties preferences = new Properties();
        preferences.setProperty("hudVisible", Boolean.toString(hudVisible));
        preferences.setProperty("compact", Boolean.toString(compact));
        Path file = getConfigPath();
        try {
            Files.createDirectories(file.getParent());
            try (OutputStream output = Files.newOutputStream(file)) {
                preferences.store(output, "FieldKit client preferences");
            }
        } catch (IOException ignored) {
            // A failed settings write must not crash the game.
        }
    }
}
