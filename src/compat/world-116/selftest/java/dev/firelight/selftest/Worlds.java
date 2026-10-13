package dev.firelight.selftest;

import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.DataPackConfig;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.levelgen.WorldGenSettings;

import java.util.Properties;

final class Worlds {
    private Worlds() {
    }

    static void create(Minecraft mc, String name, boolean flat, long seed) {
        LevelSettings settings = new LevelSettings(name, GameType.CREATIVE, false, Difficulty.EASY, true,
                new GameRules(), DataPackConfig.DEFAULT);
        RegistryAccess.RegistryHolder registries = RegistryAccess.builtin();
        Properties properties = new Properties();
        properties.setProperty("level-seed", Long.toString(seed));
        properties.setProperty("level-type", flat ? "flat" : "default");
        mc.createLevel(name, settings, registries, WorldGenSettings.create(registries, properties));
    }
}
