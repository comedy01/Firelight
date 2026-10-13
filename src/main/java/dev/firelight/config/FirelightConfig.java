package dev.firelight.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class FirelightConfig {
    public static final String FILE_NAME = "firelight.json";

    private static final Logger LOGGER = LogManager.getLogger("firelight");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static final int MIN_STRENGTH_PERCENT = 10;
    public static final int MAX_STRENGTH_PERCENT = 300;

    private boolean enabled = true;
    private float strength = 1.0f;
    private boolean torches = true;
    private boolean lanterns = true;
    private boolean candles = true;
    private boolean fires = true;
    private boolean furnaces = true;
    private boolean entities = true;

    public boolean enabled() {
        return enabled;
    }

    public void setEnabled(boolean value) {
        enabled = value;
    }

    public float strength() {
        return clamp(strength, MIN_STRENGTH_PERCENT / 100f, MAX_STRENGTH_PERCENT / 100f);
    }

    public void setStrength(float value) {
        strength = value;
    }

    public boolean torches() {
        return torches;
    }

    public void setTorches(boolean value) {
        torches = value;
    }

    public boolean lanterns() {
        return lanterns;
    }

    public void setLanterns(boolean value) {
        lanterns = value;
    }

    public boolean candles() {
        return candles;
    }

    public void setCandles(boolean value) {
        candles = value;
    }

    public boolean fires() {
        return fires;
    }

    public void setFires(boolean value) {
        fires = value;
    }

    public boolean furnaces() {
        return furnaces;
    }

    public void setFurnaces(boolean value) {
        furnaces = value;
    }

    public boolean entities() {
        return entities;
    }

    public void setEntities(boolean value) {
        entities = value;
    }

    public void resetToDefaults() {
        FirelightConfig fresh = new FirelightConfig();
        enabled = fresh.enabled;
        strength = fresh.strength;
        torches = fresh.torches;
        lanterns = fresh.lanterns;
        candles = fresh.candles;
        fires = fresh.fires;
        furnaces = fresh.furnaces;
        entities = fresh.entities;
    }

    private static float clamp(float value, float min, float max) {
        return Float.isNaN(value) ? min : Math.max(min, Math.min(max, value));
    }

    public static FirelightConfig load(Path file) {
        if (!Files.isRegularFile(file)) {
            FirelightConfig fresh = new FirelightConfig();
            fresh.saveQuietly(file);
            return fresh;
        }

        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            FirelightConfig loaded = GSON.fromJson(reader, FirelightConfig.class);
            if (loaded == null) {
                throw new JsonParseException("config file is empty");
            }
            return loaded;
        } catch (IOException | RuntimeException e) {
            LOGGER.warn("Could not read {}; using defaults. {}", file, e.toString());
            moveAside(file);
            FirelightConfig fresh = new FirelightConfig();
            fresh.saveQuietly(file);
            return fresh;
        }
    }

    public void save(Path file) throws IOException {
        Path absolute = file.toAbsolutePath();
        Path parent = absolute.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Path temp = absolute.resolveSibling(absolute.getFileName() + ".tmp");
        Files.write(temp, (GSON.toJson(this) + System.lineSeparator()).getBytes(StandardCharsets.UTF_8));
        try {
            Files.move(temp, absolute, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(temp, absolute, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public void saveQuietly(Path file) {
        if (file == null) {
            return;
        }
        try {
            save(file);
        } catch (IOException e) {
            LOGGER.warn("Could not save {}: {}", file, e.toString());
        }
    }

    private static void moveAside(Path file) {
        try {
            Files.move(file, file.resolveSibling(file.getFileName() + ".broken"), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            LOGGER.warn("Could not back up unreadable config {}: {}", file, e.toString());
        }
    }
}
