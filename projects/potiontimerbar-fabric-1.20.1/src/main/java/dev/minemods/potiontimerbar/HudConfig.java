package dev.minemods.potiontimerbar;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.io.IOException;
import net.fabricmc.loader.api.FabricLoader;

/** Same settings as the NeoForge client config, loaded at startup. */
final class HudConfig {
    private static final Properties VALUES = new Properties();
    static {
        Path path = FabricLoader.getInstance().getConfigDir().resolve("potiontimerbar-client.toml");
        try {
            Files.createDirectories(path.getParent());
            if (Files.notExists(path)) {
                Files.writeString(path, "[hud]\nenabled = true\nautoAvoidAir = true\nverticalOffset = 0\n");
            }
            try (var reader = Files.newBufferedReader(path)) { VALUES.load(reader); }
        } catch (IOException | RuntimeException ignored) {
            // Unreadable configuration must never prevent joining a world.
        }
    }
    static boolean enabled() { return Boolean.parseBoolean(VALUES.getProperty("enabled", "true").trim()); }
    static boolean autoAvoidAir() { return Boolean.parseBoolean(VALUES.getProperty("autoAvoidAir", "true").trim()); }
    static int verticalOffset() {
        try { return Math.max(-60, Math.min(60, Integer.parseInt(VALUES.getProperty("verticalOffset", "0").trim()))); }
        catch (NumberFormatException ignored) { return 0; }
    }
    private HudConfig() {}
}
