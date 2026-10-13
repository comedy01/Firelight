package dev.firelight.client;

import dev.firelight.config.FirelightConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;

public final class FirelightClient {
    public static final String MOD_ID = "firelight";
    private static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    private static FirelightConfig config = new FirelightConfig();
    private static Path configPath;
    private static boolean failed;

    private FirelightClient() {
    }

    public static void init(Path configDir) {
        configPath = configDir.resolve(FirelightConfig.FILE_NAME);
        config = FirelightConfig.load(configPath);
    }

    public static FirelightConfig config() {
        return config;
    }

    public static Path configPath() {
        return configPath;
    }

    public static void saveConfig() {
        config.saveQuietly(configPath);
    }

    public static boolean active() {
        return !failed && config.enabled();
    }

    public static void fail(Throwable e) {
        if (!failed) {
            failed = true;
            LOGGER.error("Firelight hit an error and turned itself off for this session", e);
        }
    }
}
