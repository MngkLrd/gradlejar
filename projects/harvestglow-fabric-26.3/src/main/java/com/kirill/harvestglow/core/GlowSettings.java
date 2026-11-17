package com.kirill.harvestglow.core;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

/** Plain, dependency-free client settings shared by all supported loaders. */
public final class GlowSettings {
    public static int radiusHorizontal = 24;
    public static int radiusVertical = 8;
    public static int checksPerTick = 384;
    public static int maxVisible = 40;
    public static int particleBudget = 12;
    public static boolean enabledByDefault = true;
    public static boolean looseModdedDetection = true;
    public static boolean showToggleMessage = true;
    public static final Set<String> forcedCropBlocks = new HashSet<>();
    public static final Set<String> ignoredBlocks = new HashSet<>();
    public static final Set<String> growthProperties = new HashSet<>(Arrays.asList("age","stage","growth","maturity"));

    private GlowSettings() {}
    public static void load(Path file) {
        Properties p = new Properties();
        p.setProperty("radiusHorizontal", "24");
        p.setProperty("radiusVertical", "8");
        p.setProperty("checksPerTick", "384");
        p.setProperty("maxVisible", "40");
        p.setProperty("particleBudget", "12");
        p.setProperty("enabledByDefault", "true");
        p.setProperty("looseModdedDetection", "true");
        p.setProperty("showToggleMessage", "true");
        p.setProperty("growthProperties", "age,stage,growth,maturity");
        p.setProperty("forcedCropBlocks", "");
        p.setProperty("ignoredBlocks", "");
        try {
            Files.createDirectories(file.getParent());
            if (Files.isRegularFile(file)) {
                try (InputStream in = Files.newInputStream(file)) { p.load(in); }
            } else {
                try (OutputStream out = Files.newOutputStream(file)) {
                    p.store(out, "Harvest Glow: lightweight client-side crop markers");
                }
            }
            radiusHorizontal = number(p, "radiusHorizontal", 24, 4, 48);
            radiusVertical = number(p, "radiusVertical", 8, 2, 24);
            checksPerTick = number(p, "checksPerTick", 384, 64, 2048);
            maxVisible = number(p, "maxVisible", 40, 4, 128);
            particleBudget = number(p, "particleBudget", 12, 2, 48);
            enabledByDefault = Boolean.parseBoolean(p.getProperty("enabledByDefault", "true"));
            looseModdedDetection = Boolean.parseBoolean(p.getProperty("looseModdedDetection", "true"));
            showToggleMessage = Boolean.parseBoolean(p.getProperty("showToggleMessage", "true"));
            assign(growthProperties, p.getProperty("growthProperties", "age,stage,growth,maturity"));
            assign(forcedCropBlocks, p.getProperty("forcedCropBlocks", ""));
            assign(ignoredBlocks, p.getProperty("ignoredBlocks", ""));
        } catch (IOException ex) {
            System.err.println("[Harvest Glow] Could not load config: " + ex.getMessage());
        }
    }
    private static int number(Properties p, String key, int def, int lo, int hi) {
        try { return Math.max(lo, Math.min(hi, Integer.parseInt(p.getProperty(key, ""+def).trim()))); }
        catch (NumberFormatException e) { return def; }
    }
    private static void assign(Set<String> target, String csv) {
        target.clear();
        for (String s : csv.split(",")) {
            String v = s.trim().toLowerCase(java.util.Locale.ROOT);
            if (!v.isEmpty()) target.add(v);
        }
    }
}
