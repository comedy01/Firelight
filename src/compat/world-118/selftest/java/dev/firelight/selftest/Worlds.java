package dev.firelight.selftest;

import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.dedicated.DedicatedServerProperties;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.DataPackConfig;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.levelgen.WorldGenSettings;

final class Worlds {
    private Worlds() {
    }

    static void create(Minecraft mc, String name, boolean flat, long seed) {
        LevelSettings settings = new LevelSettings(name, GameType.CREATIVE, false, Difficulty.EASY, true,
                new GameRules(), DataPackConfig.DEFAULT);
        RegistryAccess registries = RegistryAccess.BUILTIN.get();
        WorldGenSettings generation = WorldGenSettings.create(registries,
                new DedicatedServerProperties.WorldGenProperties(Long.toString(seed), new JsonObject(), !flat,
                        flat ? "flat" : "default"));
        mc.createLevel(name, settings, registries, generation);
    }
}
