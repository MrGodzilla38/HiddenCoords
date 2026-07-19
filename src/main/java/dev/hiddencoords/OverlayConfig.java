package dev.hiddencoords;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** Small dependency-free config store for window state and placement. */
public final class OverlayConfig {
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("hidden-coords.properties");

    public boolean alwaysOnTop = true;
    public int fontSize = 20;
    public int x = Integer.MIN_VALUE;
    public int y = Integer.MIN_VALUE;
    public int width = 300;
    public int height = 150;

    public static OverlayConfig load() {
        OverlayConfig config = new OverlayConfig();
        if (!Files.isRegularFile(FILE)) return config;
        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(FILE)) {
            properties.load(reader);
            config.alwaysOnTop = Boolean.parseBoolean(properties.getProperty("alwaysOnTop", "true"));
            config.fontSize = bounded(properties.getProperty("fontSize"), 20, 12, 48);
            config.x = integer(properties.getProperty("x"), Integer.MIN_VALUE);
            config.y = integer(properties.getProperty("y"), Integer.MIN_VALUE);
            config.width = bounded(properties.getProperty("width"), 300, 260, 2000);
            config.height = bounded(properties.getProperty("height"), 150, 120, 1600);
        } catch (IOException | IllegalArgumentException exception) {
            CoordOverlayMod.LOGGER.warn("Could not read Hidden Coords config; using defaults", exception);
        }
        return config;
    }

    public synchronized void save() {
        Properties properties = new Properties();
        properties.setProperty("alwaysOnTop", Boolean.toString(alwaysOnTop));
        properties.setProperty("fontSize", Integer.toString(fontSize));
        properties.setProperty("x", Integer.toString(x));
        properties.setProperty("y", Integer.toString(y));
        properties.setProperty("width", Integer.toString(width));
        properties.setProperty("height", Integer.toString(height));
        try {
            Files.createDirectories(FILE.getParent());
            try (Writer writer = Files.newBufferedWriter(FILE)) {
                properties.store(writer, "Hidden Coords window settings");
            }
        } catch (IOException exception) {
            CoordOverlayMod.LOGGER.warn("Could not save Hidden Coords config", exception);
        }
    }

    private static int integer(String value, int fallback) {
        try { return value == null ? fallback : Integer.parseInt(value); }
        catch (NumberFormatException ignored) { return fallback; }
    }

    private static int bounded(String value, int fallback, int minimum, int maximum) {
        return Math.clamp(integer(value, fallback), minimum, maximum);
    }
}
